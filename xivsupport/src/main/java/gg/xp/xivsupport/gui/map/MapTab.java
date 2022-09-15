package gg.xp.xivsupport.gui.map;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.reevent.util.Utils;
import gg.xp.xivsupport.events.actlines.events.MapChangeEvent;
import gg.xp.xivsupport.events.actlines.events.XivBuffsUpdatedEvent;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.EventEntityFilter;
import gg.xp.xivsupport.gui.tables.filters.GroovyFilter;
import gg.xp.xivsupport.gui.tables.filters.NonCombatEntityFilter;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

@ScanMe
public class MapTab extends JPanel {

	private final TableWithFilterAndDetails<XivCombatant, Map.Entry<Field, Object>> table;
	private final RefreshLoop<MapTab> mapRefresh;
	private final MapPanel mapPanel;
	private final MapDataController mapDataController;
	private final MapDataScrubber scrubber;
	private final Component configPanel;
	private final JSplitPane split;
	private volatile boolean selectionRefreshPending;

	public MapTab(GroovyManager mgr, MapDataController mdc, MapConfig config) {
//		super("Map");
		super(new BorderLayout());
		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		split.setOneTouchExpandable(true);
		this.mapDataController = mdc;
		this.mapPanel = new MapPanel(mdc);
//		setPreferredSize(getMaximumSize());
//		setLayout(new BorderLayout());
		split.setRightComponent(mapPanel);
//		add(panel);
		JPanel controls = new JPanel(new BorderLayout());

		table = TableWithFilterAndDetails.builder("Combatants",
						() -> mdc.getCombatants().stream().sorted(Comparator.comparing(XivEntity::getId)).collect(Collectors.toList()),
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
				.addMainColumn(StandardColumns.statusEffectsColumn(mdc::buffsOnCombatant))
//				.addMainColumn(StandardColumns.parentNameJobColumn)
				.addMainColumn(StandardColumns.combatantTypeColumn)
				// HP comes from the Combatant object directly, no need to do any funny business here
				.addMainColumn(StandardColumns.hpColumnWithUnresolved(mdc::unresolvedDamage))
//				.addMainColumn(StandardColumns.mpColumn)
//				.addMainColumn(StandardColumns.posColumn)
				.addDetailsColumn(StandardColumns.fieldName)
				.addDetailsColumn(StandardColumns.fieldValue)
				.addDetailsColumn(StandardColumns.identity)
				.addDetailsColumn(StandardColumns.fieldType)
				.addDetailsColumn(StandardColumns.fieldDeclaredIn)
				.setSelectionEquivalence((a, b) -> a.getId() == b.getId())
				.setDetailsSelectionEquivalence((a, b) -> a.getKey().equals(b.getKey()))
				.addFilter(EventEntityFilter::selfFilter)
				.addFilter(NonCombatEntityFilter::new)
				.addFilter(GroovyFilter.forClass(XivCombatant.class, mgr, "it"))
				.build();
		table.setBottomScroll(false);
		mapRefresh = new RefreshLoop<>("MapTableRefresh", this, MapTab::updateMapPanel, u -> 100L);

		controls.add(table, BorderLayout.CENTER);
		split.setLeftComponent(controls);
		split.setDividerLocation(0.35);
		split.setResizeWeight(0.25);

		table.getMainTable().getSelectionModel().addListSelectionListener(l -> {
			if (l.getValueIsAdjusting()) {
				return;
			}
			if (!selectionRefreshPending) {
				selectionRefreshPending = true;
				SwingUtilities.invokeLater(() -> {
					mapPanel.setSelection(table.getCurrentSelection());
					selectionRefreshPending = false;
				});
			}
		});

		//noinspection ConstantConditions
		SwingUtilities.invokeLater(() -> table.getSplitPane().setDividerLocation(0.75));

		mapPanel.setSelectionCallback(table::setAndScrollToSelection);
		scrubber = new MapDataScrubber(mdc, this::toggleSettings);
		mdc.setCallback(() -> {
			refreshIfVisible();
			scrubber.repaint();
		});
		mapRefresh.start();
		add(split);
		add(scrubber, BorderLayout.NORTH);
		configPanel = config.makeComponent();
		configPanel.setVisible(false);
		add(configPanel, BorderLayout.EAST);
	}

	@Override
	public void setVisible(boolean vis) {
		if (vis) {
			table.signalNewData();
		}
		super.setVisible(vis);
	}

	private void refreshIfVisible() {
		if (table.getMainTable().isShowing()) {
			table.signalNewData();
			scrubber.repaint();
		}
	}


	private void updateMapPanel() {
		mapPanel.setCombatants(table.getMainModel().getData());
	}

	private void signalUpdate() {
		mapDataController.captureSnapshot();
		refreshIfVisible();
	}

	@HandleEvents
	public void stateRecalc(EventContext ctx, XivStateRecalculatedEvent event) {
		signalUpdate();
	}

	@HandleEvents
	public void buffRecalc(EventContext ctx, XivBuffsUpdatedEvent event) {
		signalUpdate();
	}

	@HandleEvents
	public void mapChange(EventContext context, MapChangeEvent event) {
		signalUpdate();
	}

	public void toggleSettings() {
		double before = split.getResizeWeight();
		split.setResizeWeight(0);
		configPanel.setVisible(!configPanel.isVisible());
		SwingUtilities.invokeLater(() -> split.setResizeWeight(before));
	}
}
