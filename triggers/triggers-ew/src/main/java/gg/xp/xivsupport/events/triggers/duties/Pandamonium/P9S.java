package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcAbilityUsedCallout;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@CalloutRepo(name = "P9S", duty = KnownDuty.P9S)
public class P9S extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(P9S.class);

	@NpcCastCallout(0x814C)
	private final ModifiableCallout<AbilityCastStart> gluttonysAugur = ModifiableCallout.durationBasedCall("Gluttony's Augur", "Raidwide");

	//Mage
	@NpcCastCallout(0x8118)
	private final ModifiableCallout<AbilityCastStart> raveningMage = ModifiableCallout.durationBasedCall("Ravening: Mage", "Raidwide soon");
	@NpcCastCallout(0x8151) //Wind + Fire
	private final ModifiableCallout<AbilityCastStart> dualityOfDeathWindFire = ModifiableCallout.durationBasedCall("Duality of Death", "Tank Swap");
	@NpcCastCallout(0x8154) //Ice left hand, Fire right hand
	private final ModifiableCallout<AbilityCastStart> dualspellIceFire = ModifiableCallout.durationBasedCall("Dualspell: Ice + Fire", "Buddy stacks soon");
	@NpcCastCallout(0x8155) //Lightning left hand, Ice right hand
	private final ModifiableCallout<AbilityCastStart> dualspellLightIce = ModifiableCallout.durationBasedCall("Dualspell: Lightning + Ice", "Proteans soon");
	@NpcAbilityUsedCallout(0x8123) //Ice screen effect TODO: maybe later this needs to be called differently
	private final ModifiableCallout<AbilityUsedEvent> dualspellScreenIce = new ModifiableCallout<>("Dualspell: Ice", "In");
	@NpcAbilityUsedCallout(0x8122) //Fire screen effect
	private final ModifiableCallout<AbilityUsedEvent> dualspellScreenFire = new ModifiableCallout<>("Dualspell: Fire", "Out");
	@NpcAbilityUsedCallout(0x815C) //Lightning screen effect on second dualspell
	private final ModifiableCallout<AbilityUsedEvent> dualspellScreenLightning = new ModifiableCallout<>("Dualspell: Lightning", "Out");

	//Martialist
	@NpcCastCallout(0x8119)
	private final ModifiableCallout<AbilityCastStart> raveningMartialist = ModifiableCallout.durationBasedCall("Ravening: Martialist", "Raidwide soon");
	//0x815E cardinal walls? NPCs have headings
	@NpcCastCallout(0x816F)
	private final ModifiableCallout<AbilityCastStart> ascendantFist = ModifiableCallout.durationBasedCall("Ascendant Fist", "Tank Bleed and Swap");
	@NpcCastCallout(0x815F)
	private final ModifiableCallout<AbilityCastStart> archaicRockbreaker = ModifiableCallout.durationBasedCall("Archaic Rockbreaker", "Knockback and buddy stacks");
	@NpcCastCallout(0x816D)
	private final ModifiableCallout<AbilityCastStart> archaicDemolish = ModifiableCallout.durationBasedCall("Archaic Demolish", "Light parties");

	private final XivState state;
	private final StatusEffectRepository buffs;
	private final ActiveCastRepository acr;

	public P9S(XivState state, StatusEffectRepository buffs, ActiveCastRepository acr) {
		this.state = state;
		this.buffs = buffs;
		this.acr = acr;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P9S);
	}

	private XivState getState() {
		return state;
	}

	private StatusEffectRepository getBuffs() {
		return buffs;
	}

	private ActiveCastRepository getAcr() {
		return acr;
	}

	private final ModifiableCallout<AbilityCastStart> martCombInFirst = ModifiableCallout.durationBasedCall("Martialist Combination: In First", "In");
	private final ModifiableCallout<AbilityCastStart> martCombOutFirst = ModifiableCallout.durationBasedCall("Martialist Combination: Out First", "Out");
	private final ModifiableCallout<AbilityUsedEvent> martCombBehind = new ModifiableCallout<>("Martialist Combination: Behind", "Go behind");
	private final ModifiableCallout<AbilityUsedEvent> martCombFront = new ModifiableCallout<>("Martialist Combination: Front", "Go front");
	private final ModifiableCallout<?> martCombInSecond = new ModifiableCallout<>("Martialist Combination: In Second", "In");
	private final ModifiableCallout<?> martCombOutSecond = new ModifiableCallout<>("Martialist Combination: Out Second", "Out");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> martialistCombination = SqtTemplates.sq(20_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(33127, 33128),
			(e1, s) -> {
				//33128/0x8168 rear safe, 33127/0x8167 front safe
				//33125 out safe, 33126 in safe
				//TODO: Collect 8 0x8161 casts and determine safe spot from the arriving fissure AOEs
				Optional<AbilityCastStart> inOutOp = getAcr().getActiveCastById(33125, 33126).map(ct -> ct.getCast());
				if (inOutOp.isPresent()) {
					AbilityCastStart inOut = inOutOp.get();
					if(inOut.getAbility().getId() == 33125) {
						s.accept(martCombOutFirst.getModified(e1));
					} else {
						s.accept(martCombInFirst.getModified(e1));
					}

					//Donut or out finishes
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x8239));

					if(e1.getAbility().getId() == 0x8167) {
						s.accept(martCombFront.getModified());
					} else {
						s.accept(martCombBehind.getModified());
					}

					//816B starts his wind up for front kick, back kick unknown
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x816B));

					//TODO: Collect last 4 0x8161 casts to call a safe spot (for melee uptime?)
					if(inOut.getAbility().getId() == 33125) {
						s.accept(martCombInSecond.getModified());
					} else {
						s.accept(martCombOutSecond.getModified());
					}
				} else {
					log.info("Martialist Combination: Couldn't find in/out kick");
				}
			});
}
