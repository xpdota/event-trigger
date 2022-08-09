package gg.xp.xivsupport.callouts.conversions;

import gg.xp.xivsupport.gui.util.HasFriendlyName;
import gg.xp.xivsupport.models.XivPlayerCharacter;

import java.util.function.Function;

public enum PlayerNameConversion implements HasFriendlyName {
	FULL_NAME("Full Name", XivPlayerCharacter::getName),
	FIRST_NAME("First Name", xpc -> xpc.getName().split(" ")[0]),
	LAST_NAME("Last Name", xpc -> xpc.getName().split(" ")[1]),
	INITIALS("Initials", xpc -> {
		String first = xpc.getName().split(" ")[0];
		String last = xpc.getName().split(" ")[1];
		return first.charAt(0) + "." + last.charAt(0) + '.';
	}),
	JOB_NAME("Job Name", xpc -> xpc.getJob().getFriendlyName()),
	JOB_ABBREV("Job Abbreviation", xpc -> xpc.getJob().name());

	private final String label;
	private final Function<XivPlayerCharacter, String> conversion;

	PlayerNameConversion(String label, Function<XivPlayerCharacter, String> conversion) {
		this.label = label;
		this.conversion = conversion;
	}

	@Override
	public String getFriendlyName() {
		return label;
	}

	public String convert(XivPlayerCharacter xpc) {
		return conversion.apply(xpc);
	}
}
