package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CalloutRepo(name = "P10N", duty = KnownDuty.P10N)
public class P10N extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(P10N.class);

	@NpcCastCallout(0x8259)
	private final ModifiableCallout<AbilityCastStart> silkspit = ModifiableCallout.durationBasedCall("Silkspit", "Spread AOEs");
	@NpcCastCallout(0x825D)
	private final ModifiableCallout<AbilityCastStart> pandaemoniacPillars = ModifiableCallout.durationBasedCall("Pandæmoniac Pillars", "Soak towers");
	@NpcCastCallout(value = 0x8262, suppressMs = 100)
	private final ModifiableCallout<AbilityCastStart> imprisonment = ModifiableCallout.durationBasedCall("Imprisonment", "Away from towers");
	@NpcCastCallout(0x827B)
	private final ModifiableCallout<AbilityCastStart> ultima = ModifiableCallout.durationBasedCall("Ultima", "Raidwide with bleed");
	@NpcCastCallout(value = 0x8264, suppressMs = 100)
	private final ModifiableCallout<AbilityCastStart> cannonspawn = ModifiableCallout.durationBasedCall("Cannonspawn", "Under towers");
	@NpcCastCallout(0x8276)
	private final ModifiableCallout<AbilityCastStart> pandaemonicMeltdown = ModifiableCallout.durationBasedCall("Pandæmonic Meltdown", "Stack");
	@NpcCastCallout(0x8278)
	private final ModifiableCallout<AbilityCastStart> soulGrasp = ModifiableCallout.durationBasedCall("Soul Grasp", "Tank stack");
	@NpcCastCallout(0x8721) //TODO: identify which bridges were activated for the touchdown call later 825B and 825C are the AOEs, dont know which is the big or small ones, only the big matters
	private final ModifiableCallout<AbilityCastStart> entanglingWeb = ModifiableCallout.durationBasedCall("Entangling Web", "Bait AOEs");
	@NpcCastCallout(0x8265) //east safe, 0x8266 is RP cast
	private final ModifiableCallout<AbilityCastStart> pandaemoniacRayWestHit = ModifiableCallout.durationBasedCall("Pandæmoniac Ray: West hit", "East safe");
	@NpcCastCallout(0x8267) //west safe
	private final ModifiableCallout<AbilityCastStart> pandaemoniacRayEastHit = ModifiableCallout.durationBasedCall("Pandæmoniac Ray: East hit", "West safe");
	@NpcCastCallout(0x8270)
	private final ModifiableCallout<AbilityCastStart> partedPlumes = ModifiableCallout.durationBasedCall("Parted Plumes", "Out of middle");
	@NpcCastCallout(0x8268) //8268 from boss, 8269 from fake (growing aoe on floor) TODO: identify which sides safe from entangling web
	private final ModifiableCallout<AbilityCastStart> touchdown = ModifiableCallout.durationBasedCall("Touchdown", "Move to side arenas");
	@NpcCastCallout(0x826A)
	private final ModifiableCallout<AbilityCastStart> harrowingHell = ModifiableCallout.durationBasedCall("Harrowing Hell", "Heavy raidwides");
//	@NpcCastCallout(0x826F) //TODO: is this even worth calling, it cant be immuned
//	private final ModifiableCallout<AbilityCastStart> harrowingHellKB = ModifiableCallout.durationBasedCall("Harrowing Hell: Knockback", "Knockback");
	@NpcCastCallout(0x8272) //fake actors 8273, 8274
	private final ModifiableCallout<AbilityCastStart> wickedStep = ModifiableCallout.durationBasedCall("Wicked Step", "Tank towers");

	private final XivState state;
	private final StatusEffectRepository buffs;

	public P10N(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P10N);
	}

	private XivState getState() {
		return state;
	}

	private StatusEffectRepository getBuffs() {
		return buffs;
	}
}
