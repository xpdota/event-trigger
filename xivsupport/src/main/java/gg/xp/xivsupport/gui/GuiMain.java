package gg.xp.xivsupport.gui;

import ch.qos.logback.classic.Level;
import gg.xp.reevent.context.StateStore;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventHandler;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.util.Utils;
import gg.xp.xivdata.data.XivMap;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.HasEffects;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.actlines.events.XivBuffsUpdatedEvent;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.events.misc.RawEventStorage;
import gg.xp.xivsupport.events.misc.pulls.Pull;
import gg.xp.xivsupport.events.misc.pulls.PullTracker;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.XivStateImpl;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.ws.ActWsConnectionStatusChangedEvent;
import gg.xp.xivsupport.events.ws.WsState;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.groovy.GroovyScriptManager;
import gg.xp.xivsupport.gui.map.MapTab;
import gg.xp.xivsupport.gui.overlay.OverlayConfig;
import gg.xp.xivsupport.gui.overlay.OverlayMain;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomRightClickOption;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.GlobalGuiOptions;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.AbilityResolutionFilter;
import gg.xp.xivsupport.gui.tables.filters.ActLineFilter;
import gg.xp.xivsupport.gui.tables.filters.EventAbilityOrBuffFilter;
import gg.xp.xivsupport.gui.tables.filters.EventClassFilterFilter;
import gg.xp.xivsupport.gui.tables.filters.EventEntityFilter;
import gg.xp.xivsupport.gui.tables.filters.GroovyFilter;
import gg.xp.xivsupport.gui.tables.filters.LogLevelVisualFilter;
import gg.xp.xivsupport.gui.tables.filters.NonCombatEntityFilter;
import gg.xp.xivsupport.gui.tables.filters.PullNumberFilter;
import gg.xp.xivsupport.gui.tables.filters.SystemEventFilter;
import gg.xp.xivsupport.gui.tables.renderers.AbilityEffectListRenderer;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.gui.tables.renderers.NameJobRenderer;
import gg.xp.xivsupport.gui.tabs.AdvancedTab;
import gg.xp.xivsupport.gui.tabs.GroovyTab;
import gg.xp.xivsupport.gui.tabs.LibraryTab;
import gg.xp.xivsupport.gui.tabs.SmartTabbedPane;
import gg.xp.xivsupport.gui.tabs.UpdatesPanel;
import gg.xp.xivsupport.gui.util.CatchFatalError;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.replay.ReplayController;
import gg.xp.xivsupport.replay.gui.ReplayAdvancePseudoFilter;
import gg.xp.xivsupport.replay.gui.ReplayControllerGui;
import gg.xp.xivsupport.slf4j.LogCollector;
import gg.xp.xivsupport.slf4j.LogEvent;
import gg.xp.xivsupport.speech.TtsRequest;
import gg.xp.xivsupport.sys.Threading;
import gg.xp.xivsupport.sys.XivMain;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.MouseEvent;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@SuppressWarnings({"ReturnOfNull", "CodeBlock2Expr"})
public class GuiMain {

