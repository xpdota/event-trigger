package gg.xp.xivsupport.gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import gg.xp.reevent.context.StateStore;
import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.util.Utils;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.actlines.events.XivBuffsUpdatedEvent;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.events.misc.RawEventStorage;
import gg.xp.xivsupport.events.misc.Stats;
import gg.xp.xivsupport.events.misc.pulls.Pull;
import gg.xp.xivsupport.events.misc.pulls.PullTracker;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.jobs.StatusEffectRepository;
import gg.xp.xivsupport.events.ws.ActWsConnectionStatusChangedEvent;
import gg.xp.xivsupport.events.ws.WsState;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.overlay.OverlayMain;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.ActLineFilter;
import gg.xp.xivsupport.gui.tables.filters.EventAbilityOrBuffFilter;
import gg.xp.xivsupport.gui.tables.filters.EventClassFilterFilter;
import gg.xp.xivsupport.gui.tables.filters.EventEntityFilter;
import gg.xp.xivsupport.gui.tables.filters.EventTypeFilter;
import gg.xp.xivsupport.gui.tables.filters.LogLevelVisualFilter;
import gg.xp.xivsupport.gui.tables.filters.NonCombatEntityFilter;
import gg.xp.xivsupport.gui.tables.filters.SystemEventFilter;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.gui.tables.renderers.NameJobRenderer;
import gg.xp.xivsupport.gui.tree.TopologyTreeEditor;
import gg.xp.xivsupport.gui.tree.TopologyTreeModel;
import gg.xp.xivsupport.gui.tree.TopologyTreeRenderer;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.slf4j.LogCollector;
import gg.xp.xivsupport.slf4j.LogEvent;
import gg.xp.xivsupport.speech.TtsRequest;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GuiMain {

	private static final Logger log = LoggerFactory.getLogger(GuiMain.class);
	private static final ExecutorService exs = Executors.newCachedThreadPool();
	private final EventMaster master;
	private final StateStore state;
	private final PicoContainer container;
	private JTabbedPane tabPane;

	public static void main(String[] args) {
		log.info("GUI Init");
		log.info("Classpath: {}", System.getProperty("java.class.path"));
		try {
//			UIManager.setLookAndFeel(new DarculaLaf());
			UIManager.setLookAndFeel(new FlatDarculaLaf());
		}
		catch (Throwable t) {
			log.error("Error setting up look and feel", t);
		}
		MutablePicoContainer pico = XivMain.masterInit();
		pico.addComponent(GuiMain.class);
		pico.getComponent(GuiMain.class);
		// TODO: doesn't transfer over to test modes
		installCustomEventQueue();
	}

	public GuiMain(EventMaster master, MutablePicoContainer container) {
		log.info("Starting GUI setup");
		this.master = master;
		this.state = master.getDistributor().getStateStore();
		this.container = container;
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Triggevent");
			tabPane = new JTabbedPane();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(960, 720);
			frame.setVisible(true);
			frame.add(tabPane);
		});
		SwingUtilities.invokeLater(() -> tabPane.addTab("System", new SystemTabPanel()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Plugins", new PluginTopologyPanel()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Plugin Settings", new PluginSettingsPanel()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Combatants", getCombatantsPanel()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Buffs", getStatusEffectsPanel()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Events", getEventsPanel()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("ACT Log", getActLogPanel()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("System Log", getSystemLogPanel()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Pulls", getPullsTab()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Advanced", new AdvancedPanel()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Import/Export", new JPanel()));
