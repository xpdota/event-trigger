package gg.xp.xivsupport.gui.library;

import gg.xp.xivdata.data.ActionIcon;
import gg.xp.xivdata.data.ActionInfo;
import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.IdOrNameFilter;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class ActionTable {
	private ActionTable() {
	}

	public static TableWithFilterAndDetails<ActionInfo, Object> table() {
		return TableWithFilterAndDetails.builder("Actions/Abilities", () -> {
					Map<Long, ActionInfo> csvValues = ActionLibrary.getAll();
					List<ActionInfo> values = new ArrayList<>(csvValues.values());
					values.sort(Comparator.comparing(ActionInfo::actionid));
					return values;
				}, unused -> Collections.emptyList())
				.addMainColumn(new CustomColumn<>("ID", v -> String.format("0x%X (%s)", v.actionid(), v.actionid()), col -> {
					col.setMinWidth(100);
					col.setMaxWidth(100);
				}))
				.addMainColumn(new CustomColumn<>("Name", ActionInfo::name, col -> {
					col.setPreferredWidth(200);
				}))
				.addMainColumn(new CustomColumn<>("Icon", ai -> {
					ActionIcon icon = ai.getIcon();
					if (icon == null) {
						return null;
					}
					else {
						return icon.getIconUrl();
					}
				}, col -> {
					col.setCellRenderer(new ActionAndStatusRenderer(true, false, false));
					col.setPreferredWidth(500);
				}))
				.addFilter(t -> new IdOrNameFilter<>("Name/ID", ActionInfo::actionid, ActionInfo::name, t))
				.setFixedData(true)
				.build();
	}

	public static void showChooser(Window owner, Consumer<ActionInfo> callback) {
		TableWithFilterAndDetails<ActionInfo, Object> table = table();
		ChooserDialog.showChooser(owner, table, callback);
	}
}
