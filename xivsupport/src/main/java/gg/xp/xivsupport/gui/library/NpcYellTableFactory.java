package gg.xp.xivsupport.gui.library;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomRightClickOption;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.IdFilter;
import gg.xp.xivsupport.gui.tables.filters.TextBasedFilter;
import gg.xp.xivsupport.gui.util.GuiUtil;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@ScanMe
public final class NpcYellTableFactory {

	private final RightClickOptionRepo rightClicks;

	public NpcYellTableFactory(RightClickOptionRepo rightClicks) {
		this.rightClicks = rightClicks;
	}

	public TableWithFilterAndDetails<NpcYellInfo, Object> table() {
		return TableWithFilterAndDetails.<NpcYellInfo, Object>builder("NpcYell Entries", () -> {
					Map<Integer, NpcYellInfo> csvValues = NpcYellLibrary.INSTANCE.getAll();
					List<NpcYellInfo> values = new ArrayList<>(csvValues.values());
					values.sort(Comparator.comparing(NpcYellInfo::id));
					return values;
				}, unused -> Collections.emptyList())
				.addMainColumn(new CustomColumn<NpcYellInfo>("ID", v -> String.format("0x%X (%s)", v.id(), v.id()), col -> {
					col.setMinWidth(100);
					col.setMaxWidth(100);
				}).withFilter(t -> new IdFilter<>(t, "ID", nyi -> (long) nyi.id())))
				.addMainColumn(new CustomColumn<NpcYellInfo>("Text", NpcYellInfo::text, col -> {
					col.setPreferredWidth(200);
				}).withFilter(t -> new TextBasedFilter<>(t, "Text", NpcYellInfo::text)))
				.addWidget(tbl -> JumpToIdWidget.create(tbl, nyi -> (long) nyi.id()))
				// TODO: ability to create easy trigger from library
				.withRightClickRepo(rightClicks.withMore(
						CustomRightClickOption.forRow("Open on XivAPI", NpcYellInfo.class, nyi -> {
							GuiUtil.openUrl(XivApiUtils.singleItemUrl("NpcYell", nyi.id()));
						})
				))
				.setFixedData(true)
				.build();
	}

	public @Nullable NpcYellInfo pickItem(Window owner) {
		return ChooserDialog.chooserReturnItem(owner, table());
	}

}
