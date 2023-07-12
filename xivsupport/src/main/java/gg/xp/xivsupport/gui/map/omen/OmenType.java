package gg.xp.xivsupport.gui.map.omen;

import gg.xp.xivsupport.gui.util.HasFriendlyName;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the properties we can infer from the "CastType" column
 */
public enum OmenType implements HasFriendlyName {

	CIRCLE("Circle", OmenShape.CIRCLE, false, OmenLocationType.TARGET_IF_AVAILABLE),
	CIRCLE_HB("Circle+Hitbox", OmenShape.CIRCLE, true, OmenLocationType.TARGET_IF_AVAILABLE),
	DONUT("Donut", OmenShape.DONUT, false, OmenLocationType.CASTER),
	DONUT_HB("Donut+Hitbox", OmenShape.DONUT, true, OmenLocationType.CASTER),
	RECTANGLE("Rectangle", OmenShape.RECTANGLE, false, OmenLocationType.CASTER_FACE_TARGET),
	RECTANGLE_HB("Rectangle+Hitbox", OmenShape.RECTANGLE, true, OmenLocationType.CASTER_FACE_TARGET),
	CONE("Cone", OmenShape.CONE, false, OmenLocationType.CASTER_FACE_TARGET),
	CONE_HB("Cone+Hitbox", OmenShape.CONE, true, OmenLocationType.CASTER_FACE_TARGET),
	CROSS("Cross", OmenShape.CROSS, false, OmenLocationType.CASTER),
	RECTANGLE_FRONT_BACK("Front/Back Rectangle", OmenShape.RECTANGLE_CENTERED, false, OmenLocationType.CASTER),
	;

	private final String friendlyName;
	private final OmenShape type;
	private final boolean addHitbox;
	private final OmenLocationType locationType;

	OmenType(String friendlyName, OmenShape type, boolean addHitbox, OmenLocationType locationType) {
		this.friendlyName = friendlyName;
		this.type = type;
		this.addHitbox = addHitbox;
		this.locationType = locationType;
	}

	public String getFriendlyName() {
		return friendlyName;
	}

	public static @Nullable OmenType fromCastType(int castType) {
		return switch (castType) {
			case 2 -> CIRCLE;
			case 3 -> CONE_HB;
			case 4 -> RECTANGLE_HB;
			case 5 -> CIRCLE_HB;
			// HB or not?
			case 6 -> CIRCLE;
			// HB or not?
			case 8 -> RECTANGLE;
			case 10 -> DONUT;
			case 11 -> CROSS;
			case 12 -> RECTANGLE;
			case 13 -> CONE;
			default -> null;
		};
	}

	public OmenShape shape() {
		return type;
	}

	public boolean addHitbox() {
		return addHitbox;
	}

	public OmenLocationType locationType() {
		return locationType;
	}

	@Override
	public String toString() {
		return "AbilityOmenInfo[" +
		       "type=" + type + ", " +
		       "addHitbox=" + addHitbox + ", " +
		       "locationType=" + locationType + ']';
	}


}
