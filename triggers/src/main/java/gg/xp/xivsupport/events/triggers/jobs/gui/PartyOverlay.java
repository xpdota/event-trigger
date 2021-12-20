package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.jobs.StatusEffectRepository;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.gui.tables.renderers.NameJobRenderer;
import gg.xp.xivsupport.gui.tables.renderers.StatusEffectListRenderer;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ScanMe
public class PartyOverlay extends XivOverlay {

	private static final Logger log = LoggerFactory.getLogger(PartyOverlay.class);

	private static final int numberOfRows = 8;
	private final XivState state;

	private final StatusEffectRepository statuses;
	private final CustomTableModel<XivPlayerCharacter> tableModel;
	private final JTable table;
	private volatile List<XivPlayerCharacter> party = Collections.emptyList();


	private static final CustomColumn<XivCombatant> nameJobColumnTransparent
			= new CustomColumn<>("Name", c -> c, c -> {
		c.setCellRenderer(new NameJobRenderer(true, true));
		c.setPreferredWidth(125);
	});
	private CustomColumn<XivCombatant> statusEffectsColumn() {
		return new CustomColumn<>("Statuses", entity -> statuses.statusesOnTarget(entity).stream()
				.map(BuffApplied::getBuff)
				.collect(Collectors.toList()), c -> {
			c.setCellRenderer(new StatusEffectListRenderer());
			c.setPreferredWidth(300);
		});
	}

	public PartyOverlay(PersistenceProvider persistence, EventDistributor dist, XivState state, StatusEffectRepository statuses, StandardColumns columns) {
		super("Party Overlay", "party-overlay", persistence);
		this.state = state;
		this.statuses = statuses;
		tableModel = CustomTableModel.builder(() -> party)
				.addColumn(nameJobColumnTransparent)
				.addColumn(columns.hpColumnWithUnresolved().withExtraConfig(c -> c.setPreferredWidth(150)))
				.addColumn(columns.statusEffectsColumn().withExtraConfig(c -> c.setPreferredWidth(200)))
//				.addColumn(StandardColumns.mpColumn)
				.build();
//		getPanel().setPreferredSize();
		table = new JTable(tableModel);
		table.setBackground(null);
//		table.getPreferredSize();
		table.setOpaque(false);
		table.setCellSelectionEnabled(false);
		tableModel.configureColumns(table);
		getPanel().add(table);
		dist.registerHandler(XivStateRecalculatedEvent.class, (ctx, e) -> refresh());
		dist.registerHandler(AbilityUsedEvent.class, (ctx, e) -> refresh());
//		dist.registerHandler(AbilityResolvedEvent.class, (ctx, e) -> table.signalNewData());
		dist.registerHandler(BuffApplied.class, (ctx, e) -> refresh());
		dist.registerHandler(BuffRemoved.class, (ctx, e) -> refresh());
		repackSize();
	}

	private void repackSize() {
		table.setPreferredSize(new Dimension(table.getPreferredSize().width, table.getRowHeight() * numberOfRows + 1));
		getFrame().revalidate();
		redoScale();
	}


	private void getAndSort() {
		if (!getEnabled().get()) {
			party = Collections.emptyList();
			return;
		}
		// For testing
//		party = state.getCombatantsListCopy().stream().filter(XivCombatant::isCombative).sorted(Comparator.comparing(XivEntity::getId)).limit(8).collect(Collectors.toList());
		party = state.getPartyList();
	}


	private void refresh() {
		getAndSort();
		tableModel.signalNewData();
	}
}
