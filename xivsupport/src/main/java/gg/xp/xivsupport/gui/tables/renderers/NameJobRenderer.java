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

public class NameJobRenderer implements TableCellRenderer {
	private final DefaultTableCellRenderer fallback = new DefaultTableCellRenderer();

	private final IconNameIdRenderer renderer = new IconNameIdRenderer();
	private boolean showId;
	{
		renderer.setIdAlpha(128);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		Component defaultComponent = fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		final HasIconURL icon;
		final String text;
		final String idText;
		final String tooltip;
		if (value instanceof XivEntity entity) {
			String name = entity.getName();
			idText = String.format("%X", entity.getId());
			Job job;
			if (value instanceof XivCombatant cbt) {
				if (name == null || name.isBlank()) {
					text = String.format("No Name (%s:%s)", cbt.getbNpcId(), cbt.getbNpcNameId());
				}
				else {
					text = name;
				}
				HitPoints hp = cbt.getHp();
				String hpStr = hp == null ? "null" : hp.getShortString();
				long shieldAmount = cbt.getShieldAmount();
				String shieldStr = shieldAmount > 0 ? String.format(" +~%s shield", shieldAmount) : "";
				if (value instanceof XivPlayerCharacter player && (job = player.getJob()) != null) {
					icon = (job);
					tooltip = (String.format("%s - %s (0x%x, %s)%n%nHP: (%s)%s%n%s", player.getName(), player.getJob(), player.getId(), player.getId(), hpStr, shieldStr, cbt.getPos()));
				}
				else {
					tooltip = String.format("%s (0x%x, %s)%n%nHP: (%s)%s%n%s%nNPC ID %s, name %s", cbt.getName(), cbt.getId(), cbt.getId(), hpStr, shieldStr, cbt.getPos(), cbt.getbNpcId(), cbt.getbNpcNameId());
					icon = null;
				}
			}
			else {
				if (name == null) {
					name = String.format("Unknown 0x%X", entity.getId());
				}
				text = name;
				tooltip = String.format("%s (0x%x, %s)", entity.getName(), entity.getId(), entity.getId());
				icon = null;
			}
		}
		else if (value == null) {
			tooltip = null;
			text = null;
			icon = null;
			idText = null;
		}
		else {
			return defaultComponent;
		}
		renderer.reset();
		renderer.setMainText(text);
		renderer.setIdText(showId ? idText : null);
		renderer.setIcon(icon);
		renderer.setToolTipText(tooltip);
		renderer.formatFrom(defaultComponent);
		return renderer;
	}

	public boolean isShowId() {
		return showId;
	}

	public void setShowId(boolean showId) {
		this.showId = showId;
	}
}
