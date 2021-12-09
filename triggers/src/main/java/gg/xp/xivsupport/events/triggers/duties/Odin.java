package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;

/**
 * Example trigger pack for a duty
 */
// @CalloutRepo indicates that the system should scan for fields defined as ModifiableCallout. The user is presented
// with a UI to enable/disable them, and change the callout text under the Plugins > Callouts tab.
// The name chosen here will show in the UI.
@CalloutRepo("Urth's Fount (Odin)")
// You should not chang the class name once you publish this, as it is used to determine the settings key to store
// customizations to the callouts.
// FilteredEventHandler is an optional interface, giving you the 'enabled' option (see below).
public class Odin implements FilteredEventHandler {

	// Since we have @CalloutRepo
	private final ModifiableCallout valknut = new ModifiableCallout("Valknut (Out)", "Out");

	// This comes from FilteredEventHandler. In this case, we want to restrict this set of triggers to a specific
	// zone (Urth's Fount, in this case, Zone ID 394).
	@Override
	public boolean enabled(EventContext context) {
		return context.getStateInfo().get(XivState.class).zoneIs(394);
	}

	// This is an actual callout. You can specify as many as you want, but you have to follow the usual Java conventions
	// (e.g. they need to have unique names or it won't compile).
	// The @HandleEvents annotation is what tells the scanner that this is a method that should be called when we have
	// an event of the given type to handle.
	@HandleEvents(name = "Valknut (Out)")
	// The name should not be changed once published, as the is used for the settings keys (just like the class name).
	// The first argument is always EventContext, which gives you the ability to both query zone/player/etc info (as
	// can be seen in the `enabled` method above, as well as submit new events (in this case, a callout).
	// The second argument is the type of event to listen for. In this case, we want to know when something starts
	// casting an ability.
	public void valknut(EventContext context, AbilityCastStart event) {
		// Valknut has ID 0xC49. As per usual Java conventions, numbers can be specified as base-10 or base-16. Note
		// that numbers are always signed in Java - so if something is in the 0x80000000 - 0xFFFFFFFF range, you need
		// to make sure you specify it as a long by putting L at the end of it (e.g. 0xE000000L).
		if (event.getAbility().getId() == 0xC49) {
			// ModifiableCallout.getModified() returns a CalloutEvent with whatever user-specified modifications
			// applied (e.g. the text can be altered, you can pick TTS/Text/Both, or disable it entirely).
			// EventContext.accept(Event) - submit the new event to be processed immediately.
			context.accept(valknut.getModified());
		}
	}
}
