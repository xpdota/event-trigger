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
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
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
import gg.xp.xivsupport.gui.tables.filters.PullNumberFilter;
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
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingGui;
import gg.xp.xivsupport.slf4j.LogCollector;
import gg.xp.xivsupport.slf4j.LogEvent;
import gg.xp.xivsupport.speech.TtsRequest;
import gg.xp.xivsupport.sys.XivMain;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
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
import java.util.stream.IntStream;
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
		try {
			MutablePicoContainer pico = XivMain.masterInit();
			pico.addComponent(GuiMain.class);
			pico.getComponent(GuiMain.class);
			// TODO: doesn't transfer over to test modes
			installCustomEventQueue();
		}
		catch (Throwable e) {
			log.error("Startup Error!", e);
			JFrame frame = new JFrame("Startup Error!");
			JPanel panel = new JPanel();
			panel.setBorder(new LineBorder(Color.RED));
			panel.setPreferredSize(new Dimension(800, 600));
			panel.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.CENTER;
			c.gridx = 0;
			c.gridy = 0;
			c.weightx = 1;
			c.weighty = 0;
			panel.setAlignmentX(0.5f);
			panel.add(new JLabel("A Fatal Error Has Occurred"), c);
			c.gridy++;
			c.weighty = 1;
			c.fill = GridBagConstraints.BOTH;
			JTextArea textArea = new JTextArea();
			textArea.setText("You should report this as a bug and include log files in " +
					System.getenv("APPDATA") +
					System.getProperty("file.separator") +
					"triggevent" +
					" as well as this error message." +
					"\n\n" +
					"You can also try moving/renaming the properties files in that directory to see if this error is being caused by a problem with your settings." +
					"\n\n" +
					ExceptionUtils.getStackTrace(e)
			);
			textArea.setEditable(false);
			textArea.setLineWrap(true);
			textArea.setWrapStyleWord(true);
			JScrollPane scroll = new JScrollPane(textArea);
			scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
			panel.add(scroll, c);
			c.weighty = 0;
			c.gridy++;
			c.fill = GridBagConstraints.NONE;
			JButton exit = new JButton("Exit");
			exit.addActionListener(l -> System.exit(1));
			panel.add(exit, c);
			frame.add(panel);
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.validate();
			frame.pack();
			frame.setVisible(true);

		}
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
			frame.setLocationByPlatform(true);
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
			c.weighty = 0;
			c.gridx = 0;
			c.gridy = 0;
			c.weightx = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;

			ActWsConnectionStatus connectionStatusPanel = new ActWsConnectionStatus();
			connectionStatusPanel.setPreferredSize(new Dimension(100, 80));
			add(connectionStatusPanel, c);

			c.gridy++;
			c.weightx = 0;
			c.gridwidth = 1;
			XivStateStatus xivStateStatus = new XivStateStatus();
			xivStateStatus.setMinimumSize(new Dimension(200, 200));
			xivStateStatus.setPreferredSize(xivStateStatus.getMinimumSize());
