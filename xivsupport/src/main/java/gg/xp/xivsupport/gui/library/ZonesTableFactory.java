package gg.xp.xivsupport.gui.library;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomRightClickOption;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.IdOrNameFilter;
import gg.xp.xivsupport.gui.util.GuiUtil;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ScanMe
public final class ZonesTableFactory {

	private final RightClickOptionRepo rightClickOptionRepo;

	public ZonesTableFactory(RightClickOptionRepo rightClickOptionRepo) {
		this.rightClickOptionRepo = rightClickOptionRepo;
	}

	public TableWithFilterAndDetails<ZoneInfo, Object> table() {
		return TableWithFilterAndDetails.builder("Zones", () -> {
					Map<Integer, ZoneInfo> csvValues = ZoneLibrary.getFileValues();
					return csvValues.values().stream().sorted(Comparator.comparing(ZoneInfo::id)).toList();
				}, unused -> Collections.emptyList())
				.addMainColumn(new CustomColumn<>("ID", v -> String.format("0x%X (%s)", v.id(), v.id()), col -> {
					col.setMinWidth(100);
					col.setMaxWidth(100);
				}))
				.addMainColumn(new CustomColumn<>("Place Name", ZoneInfo::placeName, col -> {
//					col.setPreferredWidth(200);
				}))
				.addMainColumn(new CustomColumn<>("Duty Name", ZoneInfo::dutyName, col -> {
//					col.setPreferredWidth(200);
				}))
				.addFilter(t -> new IdOrNameFilter<>("Name/ID", zi -> (long) zi.id(), zi -> String.format("%s %s", zi.dutyName(), zi.placeName()), t))
				.addWidget(tbl -> JumpToIdWidget.create(tbl, zi -> (long) zi.id()))
				.setFixedData(true)
				.withRightClickRepo(rightClickOptionRepo.withMore(
						CustomRightClickOption.forRow("Open on XivAPI", ZoneInfo.class, zi -> {
							GuiUtil.openUrl(XivApiUtils.singleItemUrl("TerritoryType", zi.id()));
						})
				))
				.build();
	}

	public void showChooser(Window frame, Consumer<ZoneInfo> callback) {
		TableWithFilterAndDetails<ZoneInfo, Object> table = table();
		ChooserDialog.showChooser(frame, table, callback);
	}

	public List<ZoneInfo> pickItems(Window window) {
		return ChooserDialog.chooserReturnItems(window, table());
	}

	public @Nullable ZoneInfo pickItem(Window window) {
		return ChooserDialog.chooserReturnItem(window, table());
	}
}
