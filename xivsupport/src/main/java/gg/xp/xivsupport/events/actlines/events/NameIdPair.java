package gg.xp.xivsupport.events.actlines.events;

import java.util.Locale;

public interface NameIdPair {

	long getId();
	String getName();


	// TODO: this is inefficient because we should just assemble a checker once for a given input string
	// TODO: this also doesn't belong here in the first place...
	default boolean matchesFilter(String filter) {
		if (filter.startsWith("0x")) {
			// TODO: this is also inefficient because we should just be parsing the input text
			String wantedId = filter.substring(2).trim();
			String actualId = Long.toString(getId(), 16);
			return wantedId.equalsIgnoreCase(actualId);
		}
		// Treat as partial match
		return getName().toUpperCase(Locale.ROOT).contains(filter.toUpperCase());

	}
}