//			xivStateStatus.setPreferredSize(new Dimension(100, 250));
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
			master.getDistributor().registerHandler(ActWsConnectionStatusChangedEvent.class, connectionStatusPanel::connectionStatusChange);
			master.getDistributor().registerHandler(XivStateRecalculatedEvent.class, (ctx, e) -> {
				xivStateStatus.refresh();
				xivPartyPanel.refresh();
				combatantsPanel.refresh();
			});
			master.getDistributor().registerHandler(BuffApplied.class, (ctx, e) -> {
				xivStateStatus.refresh();
				xivPartyPanel.refresh();
				combatantsPanel.refresh();
			});
			master.getDistributor().registerHandler(BuffRemoved.class, (ctx, e) -> {
				xivStateStatus.refresh();
				xivPartyPanel.refresh();
				combatantsPanel.refresh();
			});
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

	private class XivStateStatus extends TitleBorderFullsizePanel implements Refreshable {

		private final List<Refreshable> refreshables = new ArrayList<>();

		public XivStateStatus() {

			super("Player Status");

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
			refreshables.add(leftItems);
			add(leftItems);

			refresh();
		}

		public void refresh() {
			refreshables.forEach(Refreshable::refresh);
		}
	}

	private class XivPartyPanel extends TitleBorderFullsizePanel implements Refreshable {

		private final CustomTableModel<XivPlayerCharacter> partyTableModel;

		public XivPartyPanel() {
			super("Party Status");
			setLayout(new BorderLayout());
			partyTableModel = CustomTableModel.builder(
							() -> state.get(XivState.class).getPartyList())
					.addColumn(StandardColumns.nameJobColumn)
					.addColumn(StandardColumns.statusEffectsColumn(container.getComponent(StatusEffectRepository.class)))
					.addColumn(StandardColumns.hpColumn)
					.addColumn(StandardColumns.mpColumn)
					.setSelectionEquivalence((a, b) -> a.getId() == b.getId())
					.build();
			JTable partyMembersTable = new JTable(8, 3);
			// TODO: see above todo, remove this when done

			partyMembersTable.setModel(partyTableModel);
			partyTableModel.configureColumns(partyMembersTable);
//			right.setLayout(new BorderLayout());
			JScrollPane scrollPane = new JScrollPane(partyMembersTable);
			add(scrollPane);
//			add(right);
			refresh();
		}

		public void refresh() {
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
					.addColumn(StandardColumns.nameJobColumn)
					.addColumn(StandardColumns.parentNameJobColumn)
					.addColumn(StandardColumns.combatantTypeColumn)
					.addColumn(StandardColumns.combatantRawTypeColumn)
					.addColumn(StandardColumns.hpColumn)
					.addColumn(StandardColumns.mpColumn)
					.addColumn(StandardColumns.posColumn)
					.addColumn(StandardColumns.entityIdColumn)
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

	private class AdvancedPanel extends JPanel implements Refreshable {
		private final KeyValueDisplaySet displayed;
		private final KeyValueDisplaySet mem;

		public AdvancedPanel() {
//			super("Advanced");
			setLayout(new GridBagLayout());

			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			c.anchor = GridBagConstraints.NORTH;
			c.weightx = 1;
			c.weighty = 0;
			c.fill = GridBagConstraints.HORIZONTAL;

			Stats stats = container.getComponent(Stats.class);
			RawEventStorage storage = container.getComponent(RawEventStorage.class);

			JPanel statsPanel = new TitleBorderFullsizePanel("Stats");
			statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.PAGE_AXIS));
			JButton refreshButton = new JButton("Refresh");
			refreshButton.addActionListener(e -> refresh());
			List<KeyValuePairDisplay<?, ?>> leftItems = List.of(
					new KeyValuePairDisplay<>(
							"Duration",
							new JTextArea(1, 15),
							() -> stats.getDuration().toString(),
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Total",
							new JTextArea(1, 15),
							() -> String.valueOf(stats.getTotal()),
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Primo",
							new JTextArea(1, 15),
							() -> String.valueOf(stats.getPrimogenitor()),
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Synthetic",
							new JTextArea(1, 15),
							() -> String.valueOf(stats.getSynthetic()),
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"In-Memory",
							new JTextArea(1, 15),
							() -> String.valueOf(storage.getEvents().size()),
							JTextArea::setText
					)
			);
			displayed = new KeyValueDisplaySet(leftItems);
			statsPanel.add(new WrapperPanel(refreshButton));
			statsPanel.add(displayed);
			statsPanel.add(new IntSettingGui(storage.getMaxEventsStoredSetting(), "Max In-Memory Events").getComponent());
			statsPanel.setPreferredSize(new Dimension(300, 300));
			add(statsPanel, c);


			TitleBorderFullsizePanel memoryPanel = new TitleBorderFullsizePanel("Memory Info");
			memoryPanel.setLayout(new BoxLayout(memoryPanel, BoxLayout.PAGE_AXIS));

			MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
			List<KeyValuePairDisplay<?, ?>> memoryItems = List.of(
					new KeyValuePairDisplay<>(
							"Heap Used",
							new JTextArea(1, 15),
							() -> (memoryMXBean.getHeapMemoryUsage().getUsed() >> 20) + "M",
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Heap Committed",
							new JTextArea(1, 15),
							() -> (memoryMXBean.getHeapMemoryUsage().getCommitted() >> 20) + "M",
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Heap Max",
							new JTextArea(1, 15),
							() -> (memoryMXBean.getHeapMemoryUsage().getMax() >> 20) + "M",
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Other Used",
							new JTextArea(1, 15),
							() -> (memoryMXBean.getNonHeapMemoryUsage().getUsed() >> 20) + "M",
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Other Committed",
							new JTextArea(1, 15),
							() -> (memoryMXBean.getNonHeapMemoryUsage().getCommitted() >> 20) + "M",
							JTextArea::setText
					)
			);
			JButton forceGcButton = new JButton("Force GC");
			forceGcButton.addActionListener(l -> {
				exs.submit(System::gc);
				exs.submit(() -> SwingUtilities.invokeLater(this::refresh));
			});
			memoryPanel.add(new WrapperPanel(forceGcButton));
			mem = new KeyValueDisplaySet(memoryItems);
			memoryPanel.add(mem);
			memoryPanel.setPreferredSize(new Dimension(300, 300));
			c.gridx++;
			add(memoryPanel, c);

			JPanel diskStoragePanel = new TitleBorderFullsizePanel("Disk Storage");
			BooleanSettingGui saveCheckbox = new BooleanSettingGui(storage.getSaveToDisk(), "Save to Disk");
			diskStoragePanel.setLayout(new BoxLayout(diskStoragePanel, BoxLayout.PAGE_AXIS));
			diskStoragePanel.add(new WrapperPanel(saveCheckbox.getComponent()));
			JButton flushButton = new JButton("Flush");
			flushButton.addActionListener(l -> storage.flushToDisk());
			diskStoragePanel.add(new WrapperPanel(flushButton));
			diskStoragePanel.setPreferredSize(new Dimension(300, 150));
			c.gridx = 0;
			c.gridy++;
			c.weighty = 1;
			add(diskStoragePanel, c);

			refresh();
		}

		@Override
		public void refresh() {
			displayed.refresh();
			mem.refresh();
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
		StatusEffectRepository statuses = container.getComponent(StatusEffectRepository.class);
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
				.addMainColumn(StandardColumns.statusEffectsColumn(statuses))
				.addMainColumn(StandardColumns.parentNameJobColumn)
				.addMainColumn(StandardColumns.combatantTypeColumn)
				.addMainColumn(StandardColumns.combatantRawTypeColumn)
				.addMainColumn(StandardColumns.hpColumn)
				.addMainColumn(StandardColumns.mpColumn)
				.addMainColumn(StandardColumns.posColumn)
				.addDetailsColumn(StandardColumns.fieldName)
				.addDetailsColumn(StandardColumns.fieldValue)
				.addDetailsColumn(StandardColumns.identity)
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
		master.getDistributor().registerHandler(BuffApplied.class, (ctx, e) -> table.signalNewData());
		master.getDistributor().registerHandler(BuffRemoved.class, (ctx, e) -> table.signalNewData());
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
				.addDetailsColumn(StandardColumns.identity)
				.addDetailsColumn(StandardColumns.fieldType)
				.addDetailsColumn(StandardColumns.fieldDeclaredIn)
				// TODO: time range filter
				.addFilter(EventTypeFilter::new)
				.addFilter(SystemEventFilter::new)
				.addFilter(EventClassFilterFilter::new)
				.addFilter(EventEntityFilter::eventSourceFilter)
				.addFilter(EventEntityFilter::eventTargetFilter)
				.addFilter(EventAbilityOrBuffFilter::new)
				.addFilter(r -> new PullNumberFilter(pulls, r))
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
				.addDetailsColumn(StandardColumns.identity)
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
				.addMainColumn(new CustomColumn<>("Zone", p -> p.getZone().getName(), col -> {
					col.setPreferredWidth(200);
				}))
				.addMainColumn(new CustomColumn<>("Status", Pull::getStatus, col -> {
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
					if (delta > 100) {
						log.warn("Slow GUI performance: took {}ms to dispatch event {}", delta, event);
					}
				}
			}
		});
	}
}
