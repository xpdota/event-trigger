package gg.xp.xivsupport.gui.map;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.util.HasFriendlyName;
import org.jetbrains.annotations.Nullable;

public enum OmenType implements HasFriendlyName {
	UNKNOWN("Unknown"),
	RAIDWIDE("Raidwide"),
	CIRCLE("Circle"),
	DONUT("Donut"),
	RECTANGLE("Rectangle"),
	RECTANGLE_CENTERED("Rectangle, Centered"),
	CONE("Cone"),
	CROSS("Cross");

	private final String friendlyName;

	OmenType(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}

	public static OmenType fromActionInfo(@Nullable ActionInfo info) {
		if (info == null) {
			return UNKNOWN;
		}
		switch (info.castType()) {
			case 2, 5, 6, 7 -> {
				if (info.effectRange() >= 100) {
					return RAIDWIDE;
				}
				return CIRCLE;
			}
			case 3, 13 -> {
				return CONE;
			}
			case 10 -> {
				return DONUT;
			}
			case 11 -> {
				return CROSS;
			}
			case 4, 8 -> {
				return RECTANGLE;
			}
			case 12 -> {
				return RECTANGLE_CENTERED;
			}
			default -> {
				return UNKNOWN;
			}
		}
	}

	public static String describe(@Nullable ActionInfo ai) {
		if (ai == null) {
			return "";
		}
		int range = ai.effectRange();
		if (range == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(range).append('y');
		OmenType omenType = fromActionInfo(ai);
		switch (omenType) {
			case CIRCLE -> {
				sb.append(" (Circle)");
			}
			case DONUT -> {
				sb.append(" (Donut)");
			}
			case RECTANGLE -> {
				sb.append('×').append(ai.xAxisModifier()).append("y (Rectangle)");
			}
			case RECTANGLE_CENTERED -> {
				sb.append('×').append(ai.xAxisModifier()).append("y (Rectangle, Front/Back)");
			}
			case CONE -> {
				sb.append(" (Cone)");
			}
			case RAIDWIDE -> {
				sb.append(" (Raidwide)");
			}
			case UNKNOWN -> {
				sb.append(" (Unknown: ").append(ai.castType()).append(", ").append(ai.xAxisModifier()).append(')');
			}
		}
		return sb.toString();
	}
}
