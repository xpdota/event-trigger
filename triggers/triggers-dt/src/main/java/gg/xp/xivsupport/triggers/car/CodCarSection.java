package gg.xp.xivsupport.triggers.car;

import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.Position;

public enum CodCarSection {
	INSIDE,
	WEST_OUTSIDE,
	EAST_OUTSIDE;

	private static final ArenaPos ap = new ArenaPos(100, 100, 18.5, 10);

	public static CodCarSection forPos(Position pos) {
		double dist = pos.distanceFrom2D(Position.of2d(100, 100));
		// Outer ring
		if (dist > 33) {
			return pos.x() < 100 ? WEST_OUTSIDE : EAST_OUTSIDE;
		}
		ArenaSector sect = ap.forPosition(pos);
		return switch (sect) {
			case WEST -> WEST_OUTSIDE;
			case EAST -> EAST_OUTSIDE;
			default -> INSIDE;
		};
	}
}
