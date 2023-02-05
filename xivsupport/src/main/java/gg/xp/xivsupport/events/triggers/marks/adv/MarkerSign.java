package gg.xp.xivsupport.events.triggers.marks.adv;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

import java.util.Arrays;
import java.util.Locale;

public enum MarkerSign implements HasFriendlyName {

	ATTACK_NEXT("Next Attack", "attack"),
	ATTACK1("Attack 1", "attack1", ATTACK_NEXT),
	ATTACK2("Attack 2", "attack2", ATTACK_NEXT),
	ATTACK3("Attack 3", "attack3", ATTACK_NEXT),
	ATTACK4("Attack 4", "attack4", ATTACK_NEXT),
	ATTACK5("Attack 5", "attack5", ATTACK_NEXT),

	BIND_NEXT("Next Bind", "bind"),
	BIND1("Bind 1", "bind1", BIND_NEXT),
	BIND2("Bind 2", "bind2", BIND_NEXT),
	BIND3("Bind 3", "bind3", BIND_NEXT),

	IGNORE_NEXT("Next Ignore", "ignore") {
		@Override
		public String getKoreanCommand() {
			return "stop";
		}
	},
	IGNORE1("Ignore 1", "ignore1", IGNORE_NEXT) {
		@Override
		public String getKoreanCommand() {
			return "stop1";
		}
	},
	IGNORE2("Ignore 2", "ignore2", IGNORE_NEXT) {
		@Override
		public String getKoreanCommand() {
			return "stop2";
		}
	},

	CIRCLE("Circle", "circle"),
	CROSS("Cross", "cross"),
	SQUARE("Square", "square"),
	TRIANGLE("Triangle", "triangle"),

	CLEAR("Clear Marker", "clear");

	private final String desc;
	private final String command;
	private final MarkerSign base;

	MarkerSign(String desc, String command) {
		this.desc = desc;
		this.command = command;
		this.base = this;
	}

	MarkerSign(String desc, String command, MarkerSign base) {
		this.desc = desc;
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

	public static MarkerSign fromId(int id) {
		return switch (id) {
			case 0 -> ATTACK1;
			case 1 -> ATTACK2;
			case 2 -> ATTACK3;
			case 3 -> ATTACK4;
			case 4 -> ATTACK5;
			case 5 -> BIND1;
			case 6 -> BIND2;
			case 7 -> BIND3;
			case 8 -> IGNORE1;
			case 9 -> IGNORE2;
			case 10 -> SQUARE;
			case 11 -> CIRCLE;
			case 12 -> CROSS;
			case 13 -> TRIANGLE;
			default -> throw new IllegalArgumentException("Unrecognized marker sign: " + id);
		};
	}

	public String getCommand() {
		return command;
	}

	public String getKoreanCommand() {
		return command;
	}

	public MarkerSign getBase() {
		return base;
	}

	@Override
	public String getFriendlyName() {
		return desc;
	}
}
