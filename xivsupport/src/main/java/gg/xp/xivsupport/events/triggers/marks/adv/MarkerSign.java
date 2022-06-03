package gg.xp.xivsupport.events.triggers.marks.adv;

import java.util.Arrays;
import java.util.Locale;

public enum MarkerSign {

	ATTACK_NEXT("attack"),
	ATTACK1("attack1", ATTACK_NEXT),
	ATTACK2("attack2", ATTACK_NEXT),
	ATTACK3("attack3", ATTACK_NEXT),
	ATTACK4("attack4", ATTACK_NEXT),
	ATTACK5("attack5", ATTACK_NEXT),

	BIND_NEXT("bind"),
	BIND1("bind1", BIND_NEXT),
	BIND2("bind2", BIND_NEXT),
	BIND3("bind3", BIND_NEXT),

	IGNORE_NEXT("ignore"),
	IGNORE1("ignore1", IGNORE_NEXT),
	IGNORE2("ignore2", IGNORE_NEXT),

	CIRCLE("circle"),
	CROSS("cross"),
	SQUARE("square"),
	TRIANGLE("triangle"),

	CLEAR("clear");

	private final String command;
	private final MarkerSign base;

	MarkerSign(String command) {
		this.command = command;
		this.base = this;
	}

	MarkerSign(String command, MarkerSign base) {
		this.command = command;
		this.base = base;
	}

	public static MarkerSign of(String s) {
		try {
			return valueOf(s.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException e) {
			return Arrays.stream(values())
					.filter(value -> value.getCommand().equalsIgnoreCase(s))
					.findAny()
					.orElseThrow(() -> new IllegalArgumentException("Not a valid marker: " + s));
		}
	}

	public String getCommand() {
		return command;
	}

	public MarkerSign getBase() {
		return base;
	}
}
