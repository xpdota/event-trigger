package gg.xp.xivsupport.gui.library;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.DamageAspect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.DamageType;
import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.gui.map.omen.OmenShape;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomRightClickOption;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.BooleanFilter;
import gg.xp.xivsupport.gui.tables.filters.GroovyFilter;
import gg.xp.xivsupport.gui.tables.filters.IdFilter;
import gg.xp.xivsupport.gui.tables.filters.TextBasedFilter;
import gg.xp.xivsupport.gui.tables.groovy.GroovyColumns;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.RenderUtils;
import gg.xp.xivsupport.gui.util.GuiUtil;
import groovy.lang.PropertyValue;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;

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

public final class ActionTableFactory {

	private final RightClickOptionRepo rightClickOptionRepo;
	private final PicoContainer container;

	public ActionTableFactory(RightClickOptionRepo rightClickOptionRepo, PicoContainer container) {
		this.rightClickOptionRepo = rightClickOptionRepo;
		this.container = container;
	}

	private static final CustomColumn<ActionInfo> idCol = new CustomColumn<ActionInfo>("ID", v -> String.format("0x%X (%s)", v.actionid(), v.actionid()), col -> {
		col.setMinWidth(100);
		col.setMaxWidth(100);
	}).withFilter(t -> new IdFilter<>(t, "ID", ActionInfo::actionid));

	private static final CustomColumn<ActionInfo> nameCol = new CustomColumn<>("Name", ActionInfo::name, col -> {
		col.setPreferredWidth(200);
	}).withFilter(t -> new TextBasedFilter<>(t, "Name", ActionInfo::name));

	private static final CustomColumn<ActionInfo> iconCol = new CustomColumn<>("Icon", ai -> {
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
	});

	private static final CustomColumn<ActionInfo> descriptionCol = new CustomColumn<>("Description", ActionInfo::description, col -> {
		col.setPreferredWidth(500);
	}).withFilter(t -> new TextBasedFilter<>(t, "Description", ActionInfo::description));

	private static final CustomColumn<ActionInfo> playerAbilityCol = new CustomColumn<ActionInfo>("Player Action", ai -> ai.isPlayerAbility() ? "✓" : "")
			.withFilter(t -> new BooleanFilter<>(t, "Player Action", ActionInfo::isPlayerAbility));

	private static final CustomColumn<ActionInfo> castCol = new CustomColumn<>("Cast", ai -> {
		// In the game files, cast times are represented as 100ms units, so we never need more than
		// one decimal place.
		double ct = ai.getCastTime();
		if (ct == 0) {
			return "";
		}
		double extra = ai.getExtraCastTime();
		if (extra > 0) {
			return "%.1f + %.1f".formatted(ct, extra);
		}
		return "%.1f".formatted(ct);
	});

	private static final CustomColumn<ActionInfo> recastCol = new CustomColumn<>("Recast", ai -> {
		int maxCharges = ai.maxCharges();
		double cd = ai.getCd();
		if (maxCharges > 1) {
			return String.format("%s (%d charges)", cd, maxCharges);
		}
		return cd > 0 ? cd : "";
	});

	private static final CustomColumn<ActionInfo> categoryCol = new CustomColumn<ActionInfo>("Category", ai -> ai.getActionCategory().getFriendlyName(), col -> {
		col.setPreferredWidth(100);
	}).withFilter(t -> new TextBasedFilter<>(t, "Category", ai -> ai.getActionCategory().getFriendlyName()));

	private static final CustomColumn<ActionInfo> damageTypeAndAspectCol = new CustomColumn<ActionInfo>("Dmg Type/Aspect", ai -> ai, c -> {
		c.setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				if (value instanceof ActionInfo ai) {
					int dtRaw = ai.attackTypeRaw();
					int daRaw = ai.aspectRaw();
					if (daRaw == 0 && dtRaw == 0) {
						return super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
					}
					DamageType dt = DamageType.forByte(dtRaw);
					DamageAspect da = DamageAspect.forByte(daRaw);
					if (dt == DamageType.WeaponOverride && da == DamageAspect.Unaspected) {
						// Special case for weaponskills
						return super.getTableCellRendererComponent(table, "Weapon-Based", isSelected, hasFocus, row, column);
					}
					if (dt == DamageType.Unknown && da == DamageAspect.Unknown) {
						return super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
					}
					String text = String.format("%s %s (%s:%s)", da, dt, daRaw, dtRaw);
					Component comp = super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
					if (dt == DamageType.Unknown) {
						return comp;
					}
					HasIconURL icon = switch (dt) {
						case Piercing, Slashing, Blunt, Shot -> GeneralIcons.DAMAGE_PHYS;
						case Magic -> GeneralIcons.DAMAGE_MAGIC;
						default -> GeneralIcons.DAMAGE_OTHER;
					};
					return IconTextRenderer.getComponent(icon, comp, false);
				}
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		});
	});

	private static final CustomColumn<ActionInfo> rangeShapeCol = new CustomColumn<ActionInfo>("Range/Shape", v -> v, c -> {
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
	});

	private void applyCommon(TableWithFilterAndDetails.TableWithFilterAndDetailsBuilder<ActionInfo, ?> builder) {
		builder.addMainColumn(idCol)
				.addMainColumn(nameCol)
				.addMainColumn(iconCol)
				.addMainColumn(descriptionCol)
				.addMainColumn(playerAbilityCol)
				.addMainColumn(castCol)
				.addMainColumn(recastCol)
				.addMainColumn(categoryCol)
				.addMainColumn(damageTypeAndAspectCol)
				.addMainColumn(rangeShapeCol)
				.addWidget(InGameAbilityPickerButton::new)
				.addWidget(tbl -> JumpToIdWidget.create(tbl, ActionInfo::actionid))
				.addFilter(GroovyFilter.forClass(ActionInfo.class, container.getComponent(GroovyManager.class), "it"))
				.withRightClickRepo(rightClickOptionRepo.withMore(
						CustomRightClickOption.forRow("Open on XivAPI", ActionInfo.class, XivApiUtils.singleItemUrlOpener("Action", ActionInfo::actionid)),
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
								}),
						CustomRightClickOption.forRow("Open on Tomestone", ActionInfo.class, TomestoneUtils.singleItemUrlOpener("action", ActionInfo::actionid))
				))
				.setFixedData(true);
	}

	private static List<ActionInfo> getAllActions() {
		Map<Integer, ActionInfo> csvValues = ActionLibrary.getAll();
		List<ActionInfo> values = new ArrayList<>(csvValues.values());
		values.sort(Comparator.comparing(ActionInfo::actionid));
		return values;
	}

	public TableWithFilterAndDetails<ActionInfo, Object> table() {
		TableWithFilterAndDetails.TableWithFilterAndDetailsBuilder<ActionInfo, Object> builder = TableWithFilterAndDetails.<ActionInfo, Object>builder("Actions/Abilities", ActionTableFactory::getAllActions, unused -> Collections.emptyList());
		applyCommon(builder);
		return builder.build();
	}

	public TableWithFilterAndDetails<ActionInfo, PropertyValue> tableWithDetails() {
		TableWithFilterAndDetails.TableWithFilterAndDetailsBuilder<ActionInfo, PropertyValue> builder = TableWithFilterAndDetails.<ActionInfo, PropertyValue>builder("Actions/Abilities", ActionTableFactory::getAllActions, GroovyColumns::getValues);
		applyCommon(builder);
		return builder.apply(GroovyColumns::addDetailColumns)
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
