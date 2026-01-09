package gg.xp.xivsupport.gui.library;

final class XivApiUtils {
	private XivApiUtils() {
	}

	static String singleItemUrl(String sheetName, long id) {
		return String.format("https://v2.xivapi.com/api/sheet/%s/%d", sheetName, id);
	}
}
