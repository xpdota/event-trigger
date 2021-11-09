package gg.xp.gui;

import gg.xp.context.StateStore;
import gg.xp.events.AutoEventDistributor;
import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.EventDistributor;
import gg.xp.events.EventMaster;
import gg.xp.events.actlines.events.XivStateChange;
import gg.xp.events.models.XivEntity;
import gg.xp.events.models.XivZone;
import gg.xp.events.state.XivState;
import gg.xp.events.ws.ActWsConnectionStatusChangedEvent;
import gg.xp.events.ws.ActWsLogSource;
import gg.xp.events.ws.WsState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class GuiMain {

	private static final Logger log = LoggerFactory.getLogger(GuiMain.class);
	private final EventMaster master;
	private final StateStore state;

	public static void main(String[] args) {
		log.info("Starting main program");
		log.info("PID: {}", ProcessHandle.current().pid());

		EventDistributor<Event> eventDistributor = new AutoEventDistributor();

		EventMaster master = new EventMaster(eventDistributor);
		master.start();

		new GuiMain(master);

		ActWsLogSource wsLogSource = new ActWsLogSource(master);
		wsLogSource.start();


		log.info("Everything seems to have started successfully");
	}

	public GuiMain(EventMaster master) {
		this.master = master;
		this.state = master.getDistributor().getStateStore();
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("My New Window");
			JTabbedPane tabPane = new JTabbedPane();
			frame.setSize(640, 480);
			frame.setVisible(true);
			JPanel mainPanel = new JPanel();
			BoxLayout mgr = new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS);
			mainPanel.setLayout(mgr);
//			mainPanel.setLayout(new GridBagLayout());
//			frame.add(mainPanel);
			ActWsConnectionStatus connectionStatusPanel = new ActWsConnectionStatus();
			mainPanel.add(connectionStatusPanel);
			XivStateStatus xivStateStatus = new XivStateStatus();
			mainPanel.add(xivStateStatus);
			// filler for alignment
			StandardPanel fillerPanel = new StandardPanel("Random Filler Panel");
			fillerPanel.add(new JLabel("How do I layout"));
			mainPanel.add(fillerPanel);
			// TODO: these don't work right because we aren't guaranteed to be the last event handler
			master.getDistributor().registerHandler(ActWsConnectionStatusChangedEvent.class, connectionStatusPanel::connectionStatusChange);
			master.getDistributor().registerHandler(XivStateChange.class, (c, e) -> xivStateStatus.refresh());
			JScrollPane scrollPanel = new JScrollPane(mainPanel);
			tabPane.addTab("System", scrollPanel);
			frame.add(tabPane);
		});
	}

	// TODO: system for plugins to install their own guis
	private class StandardPanel extends JPanel {
		public StandardPanel(String title) {
			setBorder(new TitledBorder(title));
			setPreferredSize(getMinimumSize());
		}
	}

	private class ActWsConnectionStatus extends StandardPanel {

		private final JCheckBox checkBox;

		public ActWsConnectionStatus() {
			super("System Status");
			checkBox = new JCheckBox("Connected to ACT WS");
			checkBox.setEnabled(false);
			updateGui();
			this.add(checkBox);
		}

		public void connectionStatusChange(EventContext<Event> context, ActWsConnectionStatusChangedEvent event) {
			updateGui();
		}

		private void updateGui() {
			boolean status = state.get(WsState.class).isConnected();
			SwingUtilities.invokeLater(() -> checkBox.setSelected(status));
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
//			GridBagLayout mgr = new GridBagLayout();
//			GridBagConstraints c = new GridBagConstraints();
//			c.fill = GridBagConstraints.HORIZONTAL;
//			c.weightx = 0;
//			c.weighty = 0;
//			c.gridy = 0;
//			c.gridx = 0;
//			setLayout(mgr);
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

			displayed.add(new KeyValuePairDisplay<>(
					"Player Name",
					new JTextArea(1, 20),
					() -> {
						XivEntity player = state.get(XivState.class).getPlayer();
						return player == null ? "null" : player.getName();
					},
					JTextArea::setText
			));
			displayed.add(new KeyValuePairDisplay<>(
					"Zone Name",
					new JTextArea(1, 20),
					() -> {
						XivZone zone = state.get(XivState.class).getZone();
						return zone == null ? "null" : zone.getName();
					},
					JTextArea::setText
			));
			displayed.forEach(d -> {
				this.add(d);
//				this.add(d, c);
//				c.gridy ++;
			});
//			setPreferredSize(getMinimumSize());
			refresh();
		}

		public void refresh() {
			displayed.forEach(KeyValuePairDisplay::refresh);
		}
	}
}
