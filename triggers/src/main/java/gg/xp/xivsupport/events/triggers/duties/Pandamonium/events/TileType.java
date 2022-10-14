package gg.xp.xivsupport.events.triggers.duties.Pandamonium.events;

import org.jetbrains.annotations.Nullable;

public enum TileType {
		PLUS(0x00020001),
		CROSS(0x00400020);

		public final long flag;

		TileType(long flag) {
			this.flag = flag;
		}

		public static boolean isSpawnTypeFlag(long flag) {
			return flag == PLUS.flag || flag == CROSS.flag;
		}

		public static @Nullable TileType forFLag(long flag) {
			if (flag == PLUS.flag)
				return PLUS;
			if (flag == CROSS.flag)
				return CROSS;
			else
				return null;
		}
}
