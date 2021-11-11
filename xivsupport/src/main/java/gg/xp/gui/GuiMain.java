package gg.xp.gui;

import gg.xp.context.StateStore;
import gg.xp.events.ACTLogLineEvent;
import gg.xp.events.AutoEventDistributor;
import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.EventMaster;
import gg.xp.events.actlines.events.HasSourceEntity;
import gg.xp.events.actlines.events.HasTargetEntity;
import gg.xp.events.actlines.events.XivStateChange;
import gg.xp.events.misc.RawEventStorage;
import gg.xp.events.misc.Stats;
import gg.xp.events.models.XivEntity;
import gg.xp.events.models.XivPlayerCharacter;
import gg.xp.events.models.XivZone;
import gg.xp.events.state.XivState;
import gg.xp.events.ws.ActWsConnectionStatusChangedEvent;
import gg.xp.events.ws.WsState;
import gg.xp.gui.tables.CustomColumn;
import gg.xp.gui.tables.filters.EventTypeFilter;
import gg.xp.gui.tables.TableWithFilterAndDetails;
import gg.xp.gui.tables.filters.EventEntityFilter;
import gg.xp.gui.tree.TopologyTreeEditor;
import gg.xp.gui.tree.TopologyTreeModel;
import gg.xp.gui.tree.TopologyTreeRenderer;
import gg.xp.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GuiMain {

	private static final Logger log = LoggerFactory.getLogger(GuiMain.class);
	private final EventMaster master;
	private final StateStore state;
	private final PicoContainer container;

	public static void main(String[] args) {
		MutablePicoContainer pico = XivMain.masterInit();
		pico.addComponent(GuiMain.class);
		pico.getComponent(GuiMain.class);
		installCustomEventQueue();
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
			tabPane.addTab("Events", getEventsPanel());
			tabPane.addTab("ACT Log", getActLogPanel());
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
			TitleBorderFullsizePanel fillerPanel = new TitleBorderFullsizePanel("Random Filler Panel");
			fillerPanel.setMaximumSize(new Dimension(32768, 200));
			fillerPanel.add(new JLabel("How do I layout"));
			add(fillerPanel);
			// TODO: these don't work right because we aren't guaranteed to be the last event handler
			master.getDistributor().registerHandler(ActWsConnectionStatusChangedEvent.class, connectionStatusPanel::connectionStatusChange);
			master.getDistributor().registerHandler(XivStateChange.class, (c, e) -> xivStateStatus.refresh());
		}
	}

	private class ActWsConnectionStatus extends TitleBorderFullsizePanel {

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

	private class XivStateStatus extends TitleBorderFullsizePanel implements Refreshable {

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

	private class StatsPanel extends TitleBorderFullsizePanel implements Refreshable {
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

	private class PluginTopologyPanel extends TitleBorderFullsizePanel {

		public PluginTopologyPanel() {
			super("Topology");
			setLayout(new BorderLayout());
			JTree tree = new JTree(new TopologyTreeModel(container.getComponent(AutoEventDistributor.class)));
			TopologyTreeRenderer renderer = new TopologyTreeRenderer();
			tree.setCellRenderer(renderer);
			tree.setCellEditor(new TopologyTreeEditor(tree));
			tree.setEditable(true);
			JScrollPane scrollPane = new JScrollPane(tree);
//			scrollPane.setBorder(new LineBorder(Color.BLUE));
			scrollPane.setPreferredSize(scrollPane.getMaximumSize());
			add(scrollPane);
		}
	}

	private JPanel getEventsPanel() {
		// TODO: event serial number
		// TODO: jump to parent button
		// Main table
		RawEventStorage rawStorage = container.getComponent(RawEventStorage.class);
		return TableWithFilterAndDetails.builder("Events", rawStorage::getEvents,
						currentEvent -> {
							if (currentEvent == null) {
								return Collections.emptyList();
							}
							else {
								return currentEvent.dumpFields()
										.entrySet()
										.stream()
										.filter(e -> !"serialVersionUID".equals(e.getKey().getName()))
										.collect(Collectors.toList());
							}
						})
				.addMainColumn(new CustomColumn<>("Type", e -> e.getClass().getSimpleName()))
				.addMainColumn(new CustomColumn<>("Source", e -> e instanceof HasSourceEntity ? ((HasSourceEntity) e).getSource().getName() : null))
				.addMainColumn(new CustomColumn<>("Target", e -> e instanceof HasTargetEntity ? ((HasTargetEntity) e).getTarget().getName() : null))
				// TODO: action/effect column that shows buff or
				.addMainColumn(new CustomColumn<>("Parent", e -> {
					Event parent = e.getParent();
					return parent == null ? null : parent.getClass().getSimpleName();
				}))
				.addMainColumn(new CustomColumn<>("toString", Object::toString))
				.addDetailsColumn(new CustomColumn<>("Field", e -> e.getKey().getName()))
				.addDetailsColumn(new CustomColumn<>("Value", Map.Entry::getValue))
				.addDetailsColumn(new CustomColumn<>("Field Type", e -> e.getKey().getGenericType()))
				.addDetailsColumn(new CustomColumn<>("Declared In", e -> e.getKey().getDeclaringClass().getSimpleName()))
				.addFilter(EventTypeFilter::new)
				.addFilter(EventEntityFilter::sourceFilter)
				.addFilter(EventEntityFilter::targetFilter)
				.build();
	}

	private JPanel getActLogPanel() {
		RawEventStorage rawStorage = container.getComponent(RawEventStorage.class);
		return TableWithFilterAndDetails.builder("ACT Log",
						() -> rawStorage.getEvents().stream().filter(ACTLogLineEvent.class::isInstance)
								.map(ACTLogLineEvent.class::cast)
								.collect(Collectors.toList()),
						currentEvent -> {
							if (currentEvent == null) {
								return Collections.emptyList();
							}
							else {
								return currentEvent.dumpFields()
										.entrySet()
										.stream()
										.filter(e -> !"serialVersionUID".equals(e.getKey().getName()))
										.collect(Collectors.toList());
							}
						})
				.addMainColumn(new CustomColumn<>("Line", ACTLogLineEvent::getLogLine))
				.addDetailsColumn(new CustomColumn<>("Field", e -> e.getKey().getName()))
				.addDetailsColumn(new CustomColumn<>("Value", Map.Entry::getValue))
				.addDetailsColumn(new CustomColumn<>("Field Type", e -> e.getKey().getGenericType()))
				.addDetailsColumn(new CustomColumn<>("Declared In", e -> e.getKey().getDeclaringClass().getSimpleName()))
				.build();

	}

	private static void installCustomEventQueue() {
		EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
		queue.push(new EventQueue() {
			@Override
			protected void dispatchEvent(AWTEvent event) {
				long timeBefore = System.currentTimeMillis();
				try {
					super.dispatchEvent(event);
				} finally {
					long timeAfter = System.currentTimeMillis();
					long delta = timeAfter - timeBefore;
					// TODO find good value for this
					if (delta > 250) {
						log.warn("Slow GUI performance: took {}ms to dispatch event {}", delta, event);
					}
				}
			}
		});
	}
}
