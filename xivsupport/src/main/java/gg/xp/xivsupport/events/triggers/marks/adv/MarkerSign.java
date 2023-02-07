package gg.xp.xivsupport.events.triggers.marks.adv;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.util.HasFriendlyName;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;

public enum MarkerSign implements HasFriendlyName, HasOptionalIconURL {

	ATTACK_NEXT("Next Attack", "attack"),
	ATTACK1("Attack 1", "attack1", ATTACK_NEXT, 60701),
	ATTACK2("Attack 2", "attack2", ATTACK_NEXT, 60702),
	ATTACK3("Attack 3", "attack3", ATTACK_NEXT, 60703),
	ATTACK4("Attack 4", "attack4", ATTACK_NEXT, 60704),
	ATTACK5("Attack 5", "attack5", ATTACK_NEXT, 60705),

	BIND_NEXT("Next Bind", "bind"),
	BIND1("Bind 1", "bind1", BIND_NEXT, 60706),
	BIND2("Bind 2", "bind2", BIND_NEXT, 60707),
	BIND3("Bind 3", "bind3", BIND_NEXT, 60708),

	IGNORE_NEXT("Next Ignore", "ignore") {
		@Override
		public String getKoreanCommand() {
			return "stop";
		}
	},
	IGNORE1("Ignore 1", "ignore1", IGNORE_NEXT, 60709) {
		@Override
		public String getKoreanCommand() {
			return "stop1";
		}
	},
	IGNORE2("Ignore 2", "ignore2", IGNORE_NEXT, 60710) {
		@Override
		public String getKoreanCommand() {
			return "stop2";
		}
	},

	CIRCLE("Circle", "circle", 60712),
	CROSS("Cross", "cross", 60713),
	SQUARE("Square", "square", 60711),
	TRIANGLE("Triangle", "triangle", 60714),

	CLEAR("Clear Marker", "clear");

	private final String desc;
	private final String command;
	private final MarkerSign base;
	private final @Nullable HasIconURL iconUrl;

	MarkerSign(String desc, String command) {
		this.desc = desc;
		this.command = command;
		this.base = this;
		iconUrl = null;
	}

	MarkerSign(String desc, String command, int icon) {
		this.desc = desc;
		this.command = command;
		this.base = this;
		iconUrl = makeIcon(icon);
	}

	MarkerSign(String desc, String command, MarkerSign base, int icon) {
		this.desc = desc;
		this.command = command;
		this.base = base;
		iconUrl = makeIcon(icon);
	}

	private static @Nullable HasIconURL makeIcon(@Nullable Integer icon) {
		if (icon == null) {
			return null;
		}
		return IconUtils.makeIcon(icon);
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
	public @Nullable HasIconURL getIconUrl() {
		return iconUrl;
	}

	@Override
	public String getFriendlyName() {
		return desc;
	}
}
