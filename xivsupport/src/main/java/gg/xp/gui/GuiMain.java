package gg.xp.gui;

import gg.xp.context.StateStore;
import gg.xp.events.AutoEventDistributor;
import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.EventMaster;
import gg.xp.events.actlines.events.XivStateChange;
import gg.xp.events.misc.RawEventStorage;
import gg.xp.events.misc.Stats;
import gg.xp.events.models.XivEntity;
import gg.xp.events.models.XivPlayerCharacter;
import gg.xp.events.models.XivZone;
import gg.xp.events.state.XivState;
import gg.xp.events.ws.ActWsConnectionStatusChangedEvent;
import gg.xp.events.ws.WsState;
import gg.xp.gui.tables.AutoBottomScrollHelper;
import gg.xp.gui.tables.CustomColumn;
import gg.xp.gui.tables.CustomTableModel;
import gg.xp.gui.tree.TopologyTreeEditor;
import gg.xp.gui.tree.TopologyTreeModel;
import gg.xp.gui.tree.TopologyTreeRenderer;
import gg.xp.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GuiMain {

	private static final Logger log = LoggerFactory.getLogger(GuiMain.class);
	private final EventMaster master;
	private final StateStore state;
	private final PicoContainer container;

	public static void main(String[] args) {
		MutablePicoContainer pico = XivMain.masterInit();
		pico.addComponent(GuiMain.class);
		pico.getComponent(GuiMain.class);
	}

	public GuiMain(EventMaster master, PicoContainer container) {
		this.master = master;
		this.state = master.getDistributor().getStateStore();
		this.container = container;
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("My New Window TODO pick a name");
			JTabbedPane tabPane = new JTabbedPane();
			frame.setSize(800, 600);
			frame.setVisible(true);
			JPanel mainPanel = new SystemTabPanel();
			Component scrollPanel = new JScrollPane(mainPanel);
			tabPane.addTab("System", scrollPanel);
//			tabPane.addTab("System", mainPanel);
			JPanel stats = new StatsPanel();
			tabPane.addTab("Stats", stats);
			tabPane.addTab("Plugins", new PluginTopologyPanel());
			tabPane.addTab("Events", new EventsPanel());
			tabPane.addTab("ACT Log", new JPanel());
			tabPane.addTab("System Log", new JPanel());
			tabPane.addTab("Import/Export", new JPanel());
			frame.add(tabPane);
		});
	}

	private class SystemTabPanel extends JPanel {
		SystemTabPanel() {

			BoxLayout mgr = new BoxLayout(this, BoxLayout.PAGE_AXIS);
			setLayout(mgr);
//			setLayout(new FlowLayout());
			ActWsConnectionStatus connectionStatusPanel = new ActWsConnectionStatus();
			connectionStatusPanel.setMaximumSize(new Dimension(32768, 200));
			add(connectionStatusPanel);
			XivStateStatus xivStateStatus = new XivStateStatus();
			xivStateStatus.setMaximumSize(new Dimension(32768, 400));
			add(xivStateStatus);
			// filler for alignment
			StandardPanel fillerPanel = new StandardPanel("Random Filler Panel");
			fillerPanel.setMaximumSize(new Dimension(32768, 200));
			fillerPanel.add(new JLabel("How do I layout"));
			add(fillerPanel);
			// TODO: these don't work right because we aren't guaranteed to be the last event handler
			master.getDistributor().registerHandler(ActWsConnectionStatusChangedEvent.class, connectionStatusPanel::connectionStatusChange);
			master.getDistributor().registerHandler(XivStateChange.class, (c, e) -> xivStateStatus.refresh());
		}
	}

	// TODO: system for plugins to install their own guis
	private static class StandardPanel extends JPanel {
		public StandardPanel(String title) {
			setBorder(new TitledBorder(title));
			setPreferredSize(getMinimumSize());
		}
	}

	private class ActWsConnectionStatus extends StandardPanel {

		private final KeyValueDisplaySet connectedDisp;

		public ActWsConnectionStatus() {
			super("System Status");
			JCheckBox box = new JCheckBox();
			box.setEnabled(false);
			connectedDisp = new KeyValueDisplaySet(List.of(new KeyValuePairDisplay<>(
					"Connected to ACT WS",
					box,
					() -> state.get(WsState.class).isConnected(),
					AbstractButton::setSelected
			)));
			add(connectedDisp);
//			setMinimumSize(new Dimension(500, 500));
//			setMaximumSize(new Dimension(500, 500));
//			repaint();
			updateGui();
		}

		public void connectionStatusChange(EventContext<Event> context, ActWsConnectionStatusChangedEvent event) {
			updateGui();
		}

		private void updateGui() {
			connectedDisp.refresh();
		}
	}

	;

	private class XivStateStatus extends StandardPanel implements Refreshable {

		private final List<Refreshable> displayed = new ArrayList<>();

		public XivStateStatus() {
			super("Xiv Status");

			setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

			KeyValueDisplaySet leftItems = new KeyValueDisplaySet(List.of(new KeyValuePairDisplay<>(
							"Player Name",
							new JTextArea(1, 15),
							() -> {
								XivEntity player = state.get(XivState.class).getPlayer();
								return player == null ? "null" : player.getName();
							},
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Zone Name",
							new JTextArea(1, 15),
							() -> {
								XivZone zone = state.get(XivState.class).getZone();
								return zone == null ? "null" : zone.getName();
							},
							JTextArea::setText
					)));
			displayed.add(leftItems);
			add(leftItems);

			JPanel right = new JPanel();
			right.setLayout(new BoxLayout(right, BoxLayout.PAGE_AXIS));
			JTable partyMembersTable = new JTable(8, 3);
//			partyMembersTable.setPreferredSize(new Dimension(300, 300));
			XivPlayerTableModel dataModel = new XivPlayerTableModel();
			partyMembersTable.setModel(dataModel);
			right.setLayout(new BoxLayout(right, BoxLayout.PAGE_AXIS));
			JScrollPane scrollPane = new JScrollPane(partyMembersTable);
			KeyValuePairDisplay<JScrollPane, List<XivPlayerCharacter>> tableHolder = new KeyValuePairDisplay<>(
					"Party Members",
					scrollPane,
					() -> state.get(XivState.class).getPartyList(),
					(ignored, v) -> dataModel.setData(v)
			);
//			scrollPane.setMinimumSize(new Dimension(400, 400));

			setMinimumSize(new Dimension(400, 400));
			right.add(new WrapperPanel(tableHolder.getLabel()));
			right.add((tableHolder.getComponent()));
			// TODO
//			displayed.add(tableHolder);
			add(right);


			refresh();
		}

		public void refresh() {
			displayed.forEach(Refreshable::refresh);
		}
	}

	private class StatsPanel extends StandardPanel implements Refreshable {
		private final KeyValueDisplaySet displayed;

		public StatsPanel() {
			super("Stats");

			JButton refreshButton = new JButton("Refresh");
			refreshButton.addActionListener(e -> refresh());

			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

			List<KeyValuePairDisplay<?, ?>> leftItems = List.of(
					new KeyValuePairDisplay<>(
							"Duration",
							new JTextArea(1, 15),
							() -> container.getComponent(Stats.class).getDuration().toString(),
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Total",
							new JTextArea(1, 15),
							() -> String.valueOf(container.getComponent(Stats.class).getTotal()),
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Primo",
							new JTextArea(1, 15),
							() -> String.valueOf(container.getComponent(Stats.class).getPrimogenitor()),
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Synthetic",
							new JTextArea(1, 15),
							() -> String.valueOf(container.getComponent(Stats.class).getSynthetic()),
							JTextArea::setText
					));
			displayed = new KeyValueDisplaySet(leftItems);
			add(new WrapperPanel(refreshButton));
			this.add(displayed);
			refresh();
		}

		public void refresh() {
			displayed.refresh();
		}
	}

	private class PluginTopologyPanel extends StandardPanel {

		public PluginTopologyPanel() {
			super("Topology");
			setLayout(new BorderLayout());
			JTree tree = new JTree(new TopologyTreeModel(container.getComponent(AutoEventDistributor.class)));
			TopologyTreeRenderer renderer = new TopologyTreeRenderer();
			tree.setCellRenderer(renderer);
			tree.setCellEditor(new TopologyTreeEditor(tree));
			tree.setEditable(true);
			JScrollPane scrollPane = new JScrollPane(tree);
			scrollPane.setBorder(new LineBorder(Color.BLUE));
			scrollPane.setPreferredSize(scrollPane.getMaximumSize());
			add(scrollPane);
		}
	}

	private class EventsPanel extends StandardPanel {
		public EventsPanel() {
			super("Events");
			setLayout(new BorderLayout());
			// TODO: event serial number
			// TODO: jump to parent button
			// Main table
			RawEventStorage rawStorage = container.getComponent(RawEventStorage.class);
			CustomTableModel<Event> model = CustomTableModel.builder(rawStorage::getEvents)
					.addColumn(new CustomColumn<>("Type", e -> e.getClass().getSimpleName()))
					.addColumn(new CustomColumn<>("Parent", e -> {
						Event parent = e.getParent();
						return parent == null ? null : parent.getClass().getSimpleName();
					}))
					.addColumn(new CustomColumn<>("toString", Object::toString))
					.rowSelectedCallback(e -> log.info("Selected row {}", e))
					.build();
			JTable table = new JTable(model);
			JButton refreshButton = new JButton("Refresh");
			refreshButton.addActionListener(e -> model.refresh());

			JCheckBox stayAtBottom = new JCheckBox("Scroll to Bottom");
			AutoBottomScrollHelper scroller = new AutoBottomScrollHelper(table, () -> stayAtBottom.setSelected(false));
			stayAtBottom.addItemListener(e -> scroller.setAutoScrollEnabled(stayAtBottom.isSelected()));

			// Top panel
			JPanel topPanel = new JPanel();
			topPanel.add(refreshButton);
			topPanel.add(stayAtBottom);
			add(topPanel, BorderLayout.PAGE_START);

			// Details
			StandardPanel bottomPanel = new StandardPanel("Event Details");
//			bottomPanel.setMinimumSize(new Dimension(200, 100));



			CustomTableModel<Event> detailsModel = CustomTableModel.builder(rawStorage::getEvents)
					.addColumn(new CustomColumn<>("Type", e -> e.getClass().getSimpleName()))
					.addColumn(new CustomColumn<>("Parent", e -> {
						Event parent = e.getParent();
						return parent == null ? null : parent.getClass().getSimpleName();
					}))
					.addColumn(new CustomColumn<>("toString", Object::toString))
					.rowSelectedCallback(e -> log.info("Selected row {}", e))
					.build();
			JTable detailsTable = new JTable(detailsModel);
			JScrollPane detailsScroller = new JScrollPane(detailsTable);
			detailsScroller.setPreferredSize(detailsScroller.getMaximumSize());
			bottomPanel.add(detailsScroller);

			// Split pane
			JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, scroller, detailsScroller);
			add(splitPane);
			SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.7));
		}
	}
}