//		container.addComponent(OverlayMain.class);
//		container.getComponent(OverlayMain.class);
	}

	private class SystemTabPanel extends JPanel {
		SystemTabPanel() {

			setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.anchor = GridBagConstraints.CENTER;
			c.weightx = 1;
			c.weighty = 0;
			c.gridy = 0;
			ActWsConnectionStatus connectionStatusPanel = new ActWsConnectionStatus();
			connectionStatusPanel.setPreferredSize(new Dimension(100, 80));
			add(connectionStatusPanel, c);
			XivStateStatus xivStateStatus = new XivStateStatus();
			xivStateStatus.setPreferredSize(new Dimension(100, 250));
			c.gridy++;
			add(xivStateStatus, c);
			// filler for alignment
			CombatantsPanel combatantsPanel = new CombatantsPanel();
			c.gridy++;
			c.weighty = 1;
			add(combatantsPanel, c);
			// TODO: these don't always work right because we aren't guaranteed to be the last event handler
			master.getDistributor().registerHandler(ActWsConnectionStatusChangedEvent.class, connectionStatusPanel::connectionStatusChange);
			master.getDistributor().registerHandler(XivStateRecalculatedEvent.class, (ctx, e) -> xivStateStatus.refresh());
			master.getDistributor().registerHandler(XivStateRecalculatedEvent.class, (ctx, e) -> combatantsPanel.refresh());
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
			JButton testTts = new JButton("Test TTS");
			testTts.addActionListener(e -> master.pushEvent(new TtsRequest("Test")));
			add(testTts);
//			setMinimumSize(new Dimension(500, 500));
//			setMaximumSize(new Dimension(500, 500));
//			repaint();
			updateGui();
		}

		public void connectionStatusChange(EventContext context, ActWsConnectionStatusChangedEvent event) {
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
					.addColumn(StandardColumns.jobOnlyColumn)
					// TODO: seeing custom renderer here does not work, because this would normally be read by
					// TableWithFilterAndDetails, but that isn't in use here.
					.addColumn(StandardColumns.hpColumn)
					.addColumn(StandardColumns.entityIdColumn)
					.setSelectionEquivalence((a, b) -> a.getId() == b.getId())
					.build();
			JTable partyMembersTable = new JTable(8, 3);
			// TODO: see above todo, remove this when done

			partyMembersTable.setModel(partyTableModel);
			partyTableModel.configureColumns(partyMembersTable);
			right.setLayout(new BorderLayout());
			JScrollPane scrollPane = new JScrollPane(partyMembersTable);
			right.add(scrollPane);
			add(right);
			refresh();
		}

		public void refresh() {
			displayed.forEach(Refreshable::refresh);
			partyTableModel.fullRefresh();
		}
	}

	private class CombatantsPanel extends TitleBorderFullsizePanel {

		private final CustomTableModel<XivCombatant> combatantsTableModel;

		public CombatantsPanel() {
			super("Combatants");
			setLayout(new BorderLayout());
			combatantsTableModel = CustomTableModel.builder(
							() -> state.get(XivState.class).getCombatantsListCopy()
									.stream()
									.filter(XivCombatant::isCombative)
									.sorted(Comparator.comparing(XivEntity::getId))
									.collect(Collectors.toList()))
					.addColumn(StandardColumns.entityIdColumn)
					.addColumn(StandardColumns.nameJobColumn)
					.addColumn(StandardColumns.parentNameJobColumn)
					.addColumn(StandardColumns.combatantTypeColumn)
					.addColumn(StandardColumns.combatantRawTypeColumn)
					.addColumn(StandardColumns.hpColumn)
					.addColumn(StandardColumns.mpColumn)
					.addColumn(StandardColumns.posColumn)
					.setSelectionEquivalence((a, b) -> a.getId() == b.getId())
					.build();
			JTable table = new JTable(combatantsTableModel);
			combatantsTableModel.configureColumns(table);
			JScrollPane scrollPane = new JScrollPane(table);
			add(scrollPane);
		}

		public void refresh() {
			combatantsTableModel.fullRefresh();
		}
	}

	private class AdvancedPanel extends TitleBorderFullsizePanel implements Refreshable {
		private final KeyValueDisplaySet displayed;

		public AdvancedPanel() {
			super("Advanced");

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

	private class PluginSettingsPanel extends TitleBorderFullsizePanel {

		private final JTabbedPane tabPanel;

		public PluginSettingsPanel() {
			super("Plugin Settings");
			setLayout(new BorderLayout());
			tabPanel = new JTabbedPane();
			add(tabPanel);
			exs.submit(this::getAndAddTabs);
		}

		private void addTab(PluginTab tab) {
			String tabName = tab.getTabName();
			log.info("Adding Plugin Tab {}", tabName);
			tabPanel.addTab(tabName, tab.getTabContents());
		}

		private void addTabs(List<PluginTab> tabs) {
			tabs.forEach(tab -> SwingUtilities.invokeLater(() -> addTab(tab)));
		}

		private void getAndAddTabs() {
			while (true) {
				// Kinda bad...
				try {
					List<PluginTab> components = container.getComponents(PluginTab.class);
					SwingUtilities.invokeLater(() -> this.addTabs(components));
					return;
				}
				catch (ConcurrentModificationException ignored) {
					try {
						Thread.sleep(1000);
					}
					catch (InterruptedException e) {
						// ignored
					}
				}
			}
		}

	}

	private JPanel getCombatantsPanel() {
		// TODO: jump to parent button
		// Main table
		XivState state = container.getComponent(XivState.class);
		TableWithFilterAndDetails<XivCombatant, Map.Entry<Field, Object>> table = TableWithFilterAndDetails.builder("Combatants",
						() -> state.getCombatantsListCopy().stream().sorted(Comparator.comparing(XivEntity::getId)).collect(Collectors.toList()),
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
				.addMainColumn(StandardColumns.entityIdColumn)
				.addMainColumn(StandardColumns.nameJobColumn)
				.addMainColumn(StandardColumns.parentNameJobColumn)
				.addMainColumn(StandardColumns.combatantTypeColumn)
				.addMainColumn(StandardColumns.combatantRawTypeColumn)
				.addMainColumn(StandardColumns.hpColumn)
				.addMainColumn(StandardColumns.mpColumn)
				.addMainColumn(StandardColumns.posColumn)
				.addDetailsColumn(StandardColumns.fieldName)
				.addDetailsColumn(StandardColumns.fieldValue)
				.addDetailsColumn(StandardColumns.fieldType)
				.addDetailsColumn(StandardColumns.fieldDeclaredIn)
				.setSelectionEquivalence((a, b) -> a.getId() == b.getId())
				.setDetailsSelectionEquivalence((a, b) -> a.getKey().equals(b.getKey()))
				// TODO: combat vs noncombat filter
//				.addFilter(EventTypeFilter::new)
//				.addFilter(SystemEventFilter::new)
//				.addFilter(EventClassFilterFilter::new)
				.addFilter(EventEntityFilter::selfFilter)
				.addFilter(NonCombatEntityFilter::new)
//				.addFilter(EventEntityFilter::targetFilter)
//				.addFilter(EventAbilityOrBuffFilter::new)
				.build();
		table.setBottomScroll(false);
		master.getDistributor().registerHandler(XivStateRecalculatedEvent.class, (ctx, e) -> table.signalNewData());
		return table;

	}

	private JPanel getStatusEffectsPanel() {
		// TODO: jump to parent button
		// Main table
		StatusEffectRepository repo = container.getComponent(StatusEffectRepository.class);
		TableWithFilterAndDetails<BuffApplied, Map.Entry<Field, Object>> table = TableWithFilterAndDetails.builder("Status Effects", repo::getBuffs,
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
//				.addMainColumn(new CustomColumn<>("Type", e -> e.getClass().getSimpleName()))
				.addMainColumn(new CustomColumn<>("Source", BuffApplied::getSource, c -> c.setCellRenderer(new NameJobRenderer())))
				.addMainColumn(new CustomColumn<>("Target", BuffApplied::getTarget, c -> c.setCellRenderer(new NameJobRenderer())))
				.addMainColumn(new CustomColumn<>("Buff/Ability", e -> e.getBuff().getName()))
				.addMainColumn(new CustomColumn<>("Initial Duration", buffApplied -> {
					double duration = buffApplied.getDuration();
					if (duration >= 9998.0d && duration <= 9999.9d) {
						return "∞";
					}
					return duration;
				}, c -> {
					c.setMinWidth(100);
					c.setMaxWidth(100);
				}))
				.addDetailsColumn(StandardColumns.fieldName)
				.addDetailsColumn(StandardColumns.fieldValue)
				.addDetailsColumn(StandardColumns.fieldType)
				.addDetailsColumn(StandardColumns.fieldDeclaredIn)
				.setSelectionEquivalence(Object::equals)
				.addFilter(EventEntityFilter::buffSourceFilter)
				.addFilter(EventEntityFilter::buffTargetFilter)
				.addFilter(EventAbilityOrBuffFilter::new)
				.build();
		table.setBottomScroll(false);
		master.getDistributor().registerHandler(XivBuffsUpdatedEvent.class, (ctx, e) -> table.signalNewData());
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
				.addMainColumn(new CustomColumn<>("Source", e -> e instanceof HasSourceEntity ? ((HasSourceEntity) e).getSource() : null, c -> c.setCellRenderer(new NameJobRenderer())))
				.addMainColumn(new CustomColumn<>("Target", e -> e instanceof HasTargetEntity ? ((HasTargetEntity) e).getTarget() : null, c -> c.setCellRenderer(new NameJobRenderer())))
				.addMainColumn(new CustomColumn<>("Buff/Ability", e -> {
					if (e instanceof HasAbility) {
						return ((HasAbility) e).getAbility();
					}
					if (e instanceof HasStatusEffect) {
						return ((HasStatusEffect) e).getBuff();
					}
					return null;
				}, c -> {
					c.setCellRenderer(new ActionAndStatusRenderer());
				}))
				.addMainColumn(new CustomColumn<>("Parent", e -> {
					Event parent = e.getParent();
					return parent == null ? null : parent.getClass().getSimpleName();
				}))
				.addDetailsColumn(StandardColumns.fieldName)
				.addDetailsColumn(StandardColumns.fieldValue)
				.addDetailsColumn(StandardColumns.fieldType)
				.addDetailsColumn(StandardColumns.fieldDeclaredIn)
				// TODO: time range filter
				.addFilter(EventTypeFilter::new)
				.addFilter(SystemEventFilter::new)
				.addFilter(EventClassFilterFilter::new)
				.addFilter(EventEntityFilter::eventSourceFilter)
				.addFilter(EventEntityFilter::eventTargetFilter)
				.addFilter(EventAbilityOrBuffFilter::new)
				// TODO: put this everywhere applicable
				.setAppendOrPruneOnly(true)
				.build();
		master.getDistributor().registerHandler(Event.class, (ctx, e) -> table.signalNewData());
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
				.addDetailsColumn(StandardColumns.fieldName)
				.addDetailsColumn(StandardColumns.fieldValue)
				.addDetailsColumn(StandardColumns.fieldType)
				.addDetailsColumn(StandardColumns.fieldDeclaredIn)
				.addFilter(ActLineFilter::new)
				.setAppendOrPruneOnly(true)
				.build();
		master.getDistributor().registerHandler(Event.class, (ctx, e) -> {
			table.signalNewData();
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
					.addMainColumn(new CustomColumn<>("Time",
							e -> Instant.ofEpochMilli(e.getEvent().getTimeStamp())
									.atZone(ZoneId.systemDefault())
									.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")), col -> {
						col.setMinWidth(80);
						col.setMaxWidth(80);
					}))
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
					.addDetailsColumn(StandardColumns.fieldName)
					.addDetailsColumn(StandardColumns.fieldValue)
					.addDetailsColumn(StandardColumns.fieldType)
					.addDetailsColumn(StandardColumns.fieldDeclaredIn)
					.addFilter(LogLevelVisualFilter::new)
					.setAppendOrPruneOnly(true)
					.build();
			instance.addCallback(table::signalNewData);
			return table;
		}
	}

	private JPanel getPullsTab() {
		PullTracker pulls = state.get(PullTracker.class);
		TableWithFilterAndDetails<Pull, Map.Entry<Field, Object>> table = TableWithFilterAndDetails.builder("Pulls",
						pulls::getPulls,
						currentPull -> {
							if (currentPull == null) {
								return Collections.emptyList();
							}
							else {
								return Utils.dumpAllFields(currentPull)
										.entrySet()
										.stream()
										.filter(e -> !"serialVersionUID".equals(e.getKey().getName()))
										.collect(Collectors.toList());
							}
						})
				.addMainColumn(new CustomColumn<>("Zone", e -> e.getZone().getName(), col -> {
					col.setPreferredWidth(200);
				}))
				.addMainColumn(new CustomColumn<>("Status", e -> e.getStatus(), col -> {
					col.setMinWidth(100);
					col.setMaxWidth(100);
					col.setResizable(false);
				}))
				.addMainColumn(new CustomColumn<>("Start", e -> e.startTime(), col -> {
					col.setPreferredWidth(200);
				}))
				.addMainColumn(new CustomColumn<>("Duration", e -> e.getCombatDuration(), col -> {
					col.setPreferredWidth(200);
				}))
				.addDetailsColumn(StandardColumns.fieldName)
				.addDetailsColumn(StandardColumns.fieldValue)
				.addDetailsColumn(StandardColumns.fieldType)
				.addDetailsColumn(StandardColumns.fieldDeclaredIn)
//				.addFilter(LogLevelVisualFilter::new)
				.setAppendOrPruneOnly(false)
				.build();
		return table;
	}


	// TODO: unhandled exception logger
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
