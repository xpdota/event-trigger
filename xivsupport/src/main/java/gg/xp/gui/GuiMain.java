package gg.xp.gui;

import gg.xp.context.StateStore;
import gg.xp.events.ACTLogLineEvent;
import gg.xp.events.AutoEventDistributor;
import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.EventMaster;
import gg.xp.events.actlines.events.HasAbility;
import gg.xp.events.actlines.events.HasSourceEntity;
import gg.xp.events.actlines.events.HasStatusEffect;
import gg.xp.events.actlines.events.HasTargetEntity;
import gg.xp.events.actlines.events.XivStateChange;
import gg.xp.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.events.misc.RawEventStorage;
import gg.xp.events.misc.Stats;
import gg.xp.events.models.XivCombatant;
import gg.xp.events.models.XivEntity;
import gg.xp.events.models.XivPlayerCharacter;
import gg.xp.events.models.XivZone;
import gg.xp.events.slf4j.LogCollector;
import gg.xp.events.slf4j.LogEvent;
import gg.xp.events.state.XivState;
import gg.xp.events.ws.ActWsConnectionStatusChangedEvent;
import gg.xp.events.ws.WsState;
import gg.xp.gui.tables.CustomColumn;
import gg.xp.gui.tables.CustomTableModel;
import gg.xp.gui.tables.TableWithFilterAndDetails;
import gg.xp.gui.tables.filters.EventAbilityOrBuffFilter;
import gg.xp.gui.tables.filters.EventClassFilterFilter;
import gg.xp.gui.tables.filters.EventEntityFilter;
import gg.xp.gui.tables.filters.EventTypeFilter;
import gg.xp.gui.tables.filters.LogLevelVisualFilter;
import gg.xp.gui.tables.filters.SystemEventFilter;
import gg.xp.gui.tree.TopologyTreeEditor;
import gg.xp.gui.tree.TopologyTreeModel;
import gg.xp.gui.tree.TopologyTreeRenderer;
import gg.xp.sys.XivMain;
import gg.xp.util.Utils;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
			JFrame frame = new JFrame("Triggevent");
			JTabbedPane tabPane = new JTabbedPane();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(960, 720);
			frame.setVisible(true);
			JPanel mainPanel = new SystemTabPanel();
			Component scrollPanel = new JScrollPane(mainPanel);
			tabPane.addTab("System", scrollPanel);
