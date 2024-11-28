package gg.xp.xivsupport.gui.library;

final class XivApiUtils {
	private XivApiUtils() {
	}

	static String singleItemUrl(String sheetName, long id) {
		return String.format("https://beta.xivapi.com/api/1/sheet/%s/%d", sheetName, id);
	}
}
