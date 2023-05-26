package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CalloutRepo(name = "P9N", duty = KnownDuty.P9N)
public class P9N extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(P9N.class);

	@NpcCastCallout(0x8117)
	private final ModifiableCallout<AbilityCastStart> gluttonysAugar = ModifiableCallout.durationBasedCall("Gluttony's Augar", "Raidwide");

	//Magus
	@NpcCastCallout(0x0) //TODO: start of fight cast
	private final ModifiableCallout<AbilityCastStart> fireIII = ModifiableCallout.durationBasedCall("Fire III", "AOEs");
	@NpcCastCallout(0x0) //TODO: start of fight cast
	private final ModifiableCallout<AbilityCastStart> blizzardIII = ModifiableCallout.durationBasedCall("Blizzard III", "Donut");
	@NpcCastCallout(0x8141)
	private final ModifiableCallout<AbilityCastStart> globalSpell = ModifiableCallout.durationBasedCall("Global Spell", "Raidwide with bleed");
	@NpcCastCallout(0x811D)
	private final ModifiableCallout<AbilityCastStart> dualspell = ModifiableCallout.durationBasedCall("Dualspell", "Donut and AOEs");

	//0x86E1 Iceflame Summoning TODO: calling this takes effort
	private final ModifiableCallout<AbilityUsedEvent> fireScreenAOE = new ModifiableCallout<>("Fire Augment: AOEs", "Large AOEs");
	private final ModifiableCallout<AbilityUsedEvent> fireScreenRocks = new ModifiableCallout<>("Fire Augment: Rocks", "Near ice");
	private final ModifiableCallout<AbilityUsedEvent> iceScreenDonut = new ModifiableCallout<>("Ice Augment: Donut", "Small Donut");
	private final ModifiableCallout<AbilityUsedEvent> iceScreenRocks = new ModifiableCallout<>("Ice Augment: Rocks", "Near fire");
	//TODO: Temporary
	private final ModifiableCallout<AbilityUsedEvent> fireScreen = new ModifiableCallout<>("Fire Augment", "Enhanced fire");
	private final ModifiableCallout<AbilityUsedEvent> iceScreen = new ModifiableCallout<>("Ice Augment", "Enhanced ice");

	//Martialist
	@NpcCastCallout(0x8128)
	private final ModifiableCallout<AbilityCastStart> archaicRockbreaker = ModifiableCallout.durationBasedCall("Archaic Rockbreaker", "Knockback, then avoid fissures");
	@NpcCastCallout(value = 0x812F, suppressMs = 200)
	private final ModifiableCallout<AbilityCastStart> archaicDemolish = ModifiableCallout.durationBasedCall("Archaic Demolish", "Healer stacks");
	@NpcCastCallout(0x8131)
	private final ModifiableCallout<AbilityCastStart> ascendantFist = ModifiableCallout.durationBasedCall("Ascendant Fist", "Tankbuster");
	@NpcCastCallout(0x814A)
	private final ModifiableCallout<AbilityCastStart> rearCombo = ModifiableCallout.durationBasedCall("Rear Combination", "Donut, go front");
	@NpcCastCallout(0x8147)
	private final ModifiableCallout<AbilityCastStart> frontCombo = ModifiableCallout.durationBasedCall("Front Combination", "Out, go behind");

	//Behemoth
	@NpcCastCallout(value = 0x8132, suppressMs = 200)
	private final ModifiableCallout<AbilityCastStart> charybdis = ModifiableCallout.durationBasedCall("Charybdis", "AOEs with puddles");
	@NpcCastCallout(0x8138) //TODO: gets called along with bile at the same time when its first seen. add bile but make sure it doesnt overlap
	private final ModifiableCallout<AbilityCastStart> beastlyRoar = ModifiableCallout.durationBasedCall("Beastly Roar", "Knockback");
//	@NpcCastCallout(0x813E)
//	private final ModifiableCallout<AbilityCastStart> beastlyBile = ModifiableCallout.durationBasedCall("Beastly Bile", "AOEs");
	@NpcCastCallout(0x813F)
	private final ModifiableCallout<AbilityCastStart> pulverizingPounce = ModifiableCallout.durationBasedCall("Pulverizing Pounce", "Stack");
	@NpcCastCallout(value = 0x8139, suppressMs = 10_000)
	private final ModifiableCallout<AbilityCastStart> gluttonousRampage = ModifiableCallout.durationBasedCall("Gluttonous Rampage", "Tank Tethers then proximity");
	@NpcCastCallout(0x8134)
	private final ModifiableCallout<AbilityCastStart> comet = ModifiableCallout.durationBasedCall("Comet", "Proximity");
	@NpcCastCallout(0x813B)
	private final ModifiableCallout<AbilityCastStart> eclipticMeteor = ModifiableCallout.durationBasedCall("Ecliptic Meteor", "Hide behind normal meteor");
	@NpcCastCallout(0x8143)
	private final ModifiableCallout<AbilityCastStart> burst = ModifiableCallout.durationBasedCall("Burst", "Away from meteor");

	private final XivState state;
	private final StatusEffectRepository buffs;

	public P9N(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P9N);
	}

	private XivState getState() {
		return state;
	}

	private StatusEffectRepository getBuffs() {
		return buffs;
	}

	@HandleEvents
	public void abilityUsed(EventContext context, AbilityUsedEvent event) {
		int id = (int)event.getAbility().getId();
		ModifiableCallout<AbilityUsedEvent> call;
		switch(id) {
			//fire screen TODO: logic for which mech it . dualspell, start, or rocks
			case 0x8122 -> {
				call = fireScreen;
			}
			//ice screen
			case 0x8123 -> {
				call = iceScreen;
			}
			default -> {
				return;
			}
		}

		context.accept(call.getModified(event));
	}
}
