package gg.xp.xivsupport.gui.library;

import gg.xp.xivdata.data.StatusEffectIcon;
import gg.xp.xivdata.data.StatusEffectInfo;
import gg.xp.xivdata.data.StatusEffectLibrary;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.IdOrNameFilter;
import gg.xp.xivsupport.gui.tables.filters.TextBasedFilter;
import gg.xp.xivsupport.gui.tables.renderers.StatusEffectListRenderer;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Deprecated // Use StatusTableFactory
public final class StatusTable {
	private StatusTable() {
	}

	@Deprecated // Use StatusTableFactory.table()
	public static TableWithFilterAndDetails<StatusEffectInfo, Object> table() {
		return TableWithFilterAndDetails.builder("Status Effects", () -> {
					Map<Integer, StatusEffectInfo> csvValues = StatusEffectLibrary.getAll();
					List<StatusEffectInfo> values = new ArrayList<>(csvValues.values());
					values.sort(Comparator.comparing(StatusEffectInfo::statusEffectId));
					return values;
				}, unused -> Collections.emptyList())
				.addMainColumn(new CustomColumn<>("ID", v -> String.format("0x%X (%s)", v.statusEffectId(), v.statusEffectId()), col -> {
					col.setMinWidth(100);
					col.setMaxWidth(100);
				}))
				.addMainColumn(new CustomColumn<>("Name", StatusEffectInfo::name, col -> {
					col.setPreferredWidth(200);
				}))
				.addMainColumn(new CustomColumn<>("Description", StatusEffectInfo::description, col -> {
					col.setPreferredWidth(500);
				}))
				.addMainColumn(new CustomColumn<>("Stacks", StatusEffectInfo::maxStacks, col -> {
					col.setMinWidth(50);
					col.setMaxWidth(50);
				}))
				.addMainColumn(new CustomColumn<>("Icons", statusEffectInfo -> statusEffectInfo.getAllIcons().stream().map(StatusEffectIcon::getIconUrl).toList(), col -> {
					col.setCellRenderer(new StatusEffectListRenderer());
					col.setPreferredWidth(500);
				}))
				.addFilter(t -> new IdOrNameFilter<>("Name/ID", StatusEffectInfo::statusEffectId, StatusEffectInfo::name, t))
				.addFilter(t -> new TextBasedFilter<>(t, "Description", StatusEffectInfo::description))
				.setFixedData(true)
				.build();
	}

	public static void showChooser(Window frame, Consumer<StatusEffectInfo> callback) {
		TableWithFilterAndDetails<StatusEffectInfo, Object> table = table();
		ChooserDialog.showChooser(frame, table, callback);
	}

	public static List<StatusEffectInfo> pickItems(Window window) {
		return ChooserDialog.chooserReturnItems(window, table());
	}

	public static @Nullable StatusEffectInfo pickItem(Window window) {
		return ChooserDialog.chooserReturnItem(window, table());
	}
}
