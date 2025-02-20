package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

public class NpcYellLibrary {

	public static final NpcYellLibrary INSTANCE = new NpcYellLibrary(NpcYellLibrary.class.getResourceAsStream("/xiv/npcyell/NpcYell.oos.gz"));

	private final Map<Integer, NpcYellInfo> values;

	public NpcYellLibrary(InputStream input) {
		values = CompressedObjectStreamLoader.loadFrom(input, NpcYellInfo::id);
	}

	public Map<Integer, NpcYellInfo> getAll() {
		return Collections.unmodifiableMap(values);
	}

	public @Nullable NpcYellInfo forId(int id) {
		NpcYellInfo npcYellInfo = getAll().get(id);
		if (npcYellInfo != null) {
			return npcYellInfo;
		}
		else {
			return new NpcYellInfo(id, "Unknown %s (0x%X)".formatted(id, id));
		}
	}

	public @Nullable NpcYellInfo forId(long id) {
		return forId((int) id);
	}

}
