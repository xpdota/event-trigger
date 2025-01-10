package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ActionLibrary {

	// TODO: this should actually list the errors
	public static void checkForErrors() {
		Map<Integer, ActionInfo> all = getAll();
		if (all.size() < 100) {
			throw new AssertionError("Action Library failed to load!");
		}
	}

	private static final ActionLibraryImpl INSTANCE = new ActionLibraryImpl(ActionLibrary.class.getResourceAsStream("/xiv/actions/Action.oos.gz"));

	public static Map<Integer, ActionInfo> getAll() {
		return INSTANCE.getAll();
	}

	public static @Nullable ActionInfo forId(int id) {
		return getAll().get(id);
	}

	public static @Nullable ActionInfo forId(long id) {
		return getAll().get((int) id);
	}

	public static @Nullable ActionIcon iconForId(long id) {
		return INSTANCE.iconForId(id);
	}

	static @Nullable ActionIcon iconForInfo(ActionInfo info) {
		return INSTANCE.iconForInfo(info);
	}
}
