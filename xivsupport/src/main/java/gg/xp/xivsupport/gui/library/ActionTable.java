package gg.xp.xivsupport.gui.library;

import gg.xp.xivdata.data.ActionIcon;
import gg.xp.xivdata.data.ActionInfo;
import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivdata.data.HasIconURL;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomRightClickOption;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.IdOrNameFilter;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.models.XivAbility;

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
		// TODO: "initial load on filter update" would be nice
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
				.addRightClickOption(CustomRightClickOption.forRow("Copy XIVAPI Icon URL", ActionInfo.class, ai -> {
					GuiUtil.copyToClipboard(ai.getXivapiUrl().toString());
				}))
				.addRightClickOption(CustomRightClickOption.forRow("Copy XIVAPI Icon As Markdown", ActionInfo.class, ai -> {
					String md = String.format("![%s](%s)", ai.name(), ai.getXivapiUrl());
					GuiUtil.copyToClipboard(md);
				}))
//				.addRightClickOption(CustomRightClickOption.forRow("Copy XIVAPI Icon As Inline", ActionInfo.class, ai -> {
//					String md = String.format("{{< inline >}} ![%s](%s) {{< /inline >}}%s", ai.name(), ai.getXivapiUrl(), ai.name());
//					GuiUtil.copyToClipboard(md);
//				}))
				.setFixedData(true)
				.build();
	}

	public static void showChooser(Window owner, Consumer<ActionInfo> callback) {
		TableWithFilterAndDetails<ActionInfo, Object> table = table();
		ChooserDialog.showChooser(owner, table, callback);
	}
}
