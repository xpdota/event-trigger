package gg.xp.xivdata.data;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * Library for worlds and DCs
 */
public final class WorldDcLibraryImpl {
	private final Map<Integer, WorldInfo> worlds;
	private final Map<Integer, DcInfo> dcs;

	public WorldDcLibraryImpl(InputStream worldsStream, InputStream dcsStream) {
		worlds = CompressedObjectStreamLoader.loadFrom(worldsStream, WorldInfo::id);
		dcs = CompressedObjectStreamLoader.loadFrom(dcsStream, DcInfo::id);
	}

	public WorldInfo getWorld(int id) {
		WorldInfo worldInfo = worlds.get(id);
		if (worldInfo == null) {
			return worlds.get(0);
		}
		return worldInfo;
	}

	public DcInfo getDc(int id) {
		DcInfo dcInfo = dcs.get(id);
		if (dcInfo == null) {
			return dcs.get(0);
		}
		return dcInfo;
	}

	public Map<Integer, WorldInfo> getAllWorlds() {
		return Collections.unmodifiableMap(worlds);
	}

	public Map<Integer, DcInfo> getAllDcs() {
		return Collections.unmodifiableMap(dcs);
	}
}
