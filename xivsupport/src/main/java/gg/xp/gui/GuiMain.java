package gg.xp.gui;

import gg.xp.context.StateStore;
import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.EventMaster;
import gg.xp.events.actlines.events.XivStateChange;
import gg.xp.events.models.XivEntity;
import gg.xp.events.models.XivPlayerCharacter;
import gg.xp.events.models.XivZone;
import gg.xp.events.state.XivState;
import gg.xp.events.ws.ActWsConnectionStatusChangedEvent;
import gg.xp.events.ws.WsState;
import gg.xp.sys.XivMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class GuiMain {

	private static final Logger log = LoggerFactory.getLogger(GuiMain.class);
	private final EventMaster master;
	private final StateStore state;

	public static void main(String[] args) {
		EventMaster eventMaster = XivMain.masterInit();
		new GuiMain(eventMaster);
	}

	public GuiMain(EventMaster master) {
		this.master = master;
		this.state = master.getDistributor().getStateStore();
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("My New Window");
			JTabbedPane tabPane = new JTabbedPane();
			frame.setSize(800, 600);
			frame.setVisible(true);
			JPanel mainPanel = new SystemStatusPanel();
			JScrollPane scrollPanel = new JScrollPane(mainPanel);
			tabPane.addTab("System", scrollPanel);
			tabPane.addTab("Stats", new JPanel());
			tabPane.addTab("Plugins", new JPanel());
			tabPane.addTab("Events", new JPanel());
			tabPane.addTab("ACT Log", new JPanel());
			tabPane.addTab("System Log", new JPanel());
			tabPane.addTab("Import/Export", new JPanel());
			frame.add(tabPane);
		});
	}

	private class SystemStatusPanel extends JPanel {
		SystemStatusPanel() {

			BoxLayout mgr = new BoxLayout(this, BoxLayout.PAGE_AXIS);
			setLayout(mgr);
			ActWsConnectionStatus connectionStatusPanel = new ActWsConnectionStatus();
			add(connectionStatusPanel);
			XivStateStatus xivStateStatus = new XivStateStatus();
			add(xivStateStatus);
			// filler for alignment
			StandardPanel fillerPanel = new StandardPanel("Random Filler Panel");
			fillerPanel.add(new JLabel("How do I layout"));
			add(fillerPanel);
			// TODO: these don't work right because we aren't guaranteed to be the last event handler
			master.getDistributor().registerHandler(ActWsConnectionStatusChangedEvent.class, connectionStatusPanel::connectionStatusChange);
			master.getDistributor().registerHandler(XivStateChange.class, (c, e) -> xivStateStatus.refresh());
		}
	}

	// TODO: system for plugins to install their own guis
	private class StandardPanel extends JPanel {
		public StandardPanel(String title) {
			setBorder(new TitledBorder(title));
			setPreferredSize(getMinimumSize());
		}
	}

	private class ActWsConnectionStatus extends StandardPanel {

		private final KeyValuePairDisplay<JCheckBox, Boolean> connectedDisp;

		public ActWsConnectionStatus() {
			super("System Status");
			JCheckBox box = new JCheckBox();
			box.setEnabled(false);
			connectedDisp = new KeyValuePairDisplay<>(
					"Connected to ACT WS",
					box,
					() -> state.get(WsState.class).isConnected(),
					AbstractButton::setSelected
			);
			add(connectedDisp);
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

	public static class KeyValuePairDisplay<C extends Component, D> extends JPanel {

		private final C component;
		private final Supplier<D> dataGetter;
		private final BiConsumer<C, D> guiUpdater;

		public KeyValuePairDisplay(String labelText, C component, Supplier<D> dataGetter, BiConsumer<C, D> guiUpdater) {
			this.component = component;
			this.dataGetter = dataGetter;
			this.guiUpdater = guiUpdater;
			JLabel label = new JLabel(labelText);
			label.setLabelFor(component);
			add(component);
			add(label);
		}

		public void refresh() {
			D newData = dataGetter.get();
			guiUpdater.accept(component, newData);
		}
	}

	private class XivStateStatus extends StandardPanel {

		private final List<KeyValuePairDisplay<?, ?>> displayed = new ArrayList<>();

		public XivStateStatus() {
			super("Xiv Status");

			setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

			JPanel left = new JPanel();
			left.setLayout(new BoxLayout(left, BoxLayout.PAGE_AXIS));
			List<KeyValuePairDisplay<?, ?>> leftItems = List.of(new KeyValuePairDisplay<>(
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
					));
			leftItems.forEach(left::add);
			displayed.addAll(leftItems);
			add(left);

			JPanel right = new JPanel();
			JTable partyMembersTable = new JTable(3, 3);
			partyMembersTable.setPreferredSize(new Dimension(300, 300));
			XivPlayerTableModel dataModel = new XivPlayerTableModel();
			partyMembersTable.setModel(dataModel);
			right.setLayout(new BoxLayout(right, BoxLayout.PAGE_AXIS));
			KeyValuePairDisplay<JScrollPane, List<XivPlayerCharacter>> tableHolder = new KeyValuePairDisplay<>(
					"Party Members",
					new JScrollPane(partyMembersTable),
					() -> state.get(XivState.class).getPartyList(),
					(ignored, v) -> {
						dataModel.setData(v);
					}
			);
			// TODO
			right.add(tableHolder);
			displayed.add(tableHolder);
			add(right);


			refresh();
		}

		public void refresh() {
			displayed.forEach(KeyValuePairDisplay::refresh);
		}
	}

	private class XivPlayerTableModel extends AbstractTableModel {

		private List<XivPlayerCharacter> data = Collections.emptyList();
		// TODO: IIRC, column models do this, but cleaner?
		private String[] columnNames = {"Name", "Job", "ID"};

		public void setData(List<XivPlayerCharacter> data) {
			this.data = data;
			fireTableDataChanged();
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		@Override
		public int getRowCount() {
			return data.size();
		}

		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 1:
					return data.get(rowIndex - 1).getName();
				case 2:
					return data.get(rowIndex - 1).getJob().name();
				case 3:
					return data.get(rowIndex - 1).getId();
				default:
					// TODO
					return null;
			}
		}
	}
}
