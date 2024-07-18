package gg.xp.xivsupport.gui.tables.filters;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasPlayerHeadMarker;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.actlines.events.JobGaugeUpdate;
import gg.xp.xivsupport.events.actlines.events.StatusEffectList;
import gg.xp.xivsupport.events.actlines.events.TickEvent;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.speech.CalloutEvent;

import java.util.Locale;

@ScanMe
public class QuickFilterBuiltins {

	public QuickFilterBuiltins(QuickFilters qf) {
		qf.register("Boss Casts", Event.class,
				event -> (event instanceof AbilityCastStart acs && isNpc(acs.getSource()))
	                                                || (event instanceof AbilityUsedEvent aue && isNpc(aue.getSource()) && !aue.getAbility().getName().toLowerCase(Locale.ROOT).equals("attack"))
	                                                || (event instanceof CalloutEvent));
		qf.register("No Player Actions", Event.class,
				event -> {
					if (event instanceof HasPlayerHeadMarker) {
						return true;
					}
					if ((event instanceof HasAbility || event instanceof HasStatusEffect) && event instanceof HasSourceEntity hse) {
						if (!isNpc(hse.getSource())) {
							return false;
						}
					}
					if (event instanceof StatusEffectList sel && !isNpc(sel.getTarget())) {
						return false;
					}
					if (event instanceof TickEvent) {
						return false;
					}
					if ((event instanceof HasTargetEntity hte && hte.getTarget().getType() == CombatantType.PET) || (event instanceof HasSourceEntity hse && hse.getSource().getType() == CombatantType.PET)) {
						return false;
					}
					if (event instanceof StatusEffectList sel && (sel.getTarget().getName() == null || sel.getTarget().getName().isEmpty())) {
						return false;
					}
					if (event instanceof JobGaugeUpdate) {
						return false;
					}
					return true;
				});
	}

	private boolean isNpc(XivCombatant combatant) {
		return combatant.getType() == CombatantType.NPC || combatant.getType() == CombatantType.FAKE || combatant.isEnvironment();
	}

}
