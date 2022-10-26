package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class OverlayNameJobRenderer implements TableCellRenderer {
	private final DefaultTableCellRenderer fallback = new DefaultTableCellRenderer();

	private final ComponentListRenderer listRenderer;
	private final boolean transparent;
	private boolean showId;

	public OverlayNameJobRenderer() {
		this(false, false);
	}

	public OverlayNameJobRenderer(boolean transparent, boolean reversed) {
		this.transparent = transparent;
		listRenderer = new ComponentListRenderer(1, reversed);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (isSelected) {
			listRenderer.setBackground(table.getSelectionBackground());
		}
		else {
			listRenderer.setBackground(null);
		}
		final Component icon;
		final Component label;
		final String tooltip;
		if (value instanceof XivEntity entity) {
			String name = entity.getName();
			if (name == null) {
				name = String.format("Unknown 0x%X", entity.getId());
			}
			label = fallback.getTableCellRendererComponent(table, name, isSelected, false, row, column);
			Job job;
			if (value instanceof XivCombatant cbt) {
				HitPoints hp = cbt.getHp();
				String hpStr = hp == null ? "null" : hp.getShortString();
				long shieldAmount = cbt.getShieldAmount();
				String shieldStr = shieldAmount > 0 ? String.format(" +~%s shield", shieldAmount) : "";
				if (value instanceof XivPlayerCharacter player && (job = player.getJob()) != null) {
					icon = IconTextRenderer.getIconOnly(job);
					tooltip = (String.format("%s - %s (0x%x, %s)%n%nHP: (%s)%s%n%s", player.getName(), player.getJob(), player.getId(), player.getId(), hpStr, shieldStr, cbt.getPos()));
				}
				else {
					tooltip = String.format("%s (0x%x, %s)%n%nHP: (%s)%s%n%s%nNPC ID %s, name %s", cbt.getName(), cbt.getId(), cbt.getId(), hpStr, shieldStr, cbt.getPos(), cbt.getbNpcId(), cbt.getbNpcNameId());
					icon = null;
				}
			}
			else {
				tooltip = String.format("%s (0x%x, %s)", entity.getName(), entity.getId(), entity.getId());
				icon = null;
			}
		}
		else if (value == null) {
			tooltip = null;
			label = null;
			icon = null;
		}
		else {
			tooltip = null;
			label = fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			icon = null;
		}
		if (transparent && label instanceof JComponent jc) {
			jc.setOpaque(false);
		}
		List<Component> components = new ArrayList<>(2);
		if (icon != null) {
			components.add(icon);
		}
		if (label != null) {
			components.add(label);
		}
		listRenderer.setComponents(components);
		listRenderer.setToolTipText(tooltip);
		return listRenderer;
	}

	public boolean isShowId() {
		return showId;
	}

	public void setShowId(boolean showId) {
		this.showId = showId;
	}
}
