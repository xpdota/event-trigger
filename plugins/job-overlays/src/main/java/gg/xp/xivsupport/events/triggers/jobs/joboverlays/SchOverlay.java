package gg.xp.xivsupport.events.triggers.jobs.joboverlays;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.ActionIcon;
import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivdata.data.Cooldown;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.XivBuffsUpdatedEvent;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.events.state.XivState;
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
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class SchOverlay extends BaseJobOverlay {

	private static final Logger log = LoggerFactory.getLogger(SchOverlay.class);

	private final ComponentListRenderer listRenderer = new ComponentListRenderer(4);
	private static final int ICON_SIZE = 40;
	private final XivState state;
	private final StatusEffectRepository buffs;
	private final CooldownHelper cdh;
	private @Nullable XivCombatant summonCombatant;
	private SchSummon summon = SchSummon.NONE;
	private @Nullable Component summonIcon;
	private List<Component> pendingActionIcons = Collections.emptyList();

	// Stuff for tracking AF CD
	private final CustomTableModel<VisualCdInfo> tableModel;
	private static final List<Cooldown> defaultCds = List.of(Cooldown.ChainStratagem, Cooldown.Aetherflow, Cooldown.Dissipation);
	private final List<VisualCdInfo> croppedCds = new ArrayList<>(3);
	{
		defaultCds.forEach(dcd -> croppedCds.add(new VisualCdInfoMain(dcd)));
	}

	private enum SchSummon {
		NONE(810),
		SELENE(0x4340),
		EOS(0x433f),
		SERAPH(0x40A1),
		DISSIPATION(0xe03);


		private @Nullable ScaledImageComponent icon;
		private final long actionIdForIcon;

		SchSummon(long actionIdForIcon) {
			this.actionIdForIcon = actionIdForIcon;
		}

		public @Nullable ScaledImageComponent getIcon() {
			if (icon == null) {
				ScaledImageComponent icon = IconTextRenderer.getIconOnly(ActionLibrary.iconForId(actionIdForIcon));
				if (icon == null) {
					this.icon = null;
				}
				else {
					this.icon = icon.withNewSize(ICON_SIZE);
				}
			}
			return icon;
		}
	}

	public SchOverlay(XivState state, StatusEffectRepository buffs, CooldownHelper cdh) {
		this.state = state;
		this.buffs = buffs;
		this.cdh = cdh;
		setPreferredSize(new Dimension(250, 120));
		add(listRenderer);
		listRenderer.setBounds(0, 5, 250, 50);
		BaseCdTrackerTable tableHolder = new BaseCdTrackerTable(() -> croppedCds);
		tableModel = tableHolder.getTableModel();
		JTable table = tableHolder.getTable();
		table.setBounds(0, 60, 250, 60);
		add(table);
	}

	/*
	Stuff I want to put on this overlay:
	Big warning if nothing summoned
	Indicator for AF CD
	Current Fey Union target

	Done:
	Current summon
	Currently queued pet actions

    */

	@HandleEvents
	public void handleStateUpdate(EventContext context, XivStateRecalculatedEvent event) {
		summonUpdate();
		uiUpdate();
	}

	@HandleEvents
	public void handleBuffsUpdate(EventContext context, XivBuffsUpdatedEvent event) {
		summonPendingActionsUpdate();
		uiUpdate();
	}

	// TODO: this isn't particularly useful - what we actually need is to see the action resolve
	// There isn't an action sync for pure heals unfortunately
	private void summonPendingActionsUpdate() {
		if (summonCombatant == null) {
			pendingActionIcons = Collections.emptyList();
		}
		else {
			List<BuffApplied> petBuffs = buffs.statusesOnTarget(summonCombatant);
			pendingActionIcons = petBuffs
					.stream()
					.map(this::summonOrderToActionMapping)
					.filter(Objects::nonNull)
					.map(IconTextRenderer::getIconOnly)
					.filter(Objects::nonNull)
					.map(icon -> icon.withNewSize(ICON_SIZE))
					.collect(Collectors.toList());
		}
	}

	private void uiUpdate() {
		List<Component> effectiveList = new ArrayList<>();
		if (summonIcon != null) {
			effectiveList.add(summonIcon);
		}
		effectiveList.addAll(pendingActionIcons);
		listRenderer.setComponents(effectiveList);
		SwingUtilities.invokeLater(this::repaint);
	}

	private @Nullable ActionIcon summonOrderToActionMapping(BuffApplied buff) {
		final long buffId = buff.getBuff().getId();
		final long actionId;
		/*
		Fey Blessing: 0x7AE
		Fey Illumination: 0x7AC
		Fey Union: 0x7AD
		Whispering Dawn: 0x77B

		Consolation: 0x7AD
		Whisper: 0x77B
		Seraphic Illumination: 0x7AC

		 */

		if (buffId == 0x77B) {
			if (summon == SchSummon.SERAPH) {
				actionId = 0x40A6;
			}
			else {
				actionId = 0x4099;
			}
		}
		else if (buffId == 0x7AC) {
			if (summon == SchSummon.SERAPH) {
				actionId = 0x40A7;
			}
			else {
				actionId = 0x409A;
			}
		}
		else if (buffId == 0x7AD) {
			if (summon == SchSummon.SERAPH) {
				actionId = 0x40A2;
			}
			else {
				actionId = 0x1D0D;
			}
		}
		else if (buffId == 0x7AE) {
			actionId = 0x409F;
		}
		else {
			return null;
		}
		return ActionLibrary.iconForId(actionId);
	}

	private void summonUpdate() {
		summonCombatant = getSummonCombatant();
		if (summonCombatant == null) {
			if (isDissipationActive()) {
				summon = SchSummon.DISSIPATION;
			}
			else {
				summon = SchSummon.NONE;
			}
		}
		else {
			if (summonCombatant.getbNpcId() == 0x3f1) {
				summon = SchSummon.SELENE;
			}
			else if (summonCombatant.getbNpcId() == 0x3f0) {
				summon = SchSummon.EOS;
			}
			else if (summonCombatant.getbNpcId() == 0x28f7) {
				summon = SchSummon.SERAPH;
			}
			else {
				// Just default to selene if we can't tell
				summon = SchSummon.SELENE;
			}
		}
		summonIcon = summon.getIcon();
	}

	private boolean isDissipationActive() {
		// Dissipation special case
		// TODO: find a way to not have this be called multiple times
		return buffs.statusesOnTarget(state.getPlayer()).stream().anyMatch(buff -> buff.getBuff().getId() == 0x317);
	}

	@Override
	protected void onBecomeVisible() {
		// Preload icons
		for (SchSummon value : SchSummon.values()) {
			value.getIcon();
		}
		summonUpdate();
		summonPendingActionsUpdate();
		uiUpdate();
	}

	private @Nullable XivCombatant getSummonCombatant() {
		// TODO: do we need to do something about chocobos/minions/etc?
		if (isDissipationActive()) {
			return null;
		}
		XivPlayerCharacter player = state.getPlayer();
		if (player == null) {
			return null;
		}
		return state.getCombatantsListCopy().stream()
				.filter(cbt -> player.equals(cbt.getParent()))
				// Highest ID seems to always be most recently summoned
				.max(Comparator.comparing(XivEntity::getId))
				.orElse(null);
	}


	private void recalcCds() {
		for (int i = 0; i < defaultCds.size(); i++) {
			Cooldown cooldown = defaultCds.get(i);
			CooldownStatus personalCd = cdh.getPersonalCd(cooldown);
			if (personalCd == null) {
				croppedCds.set(i, new VisualCdInfoMain(cooldown));
			}
			else {
				croppedCds.set(i, new VisualCdInfoMain(personalCd));
			}
		}
	}

	@Override
	protected void periodicRefresh() {
		refreshCd();
	}

	private void refreshCd() {
		recalcCds();
		SwingUtilities.invokeLater(tableModel::fullRefresh);
	}
}
