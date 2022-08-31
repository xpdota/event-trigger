package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.util.RepeatSuppressor;
import gg.xp.xivsupport.models.ArenaPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@CalloutRepo(name = "P7S", duty = KnownDuty.P7S)
public class P7S extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P7S.class);
	private final ModifiableCallout<AbilityCastStart> boughOfAttisClose = ModifiableCallout.durationBasedCall("Bough of Attis Attack Close", "Go Far");
	private final ModifiableCallout<AbilityCastStart> boughOfAttisFar = ModifiableCallout.durationBasedCall("Bough of Attis Attack Far", "Get Close");
	private final ModifiableCallout<AbilityCastStart> boughOfAttisLeft = ModifiableCallout.durationBasedCall("Bough of Attis Attack Left", "Go Right");
	private final ModifiableCallout<AbilityCastStart> boughOfAttisRight = ModifiableCallout.durationBasedCall("Bough of Attis Attack Right", "Go Left");
	private final ModifiableCallout<AbilityCastStart> dispersedAeroII = ModifiableCallout.durationBasedCall("Dispersed Aero II", "Tank Spread");
	private final ModifiableCallout<AbilityCastStart> condensedAeroII = ModifiableCallout.durationBasedCall("Condensed Aero II", "Tank Stack");

//	private final ModifiableCallout<AbilityCastStart> hemitheosHoly = ModifiableCallout.durationBasedCall("Hemitheos's Holy", "Spread");
//	private final ModifiableCallout<AbilityCastStart> hemitheosGlareIII = ModifiableCallout.durationBasedCall("Hemitheos's Glare III", "Center");
//	private final ModifiableCallout<AbilityCastStart> immortalsObol = ModifiableCallout.durationBasedCall("Immortal's Obol", "Edge, in Circles");
//	private final ModifiableCallout<AbilityCastStart> hemitheosAeroII = ModifiableCallout.durationBasedCall("Hemitheos's Aero II", "Tankbuster");
	private final ModifiableCallout<AbilityCastStart> sparkOfLife = ModifiableCallout.durationBasedCall("Spark of Life", "Raidwide"); //bleed
//	private final ModifiableCallout<AbilityCastStart> staticMoon = ModifiableCallout.durationBasedCall("Static Moon", "Out");
//	private final ModifiableCallout<AbilityCastStart> stymphalianStrike = ModifiableCallout.durationBasedCall("Stymphalian Strike", "Dive");
	private final ModifiableCallout<AbilityCastStart> bladesOfAttis = ModifiableCallout.durationBasedCall("Blades of Attis", "Exaflare");
//	private final ModifiableCallout<AbilityCastStart> hemitheosAeroIV = ModifiableCallout.durationBasedCall("Hemitheos's Aero IV", "Knockback");

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	public P7S(XivState state) {
		this.state = state;
	}

	private final XivState state;
	private XivState getState() {
		return this.state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P7S);
	}

	private final RepeatSuppressor manyActorsSupp = new RepeatSuppressor(Duration.ofMillis(100));

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		int id = (int) event.getAbility().getId();
		ModifiableCallout<AbilityCastStart> call;
		switch (id) {
			case 0x7826 -> call = boughOfAttisFar;
			case 0x7821 -> call = boughOfAttisClose;
			case 0x7824 -> call = boughOfAttisRight;
			case 0x7823 -> call = boughOfAttisLeft;
			case 0x7835 -> call = dispersedAeroII;
			case 0x7836 -> call = condensedAeroII;
			case 0x7839 -> call = sparkOfLife;
			case 0x782E -> call = bladesOfAttis;
			default -> {
				return;
			}
		}
//		if (id == 0x0)
//			call = boughOfAttisClose;
//		else if (id == 0x0) //????+1 fake
//			call = boughOfAttisFar;
//		else if (id == 0x0 && event.getSource().getPos().x() < 100) //????-1 boss
//			call = boughOfAttisLeft;
//		else if (id == 0x0 && event.getSource().getPos().x() > 100) //????-1 boss
//			call = boughOfAttisRight;
//		else if (id == 0x70) //fake x 8 = ????+1 ~1.3 sec after ???? finishes
//			call = hemitheosHoly;
//		else if (id == 0x0) //fake ????+1 ~0.7 after ???? finishes
//			call = hemitheosGlareIII;
//		else if (id == 0x0) //???+1 fake, longer cast. deals damage
//			call = immortalsObol;
//		else if (id == 0x0) //????+1 fake cast x 2 (1 each target)
//			call = hemitheosAeroII;
//		else if (id == 0x0)
//			call = sparkOfLife;
//		else if (id == 0x0 && manyActorsSupp.check(event)) //io out, ????-2 and ????-1 casted to summon eggs
//			call = staticMoon;
//		else if (id == 0x0 && manyActorsSupp.check(event)) //stymphalide dive, ????-2 and ????-1 casted to summon eggs
//			call = stymphalianStrike;
//		else if (id == 0x0 && manyActorsSupp.check(event)) //????-1 real, but instant
//			call = bladesOfAttis;
//		else if (id == 0x0) //????+1 fake, has location
//			call = hemitheosAeroIV;
//		else
//			return;
//
		context.accept(call.getModified(event));
	}
}
