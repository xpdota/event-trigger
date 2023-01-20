package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;

public class ActionLibrary {

	// TODO: this should actually list the errors
	public static void checkForErrors() {
		Map<Integer, ActionInfo> all = getAll();
		if (all.size() < 100) {
			throw new AssertionError("Action Library failed to load!");
		}
		// TODO
//		if (hasErrors) {
//			throw new RuntimeException("There was an error - check log for errors from ActionLibrary");
//		}
	}

	private static final ActionLibraryImpl INSTANCE = new ActionLibraryImpl(() -> ReadCsv.cellsFromResource("/xiv/actions/Action.csv"));


//	public static void main(String[] args) {
//		getAll().values().stream().distinct().sorted().map(s -> String.format("%06d", s)).forEach(System.out::println);
//	}
//
	public static Map<Integer, ActionInfo> getAll() {
		return INSTANCE.getAll();
	}

	public static @Nullable ActionInfo forId(int id) {
		return getAll().get(id);
	}

	public static @Nullable ActionInfo forId(long id) {
		return getAll().get((int) id);
	}

	public static ActionLibraryImpl readAltCsv(File file) {
		return new ActionLibraryImpl(() -> ReadCsv.cellsFromFile(file));
	}

	public static @Nullable ActionIcon iconForId(long id) {
		return INSTANCE.iconForId(id);
	}

	static @Nullable ActionIcon iconForInfo(ActionInfo info) {
		return INSTANCE.iconForInfo(info);
	}
}
