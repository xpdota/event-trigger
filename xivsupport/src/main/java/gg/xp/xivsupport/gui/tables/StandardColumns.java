package gg.xp.xivsupport.gui.tables;

import gg.xp.xivsupport.gui.tables.renderers.HpRenderer;
import gg.xp.xivsupport.gui.tables.renderers.JobRenderer;
import gg.xp.xivsupport.gui.tables.renderers.MpRenderer;
import gg.xp.xivsupport.gui.tables.renderers.NameJobRenderer;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;

import java.lang.reflect.Field;
import java.util.Map;

public class StandardColumns {


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

	public static final CustomColumn<XivCombatant> hpColumn
			= new CustomColumn<>("HP", XivCombatant::getHp,
			c -> {
				c.setCellRenderer(new HpRenderer());
				c.setPreferredWidth(200);
			});

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
		c.setMinWidth(50);
		c.setMaxWidth(50);
	});

	public static final CustomColumn<XivCombatant> combatantRawTypeColumn
			= new CustomColumn<>("Type#", XivCombatant::getRawType,
			c -> {
				c.setMaxWidth(60);
				c.setMinWidth(60);
			});

	public static final CustomColumn<XivCombatant> posColumn
			= new CustomColumn<>("Position", XivCombatant::getPos);


	public static final CustomColumn<Map.Entry<Field, Object>> fieldName = (new CustomColumn<>("Field", e -> e.getKey().getName()));
	public static final CustomColumn<Map.Entry<Field, Object>> fieldValue = (new CustomColumn<>("Value", Map.Entry::getValue));
	public static final CustomColumn<Map.Entry<Field, Object>> fieldType = (new CustomColumn<>("Field Type", e -> e.getKey().getGenericType()));
	public static final CustomColumn<Map.Entry<Field, Object>> fieldDeclaredIn = (new CustomColumn<>("Declared In", e -> e.getKey().getDeclaringClass().getSimpleName()));

}