	private static final Logger log = LoggerFactory.getLogger(GuiMain.class);
	private static final ExecutorService exs = Executors.newCachedThreadPool(Threading.namedDaemonThreadFactory("GuiMain"));
	private final EventMaster master;
	private final StateStore state;
	private final MutablePicoContainer container;
	private final StandardColumns columns;
	private final @Nullable ReplayController replay;
	private final RightClickOptionRepo rightClicks;
	private final GlobalGuiOptions globalGuiOpts;
	private JTabbedPane tabPane;
	private Component eventPanel;


	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		log.info("GUI Init");
		log.info("Classpath: {}", System.getProperty("java.class.path"));
		CatchFatalError.run(() -> {
			log.info("GUI Setup");
			CommonGuiSetup.setup();
			log.info("Master Init");
			MutablePicoContainer pico = XivMain.masterInit();
			pico.addComponent(GuiMain.class);
			log.info("GUI Init");
			pico.getComponent(GuiMain.class);
			long end = System.currentTimeMillis();
			log.info("Total base startup time: {}", end - start);
		});
	}

	public GuiMain(EventMaster master, MutablePicoContainer container) {
		log.info("Starting GUI setup");
		this.master = master;
		this.state = master.getDistributor().getStateStore();
		this.container = container;
		this.rightClicks = container.getComponent(RightClickOptionRepo.class);
		columns = container.getComponent(StandardColumns.class);
		replay = container.getComponent(ReplayController.class);
		globalGuiOpts = container.getComponent(GlobalGuiOptions.class);
		WindowConfig wc = container.getComponent(WindowConfig.class);
		long start = System.currentTimeMillis();
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Triggevent");
			tabPane = new SmartTabbedPane();
			frame.setLayout(new BorderLayout());
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//			frame.setLocationByPlatform(true);
			frame.setSize(1280, 960);
			frame.setLocationRelativeTo(null);
			if (wc.getStartMinimized().get() && replay == null) {
				frame.setState(JFrame.ICONIFIED);
			}
			frame.setVisible(true);
			frame.add(tabPane);
			if (replay != null) {
				frame.add(new ReplayControllerGui(container, replay).getPanel(), BorderLayout.PAGE_START);
			}
		});
		SwingUtilities.invokeLater(() -> tabPane.addTab("General", new SystemTabPanel()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Plugin Settings", new PluginSettingsPanel()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Combatants", getCombatantsPanel()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Buffs", getStatusEffectsPanel()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Events", (eventPanel = getEventsPanel())));
		SwingUtilities.invokeLater(() -> tabPane.addTab("ACT Log", getActLogPanel()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("System Log", getSystemLogPanel()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Pulls", getPullsTab()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Overlays", getOverlayConfigTab()));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Map", container.getComponent(MapTab.class)));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Library", container.getComponent(LibraryTab.class)));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Groovy", new GroovyTab(container.getComponent(GroovyScriptManager.class))));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Updates", new UpdatesPanel(container.getComponent(PersistenceProvider.class))));
		SwingUtilities.invokeLater(() -> tabPane.addTab("Advanced", new AdvancedTab(container)));
		SwingUtilities.invokeLater(() -> {
			long end = System.currentTimeMillis();
			log.info("GUI startup time: {}", end - start);
		});
	}


	private class SystemTabPanel extends JPanel {
		SystemTabPanel() {

			setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.anchor = GridBagConstraints.CENTER;
			c.weighty = 0;
			c.gridx = 0;
			c.gridy = 0;
			c.weightx = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;

			if (replay == null) {
				ActWsConnectionStatus connectionStatusPanel = new ActWsConnectionStatus();
				connectionStatusPanel.setPreferredSize(new Dimension(100, 80));
				add(connectionStatusPanel, c);
				master.getDistributor().registerHandler(ActWsConnectionStatusChangedEvent.class, connectionStatusPanel::connectionStatusChange);
			}

			c.gridy++;
			c.weightx = 0;
			c.gridwidth = 1;
			XivStateStatus xivStateStatus = new XivStateStatus();
			xivStateStatus.setMinimumSize(new Dimension(250, 200));
			xivStateStatus.setPreferredSize(xivStateStatus.getMinimumSize());
			add(xivStateStatus, c);

			c.gridx++;
			c.weightx = 1;
			XivPartyPanel xivPartyPanel = new XivPartyPanel();
			xivPartyPanel.setMinimumSize(new Dimension(1, 215));
			xivPartyPanel.setPreferredSize(new Dimension(32768, 300));
			add(xivPartyPanel, c);
			// filler for alignment
			CombatantsPanel combatantsPanel = new CombatantsPanel();
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.gridx = 0;
			c.gridy++;
			c.weighty = 1;
			add(combatantsPanel, c);
			// TODO: these don't always work right because we aren't guaranteed to be the last event handler
			EventHandler<Event> update = (ctx, e) -> {
				xivStateStatus.refresh();
				xivPartyPanel.refresh();
				combatantsPanel.refresh();
			};
			master.getDistributor().registerHandler(XivStateRecalculatedEvent.class, update);
			master.getDistributor().registerHandler(AbilityUsedEvent.class, update);
			master.getDistributor().registerHandler(BuffApplied.class, update);
			master.getDistributor().registerHandler(BuffRemoved.class, update);
		}
	}

	private class ActWsConnectionStatus extends TitleBorderFullsizePanel {

		private final KeyValueDisplaySet connectedDisp;

		public ActWsConnectionStatus() {
			super("System Status");
			JCheckBox box = new JCheckBox() {
				@Override
				protected void processMouseEvent(MouseEvent e) {
					// Ignore - lets us preserve the "enabled" look while being unclickable
				}
			};
			box.setFocusable(false);
			WsState wsState = state.get(WsState.class);
			Border defaultBorder = getBorder();
			Border badBorder = new TitledBorder(new LineBorder(Color.RED), "System Status");

			connectedDisp = new KeyValueDisplaySet(List.of(new KeyValuePairDisplay<>(
					"Connected to ACT WS",
					box,
					wsState::isConnected,
					(cb, connected) -> {
						cb.setSelected(connected);
						if (connected) {
							setBorder(defaultBorder);
						}
						else {
							setBorder(badBorder);
						}
					}
			)));
			add(connectedDisp);
			JButton testTts = new JButton("Test TTS");
			testTts.addActionListener(e -> master.pushEvent(new TtsRequest("Test")));
			add(testTts);
			updateGui();
		}

		public void connectionStatusChange(EventContext context, ActWsConnectionStatusChangedEvent event) {
			SwingUtilities.invokeLater(this::updateGui);
		}

		private void updateGui() {
			connectedDisp.refresh();
		}
	}

	private class XivStateStatus extends TitleBorderFullsizePanel implements Refreshable {

		private final List<Refreshable> refreshables = new ArrayList<>();

		public XivStateStatus() {

			super("Player Status");

			KeyValueDisplaySet leftItems = new KeyValueDisplaySet(List.of(
					new KeyValuePairDisplay<>(
							"Name",
							new JLabel(),
							() -> {
								XivPlayerCharacter player = state.get(XivStateImpl.class).getPlayer();
								return player == null ? "?" : player.getName();
							},
							JLabel::setText
					),
					new KeyValuePairDisplay<>(
							"Zone",
							new JLabel(),
							() -> {
								XivZone zone = state.get(XivStateImpl.class).getZone();
								return zone == null ? "?" : zone.getName();
							},
							JLabel::setText
					),
					new KeyValuePairDisplay<>(
							"Map",
							new JLabel(),
							() -> {
								XivMap map = state.get(XivStateImpl.class).getMap();
								return map == null ? "?" : map.getPlace();
							},
							JLabel::setText
					),
					new KeyValuePairDisplay<>(
							"Job",
							new JLabel(),
							() -> {
								XivPlayerCharacter player = state.get(XivStateImpl.class).getPlayer();
								return player == null ? "?" : player.getJob().getFriendlyName();
							},
							JLabel::setText
					),
					new KeyValuePairDisplay<>(
							"Level",
							new JLabel(),
							() -> {
								XivPlayerCharacter player = state.get(XivStateImpl.class).getPlayer();
								return player == null ? "?" : Long.toString(player.getLevel());
							},
							JLabel::setText
					)
			));
			refreshables.add(leftItems);
			add(leftItems);

			refresh();
		}

		@Override
		public void refresh() {
			SwingUtilities.invokeLater(() -> refreshables.forEach(Refreshable::refresh));
//			refreshables.forEach(r -> SwingUtilities.invokeLater(r::refresh));
		}
	}

	private class XivPartyPanel extends TitleBorderFullsizePanel implements Refreshable {

		private final CustomTableModel<XivPlayerCharacter> partyTableModel;

		public XivPartyPanel() {
			super("Party Status");
			setLayout(new BorderLayout());
			partyTableModel = CustomTableModel.builder(
							() -> state.get(XivStateImpl.class).getPartyList())
					.addColumn(StandardColumns.nameJobColumn)
					.addColumn(columns.statusEffectsColumn())
					.addColumn(columns.hpColumnWithUnresolved())
					.addColumn(StandardColumns.mpColumn)
					.setItemEquivalence((a, b) -> a.getId() == b.getId())
					.build();
			JTable partyMembersTable = new JTable(8, 3);

			partyMembersTable.setModel(partyTableModel);
			partyTableModel.configureColumns(partyMembersTable);
			RightClickOptionRepo.of(
					CustomRightClickOption.forRow(
							"Set As Primary Player",
							XivPlayerCharacter.class,
							p -> state.get(XivStateImpl.class).setPlayerTmpOverride(p))
			).configureTable(partyMembersTable, partyTableModel);
			JScrollPane scrollPane = new JScrollPane(partyMembersTable);
			add(scrollPane);
			refresh();
		}

		@Override
		public void refresh() {
			partyTableModel.signalNewData();
		}
	}


	private class CombatantsPanel extends TitleBorderFullsizePanel {

		private final CustomTableModel<XivCombatant> combatantsTableModel;
		private final XivState state;

		public CombatantsPanel() {
			super("Combatants");
			setLayout(new BorderLayout());
			state = GuiMain.this.state.get(XivStateImpl.class);
			combatantsTableModel = CustomTableModel.builder(
							() -> state.getCombatantsListCopy()
									.stream()
									.filter(XivCombatant::isCombative)
									.sorted(Comparator.comparing(XivEntity::getId))
									.collect(Collectors.toList()))
					.addColumn(StandardColumns.nameJobColumn)
					.addColumn(columns.statusEffectsColumn())
					.addColumn(StandardColumns.parentNameJobColumn)
					.addColumn(StandardColumns.combatantTypeColumn)
					.addColumn(columns.hpColumnWithUnresolved())
					.addColumn(StandardColumns.mpColumn)
					.addColumn(StandardColumns.posColumn)
					.setItemEquivalence((a, b) -> a.getId() == b.getId())
					.build();
			JTable table = new JTable(combatantsTableModel);
			combatantsTableModel.configureColumns(table);
			JScrollPane scrollPane = new JScrollPane(table);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			add(scrollPane);
		}

		public void refresh() {
			combatantsTableModel.signalNewData();
		}
	}


	private class PluginSettingsPanel extends JPanel {

		private final JTabbedPane tabPanel;

		public PluginSettingsPanel() {
//			super("Plugin Settings");
			setLayout(new BorderLayout());
			tabPanel = new SmartTabbedPane(SwingConstants.LEFT);
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

		@SuppressWarnings("BusyWait")
		private void getAndAddTabs() {
			List<PluginTab> components;
			while (true) {
				// Kinda bad...
				try {
					components = container.getComponents(PluginTab.class);
					break;
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
			List<PluginTab> allComponents = components;
			allComponents.sort(Comparator.comparing(PluginTab::getSortOrder));
			SwingUtilities.invokeLater(() -> this.addTabs(allComponents));
		}

	}

	private JPanel getCombatantsPanel() {
		// Main table
		XivState state = container.getComponent(XivStateImpl.class);
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
				.addMainColumn(columns.statusEffectsColumn())
				.addMainColumn(StandardColumns.parentNameJobColumn)
				.addMainColumn(StandardColumns.combatantTypeColumn)
				.addMainColumn(columns.hpColumnWithUnresolved())
				.addMainColumn(StandardColumns.mpColumn)
				.addMainColumn(StandardColumns.posColumn)
				.addDetailsColumn(StandardColumns.fieldName)
				.addDetailsColumn(StandardColumns.fieldValue)
				.addDetailsColumn(StandardColumns.identity)
				.addDetailsColumn(StandardColumns.fieldType)
				.addDetailsColumn(StandardColumns.fieldDeclaredIn)
				.setSelectionEquivalence((a, b) -> a.getId() == b.getId())
				.setDetailsSelectionEquivalence((a, b) -> a.getKey().equals(b.getKey()))
				.addFilter(EventEntityFilter::selfFilter)
				.addFilter(NonCombatEntityFilter::new)
				.build();
		table.setBottomScroll(false);
		master.getDistributor().registerHandler(XivStateRecalculatedEvent.class, (ctx, e) -> table.signalNewData());
		master.getDistributor().registerHandler(AbilityUsedEvent.class, (ctx, e) -> table.signalNewData());
		master.getDistributor().registerHandler(BuffApplied.class, (ctx, e) -> table.signalNewData());
		master.getDistributor().registerHandler(BuffRemoved.class, (ctx, e) -> table.signalNewData());
		return table;

	}

	private JPanel getStatusEffectsPanel() {
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
				.addMainColumn(new CustomColumn<>("Source", BuffApplied::getSource, c -> c.setCellRenderer(new NameJobRenderer())))
				.addMainColumn(new CustomColumn<>("Target", BuffApplied::getTarget, c -> c.setCellRenderer(new NameJobRenderer())))
				.addMainColumn(new CustomColumn<>("Buff/Ability", BuffApplied::getBuff, c -> c.setCellRenderer(new ActionAndStatusRenderer())))
				.addMainColumn(new CustomColumn<>("Initial Duration", buffApplied -> {
					long duration = buffApplied.getInitialDuration().getSeconds();
					if (duration >= 9998 && duration <= 10000) {
						return "∞";
					}
					return duration;
				}, c -> {
					c.setMinWidth(100);
					c.setMaxWidth(100);
				}))
				.addDetailsColumn(StandardColumns.fieldName)
				.addDetailsColumn(StandardColumns.fieldValue)
				.addDetailsColumn(StandardColumns.identity)
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
		PullTracker pulls = container.getComponent(PullTracker.class);
		ActionAndStatusRenderer asRenderer = ActionAndStatusRenderer.full();
		NameJobRenderer nameJobRenderer = new NameJobRenderer();
		BooleanSetting displayIdsSetting = globalGuiOpts.displayIds();
		displayIdsSetting.addAndRunListener(() -> asRenderer.setShowId(displayIdsSetting.get()));
		displayIdsSetting.addAndRunListener(() -> nameJobRenderer.setShowId(displayIdsSetting.get()));
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
					return event.getEffectiveHappenedAt()
							.atZone(ZoneId.systemDefault())
							.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
//					return DateTimeFormatter.ISO_TIME.format(event.getHappenedAt());
////					return event.getHappenedAt();
				}, col -> {
					col.setMaxWidth(100);
				}))
				.addMainColumn(new CustomColumn<>("Type", e -> e.getClass().getSimpleName()))
				.addMainColumn(new CustomColumn<>("Source", e -> e instanceof HasSourceEntity ? ((HasSourceEntity) e).getSource() : null, c -> c.setCellRenderer(nameJobRenderer)))
				.addMainColumn(new CustomColumn<>("Target", e -> e instanceof HasTargetEntity ? ((HasTargetEntity) e).getTarget() : null, c -> c.setCellRenderer(nameJobRenderer)))
				.addMainColumn(new CustomColumn<>("Buff/Ability", e -> {
					return e;
//					if (e instanceof HasAbility) {
//						return ((HasAbility) e).getAbility();
//					}
//					if (e instanceof HasStatusEffect) {
//						return ((HasStatusEffect) e).getBuff();
//					}
//					return null;
				}, c -> {
					c.setCellRenderer(asRenderer);
				}))
				.addMainColumn(new CustomColumn<>("Effects", e -> {
					if (e instanceof HasEffects event) {
						return event.getEffects();
					}
					return null;
				}, c -> c.setCellRenderer(new AbilityEffectListRenderer())))
				.addMainColumn(new CustomColumn<>("Parent", e -> {
					Event parent = e.getParent();
					return parent == null ? null : parent.getClass().getSimpleName();
				}))
				.addDetailsColumn(StandardColumns.fieldName)
				.addDetailsColumn(StandardColumns.fieldValue)
				.addDetailsColumn(StandardColumns.identity)
				.addDetailsColumn(StandardColumns.fieldType)
				.addDetailsColumn(StandardColumns.fieldDeclaredIn)
				.withRightClickRepo(rightClicks)
				.addFilter(SystemEventFilter::new)
				.addFilter(EventClassFilterFilter::new)
				.addFilter(AbilityResolutionFilter::new)
				.addFilter(EventEntityFilter::eventSourceFilter)
				.addFilter(EventEntityFilter::eventTargetFilter)
				.addFilter(EventAbilityOrBuffFilter::new)
//				.addFilter(FreeformEventFilter::new)
				.addFilter(GroovyFilter.forClass(Event.class, container.getComponent(GroovyManager.class)))
				.addFilter(r -> {
					PullNumberFilter pullNumberFilter = new PullNumberFilter(pulls, r);
					container.addComponent(pullNumberFilter);
					return pullNumberFilter;
				})
				.addWidget(unused -> new BooleanSettingGui(displayIdsSetting, "Show IDs", true).getComponent())
				.addWidget(replayNextPseudoFilter(Event.class))
				.setAppendOrPruneOnly(true)
				.build();

		displayIdsSetting.addListener(table::repaint);
		master.getDistributor().registerHandler(Event.class, (ctx, e) -> table.signalNewData());
		return table;

	}

	private JPanel getActLogPanel() {
		RawEventStorage rawStorage = container.getComponent(RawEventStorage.class);
		// The first way which this was implemented was by keeping its own list of events, but that meant that they
		// would never clear out.
		// The second way was to stream and filter the raw event storage, but that is inefficient because it scales
		// very poorly and causes higher CPU usage.
		// The third, not-hacky way is to just have RawEventStorage track events of a particular type for us
		TableWithFilterAndDetails<ACTLogLineEvent, Map.Entry<Field, Object>> table = TableWithFilterAndDetails.builder("ACT Log",
						() -> rawStorage.getEventsOfType(ACTLogLineEvent.class),
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
				.addDetailsColumn(StandardColumns.identity)
				.addDetailsColumn(StandardColumns.fieldType)
				.addDetailsColumn(StandardColumns.fieldDeclaredIn)
				.withRightClickRepo(rightClicks)
				.addFilter(ActLineFilter::new)
				.addWidget(replayNextPseudoFilter(ACTLogLineEvent.class))
				.setAppendOrPruneOnly(true)
				.build();
		master.getDistributor().registerHandler(ACTLogLineEvent.class, (ctx, e) -> {
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
					.addMainColumn(new CustomColumn<>("Level", e -> e.getEvent().getLevel(), col -> {
						col.setMinWidth(50);
						col.setMaxWidth(50);
						col.setResizable(false);
						col.setCellRenderer(new DefaultTableCellRenderer() {
							private final Color defaultFg = getForeground();

							@Override
							public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
								Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
								if (value == Level.ERROR) {
									setForeground(Color.RED);
								}
								else if (value == Level.WARN) {
									setForeground(Color.ORANGE);
								}
								else {
									setForeground(defaultFg);
								}
								return comp;
							}
						});
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
						col.setPreferredWidth(900);
					}))
					.withRightClickRepo(rightClicks)
					.addDetailsColumn(StandardColumns.fieldName)
					.addDetailsColumn(StandardColumns.fieldValue)
					.addDetailsColumn(StandardColumns.identity)
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
				.addMainColumn(new CustomColumn<>("Number", Pull::getPullNum, col -> {
					col.setMinWidth(50);
					col.setMaxWidth(50);
				}))
				.addMainColumn(new CustomColumn<>("Zone", p -> {
					XivZone zone = p.getZone();
					return zone == null ? "Unknown" : zone.getName();
				}, col -> {
					col.setPreferredWidth(200);
				}))
				.addMainColumn(new CustomColumn<>("Status", pull -> pull.getStatus().getFriendlyName(), col -> {
					col.setMinWidth(100);
					col.setMaxWidth(100);
					col.setResizable(false);
				}))
				.addMainColumn(new CustomColumn<>("Start", Pull::startTime, col -> {
					col.setPreferredWidth(200);
				}))
				.addMainColumn(new CustomColumn<>("Duration", Pull::getCombatDuration, col -> {
					col.setPreferredWidth(200);
				}))
				.addDetailsColumn(StandardColumns.fieldName)
				.addDetailsColumn(StandardColumns.fieldValue)
				.addDetailsColumn(StandardColumns.identity)
				.addDetailsColumn(StandardColumns.fieldType)
				.addDetailsColumn(StandardColumns.fieldDeclaredIn)
				.withRightClickRepo(RightClickOptionRepo.of(CustomRightClickOption.forRow("Filter Events Tab to This", Pull.class, pull -> {
					PullNumberFilter pnf = container.getComponent(PullNumberFilter.class);
					pnf.setPullNumberExternally(pull.getPullNum());
					// TODO: messy
					GuiUtil.bringToFront(eventPanel);
					IntStream.range(0, tabPane.getTabCount())
							.filter(i -> tabPane.getTitleAt(i).equals("Events"))
							.findFirst()
							.ifPresentOrElse(tabPane::setSelectedIndex, () -> log.error("Couldn't find Events tab"));
				})))
//				.addFilter(LogLevelVisualFilter::new)
				.setAppendOrPruneOnly(false)
				.build();
		new Thread(() -> {
			while (true) {
				table.signalNewData();
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}).start();
		return table;
	}

	private JPanel getOverlayConfigTab() {
		OverlayMain overlayMain = container.getComponent(OverlayMain.class);
		OverlayConfig oc = container.getComponent(OverlayConfig.class);
		TitleBorderFullsizePanel panel = new TitleBorderFullsizePanel("Overlays");
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		BooleanSetting visibleSetting = oc.getShow();
		{
			JPanel allOverlayControls = new JPanel();
			allOverlayControls.setLayout(new WrapLayout());


			allOverlayControls.add(new BooleanSettingGui(visibleSetting, "Show Overlays").getComponent());

			JCheckBox edit = new JCheckBox("Edit");
			edit.addActionListener(e -> overlayMain.setEditing(edit.isSelected()));
			allOverlayControls.add(edit);

			allOverlayControls.add(new BooleanSettingGui(oc.getForceShow(), "Force Visible Even When Game Inactive").getComponent());

			allOverlayControls.add(new IntSettingSpinner(oc.getMinFps(), "Min Overlay FPS").getComponent());
			allOverlayControls.add(new IntSettingSpinner(oc.getMaxFps(), "Max Overlay FPS").getComponent());

			panel.add(allOverlayControls, c);
			c.gridy++;
		}
		{
			// Will need to do something about this later
			JTable table = new JTable() {

				@Override
				public boolean isCellEditable(int row, int column) {
					// TODO: make this more official
					return !(getCellEditor(row, column) instanceof NoCellEditor);
				}
			};
			CustomTableModel<XivOverlay> tableModel = CustomTableModel.builder(overlayMain::getOverlays)
					.addColumn(StandardColumns.booleanSettingColumn("On", XivOverlay::getEnabled, 50, visibleSetting))
					.addColumn(new CustomColumn<>("Name", XivOverlay::getTitle, col -> col.setCellEditor(new NoCellEditor())))
					.addColumn(StandardColumns.longSettingBoxColumn("X", XivOverlay::getXSetting, 100))
					.addColumn(StandardColumns.longSettingBoxColumn("Y", XivOverlay::getYSetting, 100))
					.addColumn(StandardColumns.doubleSettingBoxColumn("Scale (0.8 minimum)", XivOverlay::getScaleSetting, 100))
					.addColumn(StandardColumns.doubleSettingSliderColumn("Opacity", XivOverlay::opacity, 200, 0.05))
					.build();
			table.setModel(tableModel);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			visibleSetting.addListener(() -> {
				TableCellEditor cellEditor = table.getCellEditor();
				if (cellEditor != null) {
					cellEditor.stopCellEditing();
				}
			});
			visibleSetting.addListener(panel::repaint);
			tableModel.configureColumns(table);
			JScrollPane scrollPane = new JScrollPane(table);
			scrollPane.setPreferredSize(scrollPane.getMaximumSize());
			c.weighty = 1;
			panel.add(scrollPane, c);
		}


		return panel;
	}

	private <X extends Event> @Nullable Function<TableWithFilterAndDetails<X, ?>, Component> replayNextPseudoFilter(Class<X> clazz) {
		if (replay == null) {
			return null;
		}
		return table -> new ReplayAdvancePseudoFilter<>(clazz, master, replay, table).getComponent();
	}

}
