package gg.xp.xivsupport.gui.library;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.ActionIcon;
import gg.xp.xivdata.data.ActionInfo;
import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivdata.data.HasIconURL;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.gui.map.omen.OmenShape;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomRightClickOption;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.IdOrNameFilter;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.RenderUtils;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.Platform;
import org.jetbrains.annotations.Nullable;
import org.swingexplorer.internal.GuiUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ActionTableFactory {

	private final RightClickOptionRepo rightClickOptionRepo;

	public ActionTableFactory(RightClickOptionRepo rightClickOptionRepo) {
		this.rightClickOptionRepo = rightClickOptionRepo;
	}

	public TableWithFilterAndDetails<ActionInfo, Object> table() {
		return TableWithFilterAndDetails.builder("Actions/Abilities", () -> {
					Map<Integer, ActionInfo> csvValues = ActionLibrary.getAll();
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
					if (icon == null || icon.isDefaultIcon()) {
						return "";
					}
					return icon;
				}, col -> {
					col.setCellRenderer(new DefaultTableCellRenderer() {
						@Override
						public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
							Component fallback = super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
							if (value instanceof HasIconURL hiu) {
								return IconTextRenderer.getComponent(hiu, fallback, false);
							}
							return fallback;
						}
					});
					col.setMinWidth(30);
					col.setMaxWidth(40);
				}))
				.addMainColumn(new CustomColumn<>("Description", ActionInfo::description, col -> {
					col.setPreferredWidth(500);
				}))
				.addMainColumn(new CustomColumn<>("Player Ability", ai -> ai.isPlayerAbility() ? "✓" : ""))
				.addMainColumn(new CustomColumn<>("Cast", ai -> {
					double ct = ai.getCastTime();
					if (ct == 0) {
						return "";
					}
					return ct;
				}))
				.addMainColumn(new CustomColumn<>("Recast", ai -> {
					int maxCharges = ai.maxCharges();
					double cd = ai.getCd();
					if (maxCharges > 1) {
						return String.format("%s (%d charges)", cd, maxCharges);
					}
					return cd > 0 ? cd : "";
				}))
				.addMainColumn(new CustomColumn<>("Range/Shape", Function.identity(),c -> {
					c.setCellRenderer(new DefaultTableCellRenderer() {
						@Override
						public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
							if (value instanceof ActionInfo ai) {
								Component comp = super.getTableCellRendererComponent(table, OmenShape.describe(ai), isSelected, hasFocus, row, column);
								RenderUtils.setTooltip(comp, "Raw: ct:%s, er:%sy, x:%sy".formatted(ai.castType(), ai.effectRange(), ai.xAxisModifier()));
								return comp;
							}
							return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
						}
					});
				}))
				.addFilter(t -> new IdOrNameFilter<>("Name/ID", ActionInfo::actionid, ActionInfo::name, t))
				.addWidget(InGameAbilityPickerButton::new)
				.addWidget(tbl -> JumpToIdWidget.create(tbl, ActionInfo::actionid))
				.withRightClickRepo(RightClickOptionRepo.of(
						CustomRightClickOption.forRow(
								"Copy XIVAPI Icon URL",
								ActionInfo.class,
								ai -> GuiUtil.copyTextToClipboard(ai.getXivapiUrl().toString())),
						CustomRightClickOption.forRow(
								"Copy XIVAPI Icon As Markdown",
								ActionInfo.class,
								ai -> {
									String md = String.format("![%s](%s)", ai.name(), ai.getXivapiUrl());
									GuiUtil.copyTextToClipboard(md);
								})))
//				.addRightClickOption(CustomRightClickOption.forRow("Copy XIVAPI Icon As Inline", ActionInfo.class, ai -> {
//					String md = String.format("{{< inline >}} ![%s](%s) {{< /inline >}}%s", ai.name(), ai.getXivapiUrl(), ai.name());
//					GuiUtil.copyToClipboard(md);
//				}))
				.withRightClickRepo(rightClickOptionRepo.withMore(
						CustomRightClickOption.forRow("Open on XivAPI", ActionInfo.class, ai -> {
							GuiUtil.openUrl(XivApiUtils.singleItemUrl("Action", ai.actionid()));
						})
				))
				.setFixedData(true)
				.build();
	}

	private class InGameAbilityPickerButton extends JButton {

		private final WeakReference<TableWithFilterAndDetails<ActionInfo, ?>> tableRef;

		InGameAbilityPickerButton(TableWithFilterAndDetails<ActionInfo, ?> table) {
			setToolTipText("Click this button, then use an ability (or get hit by it) in-game. The ability will then be selected in the table.");
			this.tableRef = new WeakReference<>(table);
			addActionListener(l -> {
				this.clicked();
			});
		}

		private boolean isActive() {
			return currentPicker == this;
		}

		private void clicked() {
			// I don't have to worry about thread safety because this always executes on the EDT
			if (isActive()) {
				setCurrentPicker(null);
			}
			else {
				setCurrentPicker(this);
			}
		}

		@Override
		public String getText() {
			return isActive() ? "Cancel" : "Use Ability In-Game";
		}

		public void feedAbility(AbilityUsedEvent aue) {
			TableWithFilterAndDetails<ActionInfo, ?> table = tableRef.get();
			if (table == null) {
				return;
			}
			ActionInfo ai = ActionLibrary.forId(aue.getAbility().getId());
			table.setAndScrollToSelection(ai);
		}


	}

	public void showChooser(Window owner, Consumer<ActionInfo> callback) {
		TableWithFilterAndDetails<ActionInfo, Object> table = table();
		ChooserDialog.showChooser(owner, table, callback);
	}

	public @Nullable ActionInfo pickItem(Window owner) {
		return ChooserDialog.chooserReturnItem(owner, table());
	}

	private void setCurrentPicker(InGameAbilityPickerButton picker) {
		InGameAbilityPickerButton oldPicker = currentPicker;
		currentPicker = picker;
		if (oldPicker != null) {
			SwingUtilities.invokeLater(oldPicker::repaint);
		}
		if (picker != null) {
			SwingUtilities.invokeLater(picker::repaint);
		}
	}

	private InGameAbilityPickerButton currentPicker;

	@HandleEvents
	public void onAction(EventContext context, AbilityUsedEvent aue) {
		InGameAbilityPickerButton picker = currentPicker;
		if (picker != null && (aue.getSource().isThePlayer() || aue.getTarget().isThePlayer())) {
			picker.feedAbility(aue);
			setCurrentPicker(null);
		}
	}
}
