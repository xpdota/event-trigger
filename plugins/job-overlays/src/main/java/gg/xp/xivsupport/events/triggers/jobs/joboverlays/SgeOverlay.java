package gg.xp.xivsupport.events.triggers.jobs.joboverlays;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivdata.data.Cooldown;
import gg.xp.xivdata.data.StatusEffectLibrary;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.CdTracker;
import gg.xp.xivsupport.events.state.combatstate.CooldownHelper;
import gg.xp.xivsupport.events.state.combatstate.CooldownStatus;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.jobs.gui.BaseCdTrackerTable;
import gg.xp.xivsupport.events.triggers.jobs.gui.VisualCdInfo;
import gg.xp.xivsupport.events.triggers.jobs.gui.VisualCdInfoMain;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.renderers.ComponentListRenderer;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.ScaledImageComponent;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class SgeOverlay extends BaseJobOverlay {
	private static final int ICON_SIZE = 40;
	private final XivState state;
	private final StatusEffectRepository buffs;
	private final CdTracker cooldowns;
	private final CooldownHelper cdh;
	private final ComponentListRenderer listRenderer = new ComponentListRenderer(4);
	private @Nullable XivPlayerCharacter kardionTarget;

	private final CustomTableModel<VisualCdInfo> tableModel;
	private volatile List<VisualCdInfo> croppedCds = Collections.emptyList();

	public SgeOverlay(XivState state, StatusEffectRepository buffs, CdTracker cooldowns, CooldownHelper cdh) {
		this.state = state;
		this.buffs = buffs;
		this.cooldowns = cooldowns;
		this.cdh = cdh;
		setPreferredSize(new Dimension(250, 100));
		add(listRenderer);
		listRenderer.setBounds(0, 5, 250, 50);
		BaseCdTrackerTable tableHolder = new BaseCdTrackerTable(() -> croppedCds);
		tableModel = tableHolder.getTableModel();
		JTable table = tableHolder.getTable();
		table.setBounds(0, 60, 250, 40);
		add(table);
	}

	@HandleEvents
	public void buffs(EventContext ctx, BuffApplied ba) {
		updateKardion();
	}

	@HandleEvents
	public void buffs(EventContext ctx, BuffRemoved ba) {
		updateKardion();
	}

	@HandleEvents
	public void init(EventContext ctx, InitEvent init) {
		updateKardion();
	}
//	@HandleEvents

	private void updateKardion() {
		// Kardion target
		kardionTarget = (XivPlayerCharacter) buffs.getBuffs().stream().filter(ba -> {
			long id = ba.getBuff().getId();
			return ba.getSource().isThePlayer()
					&& (id == 0xA2D);
		}).findAny().map(BuffApplied::getTarget).orElse(null);
		// If Soteria is active, display that
		@Nullable ScaledImageComponent kardiaIcon = IconTextRenderer.getIconOnly(buffs.statusesOnTarget(state.getPlayer()).stream().filter(ba -> ba.getBuff().getId() == 0xA32)
				.findAny()
				.map(ba -> StatusEffectLibrary.iconForId(ba.getBuff().getId(), ba.getStacks()))
				.orElse(StatusEffectLibrary.iconForId(0xA2D, 0)));
		if (kardionTarget == null) {
			listRenderer.setComponents(Collections.singletonList(kardiaIcon.withNewSize(ICON_SIZE)));
		}
		else {
			listRenderer.setComponents(List.of(kardiaIcon.withNewSize(ICON_SIZE), IconTextRenderer.getIconOnly(kardionTarget.getJob()).withNewSize(ICON_SIZE)));
		}
		SwingUtilities.invokeLater(this::repaint);
	}

	@Override
	protected void onBecomeVisible() {
		updateKardion();
		periodicRefresh();
	}

	@Override
	protected void periodicRefresh() {
		recalcCds();
		SwingUtilities.invokeLater(tableModel::fullRefresh);
	}

	private void recalcCds() {
		CooldownStatus personalCd = cdh.getPersonalCd(Cooldown.Phlegma);
		VisualCdInfo vci;
		if (personalCd == null) {
			// Not used yet - display placeholder
			vci = new VisualCdInfoMain(Cooldown.Phlegma);
		}
		else {
			vci = new VisualCdInfoMain(personalCd);
		}
		croppedCds = Collections.singletonList(vci);
	}
}
