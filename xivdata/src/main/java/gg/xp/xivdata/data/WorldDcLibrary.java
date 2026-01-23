package gg.xp.xivdata.data;

import java.util.Map;

public final class WorldDcLibrary {

	private static final WorldDcLibraryImpl INSTANCE = new WorldDcLibraryImpl(
			WorldDcLibrary.class.getResourceAsStream("/xiv/worlds/Worlds.oos.gz"),
			WorldDcLibrary.class.getResourceAsStream("/xiv/worlds/Datacenter.oos.gz")
	);

	private WorldDcLibrary() {

	}

	public static WorldInfo getWorld(int id) {
		return INSTANCE.getWorld(id);
	}

	public static DcInfo getDc(int id) {
		return INSTANCE.getDc(id);
	}

	public static Map<Integer, WorldInfo> getAllWorlds() {
		return INSTANCE.getAllWorlds();
	}

	public static Map<Integer, DcInfo> getAllDcs() {
		return INSTANCE.getAllDcs();
	}
}
