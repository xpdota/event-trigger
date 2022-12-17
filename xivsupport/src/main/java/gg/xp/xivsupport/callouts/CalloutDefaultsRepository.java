package gg.xp.xivsupport.callouts;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;

@ScanMe
public class CalloutDefaultsRepository {

	private final CalloutDefaults calloutDefaults;

	public CalloutDefaultsRepository(PersistenceProvider pers) {
		calloutDefaults = CalloutDefaults.noParent(pers, "callouts.global-settings.callout-defaults");
	}

	public CalloutDefaults getGlobalDefaults() {
		return calloutDefaults;
	}
}
