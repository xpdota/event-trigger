package gg.xp.xivsupport.gui.tabs;

import gg.xp.xivdata.data.ActionIcon;
import gg.xp.xivdata.data.ActionInfo;
import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivdata.data.StatusEffectIcon;
import gg.xp.xivdata.data.StatusEffectInfo;
import gg.xp.xivdata.data.StatusEffectLibrary;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.IdOrNameFilter;
import gg.xp.xivsupport.gui.tables.filters.TextBasedFilter;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.gui.tables.renderers.StatusEffectListRenderer;
import org.picocontainer.PicoContainer;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class LibraryTab extends JTabbedPane {

	public LibraryTab(PicoContainer container) {
		super(LEFT);
		{
			TableWithFilterAndDetails<StatusEffectInfo, Object> statusTable = TableWithFilterAndDetails.builder("Status Effects", () -> {
						Map<Long, StatusEffectInfo> csvValues = StatusEffectLibrary.getAll();
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
					.setAppendOrPruneOnly(true)
					.build();

			statusTable.setBottomScroll(false);
			addTab("Status Effects", statusTable);
		}
		{
			TableWithFilterAndDetails<ActionInfo, Object> actionsTable = TableWithFilterAndDetails.builder("Actions/Abilities", () -> {
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
					.setAppendOrPruneOnly(true)
					.build();

			actionsTable.setBottomScroll(false);
			addTab("Actions/Abilities", actionsTable);
			
		}
	}


}
