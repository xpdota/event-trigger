package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActionLibraryImpl {

	private static final Logger log = LoggerFactory.getLogger(ActionLibraryImpl.class);

	private final Map<Long, ActionIcon> iconCache = new ConcurrentHashMap<>();
	private final Map<Integer, ActionInfo> values;

	public ActionLibraryImpl(InputStream input) {
		log.info("Loading action library");
		values = CompressedObjectStreamLoader.loadFrom(input, (ActionInfo t) -> ((int) t.actionid()));
		log.info("Loaded action library");
	}

	public Map<Integer, ActionInfo> getAll() {
		return Collections.unmodifiableMap(values);
	}

	public @Nullable ActionInfo forId(int id) {
		return getAll().get(id);
	}

	public @Nullable ActionInfo forId(long id) {
		return getAll().get((int) id);
	}

	// Special value to indicate no icon
	private static final ActionIcon NULL_MARKER;

	static {
		try {
			NULL_MARKER = new ActionIcon(new URL("http://bar/"));
		}
		catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	// Unlike status effects, which are more complicated due to multi-icon for different stack numbers, this maps
	// the ability ID to icon directly, skipping the "icon ID" step.
	public @Nullable ActionIcon iconForId(long id) {
		ActionIcon result = iconCache.computeIfAbsent(id, missingId -> {
			ActionInfo actionInfo = forId(missingId);
			if (actionInfo == null) {
				return NULL_MARKER;
			}
			long iconId = actionInfo.iconId();
			URL resource = ActionIcon.class.getResource(String.format("/xiv/icon/%06d_hr1.png", iconId));
			if (resource == null) {
				return NULL_MARKER;
			}
			return new ActionIcon(resource);
		});
		if (result == NULL_MARKER) {
			return null;
		}
		return result;
	}

	@Nullable ActionIcon iconForInfo(ActionInfo info) {
		ActionIcon result = iconCache.computeIfAbsent(info.actionid(), missingId -> {
			long iconId = info.iconId();
			URL resource = ActionIcon.class.getResource(String.format("/xiv/icon/%06d_hr1.png", iconId));
			if (resource == null) {
				return NULL_MARKER;
			}
			return new ActionIcon(resource);
		});
		if (result == NULL_MARKER) {
			return null;
		}
		return result;

	}

}
