package gg.xp.xivsupport.gui.library;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomRightClickOption;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.BooleanEventFilter;
import gg.xp.xivsupport.gui.tables.filters.IdOrNameFilter;
import gg.xp.xivsupport.gui.tables.filters.TextBasedFilter;
import gg.xp.xivsupport.gui.tables.renderers.StatusEffectListRenderer;
import gg.xp.xivsupport.gui.util.GuiUtil;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ScanMe
public final class StatusTableFactory {

	private final RightClickOptionRepo rightClickOptionRepo;

	public StatusTableFactory(RightClickOptionRepo rightClickOptionRepo) {
		this.rightClickOptionRepo = rightClickOptionRepo;
	}

	public TableWithFilterAndDetails<StatusEffectInfo, Object> table() {
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
				.addFilter(t -> new BooleanEventFilter<>(t, "Show Useless", (checked, item) -> checked || !item.isUseless(), false))
				.addWidget(tbl -> JumpToIdWidget.create(tbl, StatusEffectInfo::statusEffectId))
				.setFixedData(true)
				.withRightClickRepo(rightClickOptionRepo.withMore(
						CustomRightClickOption.forRow("Open on XivAPI", StatusEffectInfo.class, sei -> {
							GuiUtil.openUrl(XivApiUtils.singleItemUrl("Status", sei.statusEffectId()));
						})
				))
				.build();
	}

	public void showChooser(Window frame, Consumer<StatusEffectInfo> callback) {
		TableWithFilterAndDetails<StatusEffectInfo, Object> table = table();
		ChooserDialog.showChooser(frame, table, callback);
	}

	public List<StatusEffectInfo> pickItems(Window window) {
		return ChooserDialog.chooserReturnItems(window, table());
	}

	public @Nullable StatusEffectInfo pickItem(Window window) {
		return ChooserDialog.chooserReturnItem(window, table());
	}
}
