package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;

@CalloutRepo(name = "M3N", duty = KnownDuty.M3N)
public class M3N extends AutoChildEventHandler implements FilteredEventHandler {

	@NpcCastCallout(0x9AD4)
	private final ModifiableCallout<AbilityCastStart> brutalLariatWest = new ModifiableCallout<>("Brutal Lariat: East safe", "East");
	@NpcCastCallout(0x9AD5)
	private final ModifiableCallout<AbilityCastStart> brutalLariatEast = new ModifiableCallout<>("Brutal Lariat: West safe", "West");
	@NpcCastCallout(0x9ADC)
	private final ModifiableCallout<AbilityCastStart> lariatComboWestEast = new ModifiableCallout<>("Lariat Combo: East then west", "East then West");
	@NpcCastCallout(0x9ADE)
	private final ModifiableCallout<AbilityCastStart> lariatComboEastWest = new ModifiableCallout<>("Lariat Combo: West then east", "West then East");
	@NpcCastCallout(0x9ADD)
	private final ModifiableCallout<AbilityCastStart> lariatComboWestWest = new ModifiableCallout<>("Lariat Combo: East safe", "East, stay east");
	@NpcCastCallout(0x9ADF)
	private final ModifiableCallout<AbilityCastStart> lariatComboEastEast = new ModifiableCallout<>("Lariat Combo: West safe", "West, stay west");

	public M3N(XivState state) {
		this.state = state;
	}
	private final XivState state;

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M3N);
	}
}
