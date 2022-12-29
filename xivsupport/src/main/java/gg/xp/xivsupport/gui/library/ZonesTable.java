package gg.xp.xivsupport.gui.library;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.IdOrNameFilter;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class ZonesTable {

	private ZonesTable() {
	}

	public static TableWithFilterAndDetails<ZoneInfo, Object> table() {
		return TableWithFilterAndDetails.builder("Zones", () -> {
					Map<Integer, ZoneInfo> csvValues = ZoneLibrary.getFileValues();
					return csvValues.values().stream().sorted(Comparator.comparing(ZoneInfo::id)).toList();
				}, unused -> Collections.emptyList())
				.addMainColumn(new CustomColumn<>("ID", v -> String.format("0x%X (%s)", v.id(), v.id()), col -> {
					col.setMinWidth(100);
					col.setMaxWidth(100);
				}))
//				.addMainColumn(new CustomColumn<>("Place Name", zi -> zi.placeName(), col -> {
////					col.setPreferredWidth(200);
//				}))
				.addMainColumn(new CustomColumn<>("Duty Name", ZoneInfo::dutyName, col -> {
//					col.setPreferredWidth(200);
				}))
				.addFilter(t -> new IdOrNameFilter<>("Name/ID", zi -> (long) zi.id(), zi -> String.format("%s %s", zi.dutyName(), zi.placeName()), t))
				.setFixedData(true)
				.build();
	}

	public static void showChooser(Window frame, Consumer<ZoneInfo> callback) {
		TableWithFilterAndDetails<ZoneInfo, Object> table = table();
		ChooserDialog.showChooser(frame, table, callback);
	}

	public static List<ZoneInfo> pickItems(Window window) {
		return ChooserDialog.chooserReturnItems(window, table());
	}

	public static @Nullable ZoneInfo pickItem(Window window) {
		return ChooserDialog.chooserReturnItem(window, table());
	}
}
