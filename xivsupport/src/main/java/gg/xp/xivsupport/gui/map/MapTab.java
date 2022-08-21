package gg.xp.xivsupport.gui.map;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.reevent.util.Utils;
import gg.xp.xivsupport.events.actlines.events.MapChangeEvent;
import gg.xp.xivsupport.events.actlines.events.XivBuffsUpdatedEvent;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
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
public class MapTab extends JSplitPane {

	private final TableWithFilterAndDetails<XivCombatant, Map.Entry<Field, Object>> table;
	private final RefreshLoop<MapTab> mapRefresh;
	private final MapPanel mapPanel;
	private volatile boolean selectionRefreshPending;

	public MapTab(XivState state, StandardColumns columns, ActiveCastRepository acr) {
//		super("Map");
		super(HORIZONTAL_SPLIT);
		this.mapPanel = new MapPanel(state, acr);
		setPreferredSize(getMaximumSize());
//		setLayout(new BorderLayout());
		setRightComponent(mapPanel);
//		add(panel);
		JPanel controls = new JPanel(new BorderLayout());

		table = TableWithFilterAndDetails.builder("Combatants",
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
//				.addMainColumn(StandardColumns.parentNameJobColumn)
				.addMainColumn(StandardColumns.combatantTypeColumn)
				.addMainColumn(columns.hpColumnWithUnresolved())
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
				.addFilter(GroovyFilter.forClass(XivCombatant.class, "it"))
				.build();
		table.setBottomScroll(false);
		mapRefresh = new RefreshLoop<>("MapTableRefresh", this, MapTab::updateMapPanel, u -> 100L);

		controls.add(table, BorderLayout.CENTER);
		setLeftComponent(controls);
		setDividerLocation(0.35);
		setResizeWeight(0.25);

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
		mapRefresh.start();
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
		}
	}


	private void updateMapPanel() {
		mapPanel.setCombatants(table.getMainModel().getData());
	}

	private void signalUpdate() {
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
		mapPanel.mapChange(event);
	}

}