//			tabPane.addTab("System", mainPanel);
			JPanel stats = new StatsPanel();
			// TODO: move this to a panel in first page
			tabPane.addTab("Stats", stats);
			tabPane.addTab("Plugins", new PluginTopologyPanel());
			tabPane.addTab("Combatants", getCombatantsPanel());
			tabPane.addTab("Events", getEventsPanel());
			tabPane.addTab("ACT Log", getActLogPanel());
			tabPane.addTab("System Log", getSystemLogPanel());
			tabPane.addTab("Import/Export", new JPanel());
			frame.add(tabPane);
		});
	}

	private class SystemTabPanel extends JPanel {
		SystemTabPanel() {

			setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.anchor = GridBagConstraints.CENTER;
			c.weightx = 1;
			c.weighty = 0;
//			c.weightx = 1; c.weighty = 0.5;
//			c.gridx = 0;
			c.gridy = 0;
//			c.gridwidth = 1;
//			setLayout(new FlowLayout());
			ActWsConnectionStatus connectionStatusPanel = new ActWsConnectionStatus();
			connectionStatusPanel.setPreferredSize(new Dimension(100, 80));
			add(connectionStatusPanel, c);
			XivStateStatus xivStateStatus = new XivStateStatus();
			xivStateStatus.setPreferredSize(new Dimension(100, 200));
			c.gridy++;
//			c.gridwidth = 1;
			add(xivStateStatus, c);
			// filler for alignment
			CombatantsPanel combatantsPanel = new CombatantsPanel();
//			c.gridx = 0;
			c.gridy++;
			c.weighty = 1;
			c.weighty = 1;
			add(combatantsPanel, c);
//			combatantsPanel.setPreferredSize(new Dimension(100, 100));
//			BoxLayout mgr = new BoxLayout(this, BoxLayout.PAGE_AXIS);
//			setLayout(mgr);
////			setLayout(new FlowLayout());
//			ActWsConnectionStatus connectionStatusPanel = new ActWsConnectionStatus();
//			connectionStatusPanel.setMaximumSize(new Dimension(32768, 100));
//			add(connectionStatusPanel);
//			XivStateStatus xivStateStatus = new XivStateStatus();
//			xivStateStatus.setMaximumSize(new Dimension(32768, 400));
//			add(xivStateStatus);
//			// filler for alignment
//			TitleBorderFullsizePanel combatantsPanel = new CombatantsPanel();
//			add(combatantsPanel);
////			combatantsPanel.setPreferredSize(new Dimension(100, 100));
			// TODO: these don't work right because we aren't guaranteed to be the last event handler
			master.getDistributor().registerHandler(ActWsConnectionStatusChangedEvent.class, connectionStatusPanel::connectionStatusChange);
			master.getDistributor().registerHandler(XivStateChange.class, (ctx, e) -> xivStateStatus.refresh());
			master.getDistributor().registerHandler(XivStateChange.class, (ctx, e) -> combatantsPanel.refresh());
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

	private class XivStateStatus extends JPanel implements Refreshable {

		private final List<Refreshable> displayed = new ArrayList<>();
		private final CustomTableModel<XivPlayerCharacter> partyTableModel;

		public XivStateStatus() {

			setLayout(new GridLayout(1, 2));

			KeyValueDisplaySet leftItems = new KeyValueDisplaySet(List.of(
					new KeyValuePairDisplay<>(
							"Player Name",
							new JLabel(),
							() -> {
								XivPlayerCharacter player = state.get(XivState.class).getPlayer();
								return player == null ? "null" : player.getName();
							},
							JLabel::setText
					),
					new KeyValuePairDisplay<>(
							"Zone Name",
							new JLabel(),
							() -> {
								XivZone zone = state.get(XivState.class).getZone();
								return zone == null ? "null" : zone.getName();
							},
							JLabel::setText
					),
					new KeyValuePairDisplay<>(
							"Player Job",
							new JLabel(),
							() -> {
								XivPlayerCharacter player = state.get(XivState.class).getPlayer();
								return player == null ? "null" : player.getJob().getFriendlyName();
							},
							JLabel::setText
					),
					new KeyValuePairDisplay<>(
							"Player Level",
							new JLabel(),
							() -> {
								XivPlayerCharacter player = state.get(XivState.class).getPlayer();
								return player == null ? "null" : Long.toString(player.getLevel());
							},
							JLabel::setText
					),
					new KeyValuePairDisplay<>(
							"Player World",
							new JLabel(),
							() -> {
								XivPlayerCharacter player = state.get(XivState.class).getPlayer();
								return player == null ? "null" : player.getWorld().toString();
							},
							JLabel::setText
					)
			));
			displayed.add(leftItems);
			add(new TitleBorderFullsizePanel("Player Status", leftItems));

			JPanel right = new TitleBorderFullsizePanel("Party Status");
			partyTableModel = CustomTableModel.builder(
//					() -> List.of(new XivPlayerCharacter(123, "Foo Bar", Job.WHM, new XivWorld(), 23, true)))
							() -> state.get(XivState.class).getPartyList())
					.addColumn(new CustomColumn<>("Name", XivEntity::getName))
					.addColumn(new CustomColumn<>("Job", c -> c.getJob().getFriendlyName()))
					.addColumn(new CustomColumn<>("ID", c -> Long.toString(c.getId(), 16)))
					.build();
			JTable partyMembersTable = new JTable(8, 3);
			partyMembersTable.setModel(partyTableModel);
			right.setLayout(new BorderLayout());
			JScrollPane scrollPane = new JScrollPane(partyMembersTable);
			right.add(scrollPane);
			add(right);
			refresh();
		}

		public void refresh() {
			displayed.forEach(Refreshable::refresh);
			partyTableModel.refresh();
		}
	}

	private class CombatantsPanel extends TitleBorderFullsizePanel {

		private final CustomTableModel<XivCombatant> combatantsTableModel;

		public CombatantsPanel() {
			super("Combatants");
			setLayout(new BorderLayout());
			combatantsTableModel = CustomTableModel.builder(
							() -> new ArrayList<>(state.get(XivState.class).getCombatants().values()))
					.addColumn(new CustomColumn<>("ID", c -> Long.toString(c.getId(), 16)))
					.addColumn(new CustomColumn<>("Name", XivEntity::getName))
					.addColumn(new CustomColumn<>("Is Player", XivCombatant::isPc))
					.build();
			JTable table = new JTable(combatantsTableModel);
			JScrollPane scrollPane = new JScrollPane(table);
			add(scrollPane);
		}

		public void refresh() {
			combatantsTableModel.refresh();
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

	private JPanel getCombatantsPanel() {
		// TODO: jump to parent button
		// Main table
		XivState state = container.getComponent(XivState.class);
		TableWithFilterAndDetails<XivCombatant, Map.Entry<Field, Object>> table = TableWithFilterAndDetails.builder("Events", state::getCombatantsListCopy,
						combatant -> {
							if (combatant == null) {
								return Collections.emptyList();
							}
							else {
								return Utils.dumpAllFields(combatant)
										.entrySet()
										.stream()
										.filter(e -> !"serialVersionUID".equals(e.getKey().getName()))
										.collect(Collectors.toList());
							}
						})
				.addMainColumn(new CustomColumn<>("ID", xivCombatant -> Long.toString(xivCombatant.getId(), 16)))
				.addMainColumn(new CustomColumn<>("Name", XivEntity::getName))
				.addMainColumn(new CustomColumn<>("Type", c -> {
					if (c.isThePlayer()) {
						return "YOU";
					}
					else {
						return c.getType();
					}
				}))
				.addMainColumn(new CustomColumn<>("Name", XivCombatant::getHp))
				.addMainColumn(new CustomColumn<>("Name", XivCombatant::getPos))
				.addDetailsColumn(new CustomColumn<>("Field", e -> e.getKey().getName()))
				.addDetailsColumn(new CustomColumn<>("Value", Map.Entry::getValue))
				.addDetailsColumn(new CustomColumn<>("Field Type", e -> e.getKey().getGenericType()))
				.addDetailsColumn(new CustomColumn<>("Declared In", e -> e.getKey().getDeclaringClass().getSimpleName()))
				// TODO: time range filter
//				.addFilter(EventTypeFilter::new)
//				.addFilter(SystemEventFilter::new)
//				.addFilter(EventClassFilterFilter::new)
//				.addFilter(EventEntityFilter::sourceFilter)
//				.addFilter(EventEntityFilter::targetFilter)
//				.addFilter(EventAbilityOrBuffFilter::new)
				.build();
		master.getDistributor().registerHandler(XivStateRecalculatedEvent.class, (ctx, e) -> table.signalUpdate());
		return table;

	}

	private JPanel getEventsPanel() {
		// TODO: jump to parent button
		// Main table
		RawEventStorage rawStorage = container.getComponent(RawEventStorage.class);
		TableWithFilterAndDetails<Event, Map.Entry<Field, Object>> table = TableWithFilterAndDetails.builder("Events", rawStorage::getEvents,
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
				.addMainColumn(new CustomColumn<>("Time", event -> {
					return event.getHappenedAt()
							.atZone(ZoneId.systemDefault())
							.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
//					return DateTimeFormatter.ISO_TIME.format(event.getHappenedAt());
////					return event.getHappenedAt();
				}, col -> {
					col.setMaxWidth(100);
				}))
				.addMainColumn(new CustomColumn<>("Type", e -> e.getClass().getSimpleName()))
				.addMainColumn(new CustomColumn<>("Source", e -> e instanceof HasSourceEntity ? ((HasSourceEntity) e).getSource().getName() : null))
				.addMainColumn(new CustomColumn<>("Target", e -> e instanceof HasTargetEntity ? ((HasTargetEntity) e).getTarget().getName() : null))
				.addMainColumn(new CustomColumn<>("Buff/Ability", e -> {
					if (e instanceof HasAbility) {
						return ((HasAbility) e).getAbility().getName();
					}
					if (e instanceof HasStatusEffect) {
						return ((HasStatusEffect) e).getBuff().getName();
					}
					return null;
				}))
				.addMainColumn(new CustomColumn<>("Parent", e -> {
					Event parent = e.getParent();
					return parent == null ? null : parent.getClass().getSimpleName();
				}))
				// TODO: is this column useful?
//				.addMainColumn(new CustomColumn<>("toString", Object::toString))
				.addDetailsColumn(new CustomColumn<>("Field", e -> e.getKey().getName()))
				.addDetailsColumn(new CustomColumn<>("Value", Map.Entry::getValue))
				.addDetailsColumn(new CustomColumn<>("Field Type", e -> e.getKey().getGenericType()))
				.addDetailsColumn(new CustomColumn<>("Declared In", e -> e.getKey().getDeclaringClass().getSimpleName()))
				// TODO: time range filter
				.addFilter(EventTypeFilter::new)
				.addFilter(SystemEventFilter::new)
				.addFilter(EventClassFilterFilter::new)
				.addFilter(EventEntityFilter::sourceFilter)
				.addFilter(EventEntityFilter::targetFilter)
				.addFilter(EventAbilityOrBuffFilter::new)
				.build();
		master.getDistributor().registerHandler(Event.class, (ctx, e) -> table.signalUpdate());
		return table;

	}

	private JPanel getActLogPanel() {
		RawEventStorage rawStorage = container.getComponent(RawEventStorage.class);
		TableWithFilterAndDetails<ACTLogLineEvent, Map.Entry<Field, Object>> table = TableWithFilterAndDetails.builder("ACT Log",
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
		master.getDistributor().registerHandler(Event.class, (ctx, e) -> {
			table.signalUpdate();
		});
		return table;
	}

	private JPanel getSystemLogPanel() {
		LogCollector instance = LogCollector.getInstance();
		if (instance == null) {
			JPanel panel = new TitleBorderFullsizePanel("Logs");
			panel.add(new JLabel("Error: no LogCollector instance"));
			return panel;
		}
		else {
			TableWithFilterAndDetails<LogEvent, Map.Entry<Field, Object>> table = TableWithFilterAndDetails.builder("System Log",
							instance::getEvents,
							e -> {
								if (e == null) {
									return Collections.emptyList();
								}
								return Stream.concat(
										Utils.dumpAllFields(e).entrySet().stream(),
										Utils.dumpAllFields(e.getEvent()).entrySet().stream()
								).collect(Collectors.toList());
							})
					// TODO: timestamp?
					.addMainColumn(new CustomColumn<>("Thread", e -> e.getEvent().getThreadName(), col -> {
						col.setPreferredWidth(150);
					}))
					// TODO: column widths
					.addMainColumn(new CustomColumn<>("Level", e -> e.getEvent().getLevel(), col -> {
						col.setMinWidth(50);
						col.setMaxWidth(50);
						col.setResizable(false);
					}))
					.addMainColumn(new CustomColumn<>("Where", e -> {
						StackTraceElement callerDataTop = e.getEvent().getCallerData()[0];
						String className = callerDataTop.getClassName();
						String[] split = className.split("\\.");
						String simpleClassName = split[split.length - 1];
						return simpleClassName + ":" + callerDataTop.getLineNumber();
//						return e.getEvent().getLoggerName() + ":";
					}, col -> {
						col.setPreferredWidth(200);
					}))
					.addMainColumn(new CustomColumn<>("Line", LogEvent::getEncoded, col -> {
						col.setPreferredWidth(700);
					}))
					.addDetailsColumn(new CustomColumn<>("Field", e -> e.getKey().getName()))
					.addDetailsColumn(new CustomColumn<>("Value", Map.Entry::getValue))
					.addDetailsColumn(new CustomColumn<>("Field Type", e -> e.getKey().getGenericType()))
					.addDetailsColumn(new CustomColumn<>("Declared In", e -> e.getKey().getDeclaringClass().getSimpleName()))
					.addFilter(LogLevelVisualFilter::new)
					.build();
			instance.addCallback(table::signalUpdate);
			return table;
		}
	}


	private static void installCustomEventQueue() {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		if (toolkit.isDynamicLayoutActive()) {
			toolkit.setDynamicLayout(true);
		}
		EventQueue queue = toolkit.getSystemEventQueue();
		queue.push(new EventQueue() {
			@Override
			protected void dispatchEvent(AWTEvent event) {
				long timeBefore = System.currentTimeMillis();
				try {
					super.dispatchEvent(event);
				}
				finally {
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
