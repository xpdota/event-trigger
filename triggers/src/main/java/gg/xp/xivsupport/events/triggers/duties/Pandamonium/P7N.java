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

@CalloutRepo(name = "P7N", duty = KnownDuty.P7N)
public class P7N extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P7N.class);
	private final ModifiableCallout<AbilityCastStart> boughOfAttisClose = ModifiableCallout.durationBasedCall("Bough of Attis Attack Close", "far");
	private final ModifiableCallout<AbilityCastStart> boughOfAttisFar = ModifiableCallout.durationBasedCall("Bough of Attis Attack Far", "close");
	private final ModifiableCallout<AbilityCastStart> boughOfAttisLeft = ModifiableCallout.durationBasedCall("Bough of Attis Left", "right");
	private final ModifiableCallout<AbilityCastStart> boughOfAttisRight = ModifiableCallout.durationBasedCall("Bough of Attis Right", "left");
	private final ModifiableCallout<AbilityCastStart> hemitheosHoly = ModifiableCallout.durationBasedCall("Hemitheos's Holy", "spread");
	private final ModifiableCallout<AbilityCastStart> hemitheosGlareIII = ModifiableCallout.durationBasedCall("Hemitheos's Glare III", "center");
	private final ModifiableCallout<AbilityCastStart> immortalsObol = ModifiableCallout.durationBasedCall("Immortal's Obol", "edge, in circles");
	private final ModifiableCallout<AbilityCastStart> hemitheosAeroII = ModifiableCallout.durationBasedCall("Hemitheos's Aero II", "tankbuster");
	private final ModifiableCallout<AbilityCastStart> sparkOfLife = ModifiableCallout.durationBasedCall("Spark of Life", "raidwide"); //bleed
	private final ModifiableCallout<AbilityCastStart> staticMoon = ModifiableCallout.durationBasedCall("Static Moon", "out");
	private final ModifiableCallout<AbilityCastStart> stymphalianStrike = ModifiableCallout.durationBasedCall("Stymphalian Strike", "dive");
	private final ModifiableCallout<AbilityCastStart> bladesOfAttis = ModifiableCallout.durationBasedCall("Blades of Attis", "exaflare");
	private final ModifiableCallout<AbilityCastStart> hemitheosAeroIV = ModifiableCallout.durationBasedCall("Hemitheos's Aero IV", "knockback");

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	public P7N(XivState state) {
		this.state = state;
	}

	private final XivState state;
	private XivState getState() {
		return this.state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.zoneIs(0x43D);
	}

	private final RepeatSuppressor manyActorsSupp = new RepeatSuppressor(Duration.ofMillis(100));

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		long id = event.getAbility().getId();
		ModifiableCallout<AbilityCastStart> call;
		if (id == 0x77F9) //77FA fake, casted twice
			call = boughOfAttisClose;
		else if (id == 0x77FE) //77FF fake
			call = boughOfAttisFar;
		else if (id == 0x77FD && event.getSource().getPos().x() < 100) //77FC boss
			call = boughOfAttisLeft;
		else if (id == 0x77FD && event.getSource().getPos().x() > 100) //77FC boss
			call = boughOfAttisRight;
		else if (id == 0x7807) //fake x 8 = 7808 ~1.3 sec after 7807 finishes
			call = hemitheosHoly;
		else if (id == 0x77F7) //fake 77F8 ~0.7 after 77F7 finishes
			call = hemitheosGlareIII;
		else if (id == 0x77F5) //77F6 fake, longer cast. deals damage
			call = immortalsObol;
		else if (id == 0x7809) //780A fake cast x 2 (1 each target)
			call = hemitheosAeroII;
		else if (id == 0x780B)
			call = sparkOfLife;
		else if (id == 0x7802 && manyActorsSupp.check(event)) //io out, 7800 and 7801 casted to summon eggs
			call = staticMoon;
		else if (id == 0x7803 && manyActorsSupp.check(event)) //stymphalide dive, 7800 and 7801 casted to summon eggs
			call = stymphalianStrike;
		else if (id == 0x7805 && manyActorsSupp.check(event)) //7804 real, but instant
			call = bladesOfAttis;
		else if (id == 0x7840) //7841 fake, has location
			call = hemitheosAeroIV;
		else
			return;

		context.accept(call.getModified(event));
	}
}
