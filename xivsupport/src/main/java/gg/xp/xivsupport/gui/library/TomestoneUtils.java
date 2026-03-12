package gg.xp.xivsupport.gui.library;

import gg.xp.xivsupport.gui.util.GuiUtil;

import java.util.function.Consumer;
import java.util.function.Function;

final class TomestoneUtils {
	private TomestoneUtils() {
	}

	static String singleItemUrl(String sheetName, long id) {
		return String.format("https://tomestone.gg/%s/%d/", sheetName, id);
	}

	static void openSingleItemUrl(String sheetName, long id) {
		GuiUtil.openUrl(singleItemUrl(sheetName, id));
	}

	static <X> Consumer<X> singleItemUrlOpener(String sheetName, Function<X, Long> idGetter) {
		return x -> openSingleItemUrl(sheetName, idGetter.apply(x));
	}
}
