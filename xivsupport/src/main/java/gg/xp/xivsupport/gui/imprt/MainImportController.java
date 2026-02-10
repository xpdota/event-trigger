package gg.xp.xivsupport.gui.imprt;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.CustomJsonListSetting;

import java.util.List;

public class MainImportController {

	private static final int HISTORY_SIZE = 100;
	private final CustomJsonListSetting<ImportSpec<?>> recents;

	public MainImportController(PersistenceProvider pers) {
		ObjectMapper mapper = JsonMapper.builder()
				.configure(MapperFeature.INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES, false)
				.build();
		recents = CustomJsonListSetting.builder(pers, new TypeReference<ImportSpec<?>>() {
		}, "import-data.recent-imports", "import-data.recent-imports-failures")
				.withMapper(mapper)
				.build();
	}

	public List<ImportSpec<?>> getRecentImports() {
		return recents.getItems();
	}

	public <X extends Event> EventIterator<X> readEvents(ImportSpec<X> importSpec, boolean saveToRecents) {
		EventIterator<X> iter = importSpec.eventIter();
		if (saveToRecents) {
			// Only add once we know we can successfully import
			List<ImportSpec<?>> items = recents.getItems();
			items.add(0, importSpec);
			if (items.size() > HISTORY_SIZE) {
				items = items.subList(0, HISTORY_SIZE);
			}
			recents.setItems(items);
		}
		return iter;
	}

}
