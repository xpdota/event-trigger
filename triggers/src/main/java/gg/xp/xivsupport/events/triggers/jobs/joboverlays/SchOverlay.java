package gg.xp.xivsupport.events.triggers.jobs.joboverlays;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.jobs.ActionIcon;
import gg.xp.xivdata.jobs.Cooldown;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.XivBuffsUpdatedEvent;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.jobs.CdTracker;
import gg.xp.xivsupport.events.triggers.jobs.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.jobs.gui.BaseCdTrackerOverlay;
import gg.xp.xivsupport.events.triggers.jobs.gui.BaseCdTrackerTable;
import gg.xp.xivsupport.events.triggers.jobs.gui.VisualCdInfo;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.renderers.ComponentListRenderer;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
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

import static gg.xp.xivdata.jobs.Job.SCH;

public class SchOverlay extends JPanel implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(SchOverlay.class);

	private final ComponentListRenderer listRenderer = new ComponentListRenderer(4);
	private static final int ICON_SIZE = 40;
	private final XivState state;
	private final StatusEffectRepository buffs;
	private final CdTracker cooldowns;
	private @Nullable XivCombatant summonCombatant;
	private SchSummon summon = SchSummon.NONE;
	private @Nullable Component summonIcon;
	private List<Component> pendingActionIcons = Collections.emptyList();

	// Stuff for tracking AF CD
	private final CustomTableModel<VisualCdInfo> tableModel;
	private volatile List<VisualCdInfo> croppedCds = Collections.emptyList();

	private enum SchSummon {
		NONE(810),
		SELENE(0x4340),
		EOS(0x433f),
		SERAPH(0x40A1),
		DISSIPATION(0xe03);


		final @Nullable Component icon;

		SchSummon() {
			this.icon = null;
		}

		SchSummon(long actionIdForIcon) {
			IconTextRenderer.ScaledImageComponent icon = IconTextRenderer.getIconOnly(ActionIcon.forId(actionIdForIcon));
			if (icon == null) {
				this.icon = null;
			}
			else {
				this.icon = icon.withNewSize(ICON_SIZE);
			}
		}
	}

	public SchOverlay(XivState state, StatusEffectRepository buffs, CdTracker cooldowns) {
		this.state = state;
		this.buffs = buffs;
		this.cooldowns = cooldowns;
		setLayout(null);
		setOpaque(false);
		setPreferredSize(new Dimension(250, 100));
		add(listRenderer);
		listRenderer.setBounds(0, 5, 250, 50);
		BaseCdTrackerTable tableHolder = new BaseCdTrackerTable(() -> croppedCds);
		tableModel = tableHolder.getTableModel();
		JTable table = tableHolder.getTable();
		table.setBounds(0, 60, 250, 40);
		add(table);
		// TODO: can't use getScale() from this context
		RefreshLoop<SchOverlay> refresher = new RefreshLoop<>("SchOverlay", this, SchOverlay::refreshCd, dt -> Math.max((50L), 20));
		refresher.start();
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
		return ActionIcon.forId(actionId);
	}

	private void summonUpdate() {
		summonCombatant = getSummonCombatant();
		log.info("Summon update: {}", summonCombatant);
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
		log.info("Summon: {}", summon);
		summonIcon = summon.icon;
	}

	private boolean isDissipationActive() {
		// Dissipation special case
		// TODO: find a way to not have this be called multiple times
		return buffs.statusesOnTarget(state.getPlayer()).stream().anyMatch(buff -> buff.getBuff().getId() == 0x317);
	}

	@Override
	public void setVisible(boolean vis) {
		if (vis) {
			summonUpdate();
			summonPendingActionsUpdate();
			uiUpdate();
		}
		super.setVisible(vis);
	}

	private @Nullable XivCombatant getSummonCombatant() {
		// TODO: do we need to do something about chocobos/minions/etc?
		if (isDissipationActive()) {
			return null;
		}
		XivPlayerCharacter player = state.getPlayer();
		return state.getCombatantsListCopy().stream()
				.filter(cbt -> player.equals(cbt.getParent()))
				// Highest ID seems to always be most recently summoned
				.max(Comparator.comparing(XivEntity::getId))
				.orElse(null);
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.getPlayerJob() == SCH;
	}

	private @Nullable AbilityUsedEvent getAfCd() {
		XivPlayerCharacter player = state.getPlayer();
		if (player == null) {
			return null;
		}
		return cooldowns.getCds((e) -> e.getValue().getSource().equals(player) && e.getValue().getAbility().getId() == 0xa6)
				.values().stream().findFirst().orElse(null);
	}

	private void recalcCds() {
		@Nullable AbilityUsedEvent event = getAfCd();
		if (event == null) {
			croppedCds = Collections.emptyList();
		}
		else {
			VisualCdInfo vci = new VisualCdInfo(Cooldown.Aetherflow, event, null);
			croppedCds = Collections.singletonList(vci);
		}
	}


	private void refreshCd() {
		recalcCds();
		SwingUtilities.invokeLater(tableModel::fullRefresh);
	}
}
