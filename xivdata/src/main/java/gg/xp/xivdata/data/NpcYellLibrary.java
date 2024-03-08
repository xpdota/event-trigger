package gg.xp.xivdata.data;

import gg.xp.xivdata.util.ArrayBackedMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class NpcYellLibrary {

	public static final NpcYellLibrary INSTANCE = new NpcYellLibrary();
	private final CsvMapLoader<Integer, NpcYellInfo> loader;

	public NpcYellLibrary() {
		loader = CsvMapLoader.builder(() -> ReadCsv.cellsFromResource("/xiv/npcyell/NpcYell.csv"), NpcYellLibrary::parseRow,
						(row, item) -> item.id())
				.setMapFinisher(ArrayBackedMap::new)
				.preFilterNullIds()
				.build();
	}

	private static NpcYellInfo parseRow(CsvRowHelper row) {
		int id = row.getIntId();
		String value = row.getStringOrNull(11);
		if (value == null) {
			return null;
		}
		return new NpcYellInfo(id, value);
	}

	public Map<Integer, NpcYellInfo> getAll() {
		return loader.read();
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
