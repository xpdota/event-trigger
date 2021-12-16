package gg.xp.xivsupport.gui.tables;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.actionresolution.SequenceIdTracker;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.triggers.jobs.StatusEffectRepository;
import gg.xp.xivsupport.gui.GuiMain;
import gg.xp.xivsupport.gui.tables.renderers.HpPredictedRenderer;
import gg.xp.xivsupport.gui.tables.renderers.HpRenderer;
import gg.xp.xivsupport.gui.tables.renderers.JobRenderer;
import gg.xp.xivsupport.gui.tables.renderers.MpRenderer;
import gg.xp.xivsupport.gui.tables.renderers.NameJobRenderer;
import gg.xp.xivsupport.gui.tables.renderers.StatusEffectsRenderer;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.HitPointsWithPredicted;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import jdk.jshell.PersistentSnippet;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ScanMe
public final class StandardColumns {

	private final BooleanSetting showPredictedHp;
	private final StatusEffectRepository statuses;
	private final SequenceIdTracker sqidTracker;

	public StandardColumns(PersistenceProvider persist, StatusEffectRepository statuses, SequenceIdTracker sqidTracker) {
		showPredictedHp = new BooleanSetting(persist, "gui.display-predicted-hp", false);
		this.statuses = statuses;
		this.sqidTracker = sqidTracker;
	}

	public static final CustomColumn<XivEntity> entityIdColumn
			= new CustomColumn<>("ID", c -> "0x" + Long.toString(c.getId(), 16),
			c -> {
				c.setMinWidth(80);
				c.setMaxWidth(80);
			}
	);

	public static final CustomColumn<XivCombatant> nameJobColumn
			= new CustomColumn<>("Name", c -> c, c -> {
		c.setCellRenderer(new NameJobRenderer());
		c.setPreferredWidth(150);
	});

	public static final CustomColumn<XivPlayerCharacter> jobOnlyColumn
			= new CustomColumn<>("Job", XivPlayerCharacter::getJob,
			c -> {
				c.setCellRenderer(new JobRenderer());
				c.setMinWidth(60);
				c.setMaxWidth(60);
			});

	public static final CustomColumn<XivCombatant> parentNameJobColumn
			= new CustomColumn<>("Parent", XivCombatant::getParent, c -> {
		c.setCellRenderer(new NameJobRenderer());
		c.setPreferredWidth(100);
	});

	public CustomColumn<XivCombatant> statusEffectsColumn() {
		return new CustomColumn<>("Statuses", entity -> statuses.statusesOnTarget(entity).stream().map(BuffApplied::getBuff).collect(Collectors.toList()), c -> {
			c.setCellRenderer(new StatusEffectsRenderer());
			c.setPreferredWidth(300);
		});
	}


	public static final CustomColumn<XivCombatant> hpColumn
			= new CustomColumn<>("HP", XivCombatant::getHp,
			c -> {
				c.setCellRenderer(new HpRenderer());
				c.setPreferredWidth(200);
			});

	public CustomColumn<XivCombatant> hpColumnWithUnresolved() {
		return new CustomColumn<>("HP", combatant -> {
			HitPoints realHp = combatant.getHp();
			if (realHp == null) {
				return null;
			}
			long pending;
			if (showPredictedHp.get()) {
				List<AbilityUsedEvent> events = sqidTracker.getEventsTargetedOnEntity(combatant);
				pending = events.stream().mapToLong(AbilityUsedEvent::getDamage).sum();
			}
			else {
				pending = 0;
			}
			return new HitPointsWithPredicted(realHp.getCurrent(), Math.max(realHp.getCurrent() - pending, 0), realHp.getMax());
		}, c ->
		{
			c.setCellRenderer(new HpPredictedRenderer());
			c.setPreferredWidth(200);
		});
	}

	public static final CustomColumn<XivCombatant> mpColumn
			= new CustomColumn<>("MP", xivCombatant -> {
		if (xivCombatant instanceof XivPlayerCharacter) {
			if (((XivPlayerCharacter) xivCombatant).getJob().isCombatJob()) {
				return xivCombatant.getMp();
			}
		}
		return null;
	},
			c -> {
				c.setCellRenderer(new MpRenderer());
				c.setPreferredWidth(100);
			});

	public static final CustomColumn<XivCombatant> combatantTypeColumn
			= new CustomColumn<>("Type",
			c -> {
				if (c.isThePlayer()) {
					return "YOU";
				}
				else {
					return c.getType();
				}
			}, c -> {
		c.setMinWidth(80);
		c.setMaxWidth(80);
	});

	public static final CustomColumn<XivCombatant> combatantRawTypeColumn
			= new CustomColumn<>("Type#", XivCombatant::getRawType,
			c -> {
				c.setMaxWidth(60);
				c.setMinWidth(60);
			});

	public static final CustomColumn<XivCombatant> posColumn
			= new CustomColumn<>("Position", XivCombatant::getPos);


	public static final CustomColumn<Map.Entry<Field, Object>> fieldName
			= new CustomColumn<>("Field", e -> e.getKey().getName());
	public static final CustomColumn<Map.Entry<Field, Object>> fieldValue
			= new CustomColumn<>("Value", fieldObjectEntry -> {

		Object value = fieldObjectEntry.getValue();
		if (value instanceof Long || value instanceof Integer) {
			return String.format("0x%x (%d)", value, value);
		}
		return value;
	});
	public static final CustomColumn<Map.Entry<Field, Object>> identity
			= new CustomColumn<>("Identity", e -> {
		if (e.getKey().getType().isPrimitive()) {
			return "(primitive)";
		}
		return "0x" + Integer.toString(System.identityHashCode(e.getValue()), 16);
	});
	public static final CustomColumn<Map.Entry<Field, Object>> fieldType
			= new CustomColumn<>("Field Type", e -> e.getKey().getGenericType());
	public static final CustomColumn<Map.Entry<Field, Object>> fieldDeclaredIn
			= new CustomColumn<>("Declared In", e -> e.getKey().getDeclaringClass().getSimpleName());


	public BooleanSetting getShowPredictedHp() {
		return showPredictedHp;
	}

	public static <X> CustomColumn<X> booleanSettingColumn(String name, Function<X, BooleanSetting> settingGetter) {
		return new CustomColumn<>(name, settingGetter::apply, col -> {
			col.setMaxWidth(20);
			col.setMinWidth(20);
			col.setCellRenderer(new TableCellRenderer() {
				private final DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();

				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					if (value instanceof BooleanSetting) {
						JCheckBox cb = new JCheckBox();
						cb.setOpaque(false);
						cb.setSelected(((BooleanSetting) value).get());
						cb.setBackground(defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column).getBackground());
						return cb;
					}
					else {
						return null;
					}
				}
			});
			col.setCellEditor(new BooleanSettingCellEditor());
		});
	}


	private static class BooleanSettingCellEditor extends AbstractCellEditor implements TableCellEditor {

		@Override
		public Object getCellEditorValue() {
			return null;
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			return new BooleanSettingGui((BooleanSetting) value, null).getComponent();
		}
	}
}
