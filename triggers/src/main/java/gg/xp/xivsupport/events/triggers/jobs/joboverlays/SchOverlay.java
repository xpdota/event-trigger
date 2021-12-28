package gg.xp.xivsupport.events.triggers.jobs.joboverlays;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.jobs.ActionIcon;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.XivBuffsUpdatedEvent;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.jobs.StatusEffectRepository;
import gg.xp.xivsupport.gui.tables.renderers.ComponentListRenderer;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.Nullable;

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

	private final JLabel summonLabel = new JLabel();
	private final ComponentListRenderer listRenderer = new ComponentListRenderer(4);
	private static final int ICON_SIZE = 40;
	private final XivState state;
	private final StatusEffectRepository buffs;
	private @Nullable XivCombatant summonCombatant;
	private SchSummon summon = SchSummon.NONE;
	private @Nullable Component summonIcon;
	private List<Component> pendingActionIcons = Collections.emptyList();

	private enum SchSummon {
		NONE,
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

	public SchOverlay(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setPreferredSize(new Dimension(250, 100));
		add(summonLabel);
		summonLabel.setBounds(0, 0, 400, 20);
		add(listRenderer);
		listRenderer.setBounds(0, 25, 400, 50);
		setOpaque(false);
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
		// TODO: refresher delayer
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
		repaint();
	}

	private @Nullable ActionIcon summonOrderToActionMapping(BuffApplied buff) {
		// TODO: add seraph-specific versions
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
		summonIcon = summon.icon;
	}

	private boolean isDissipationActive() {
		// Dissipation special case
		// TODO: find a way to not have this be called multiple times
		return buffs.statusesOnTarget(state.getPlayer()).stream().anyMatch(buff -> buff.getBuff().getId() == 0x317);
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
}
