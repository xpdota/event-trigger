package gg.xp.xivsupport.timelines.gui;

import gg.xp.xivdata.data.URLIcon;
import gg.xp.xivsupport.gui.tables.renderers.EmptyRenderer;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.TimelineBar;
import gg.xp.xivsupport.timelines.VisualTimelineEntry;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.net.URL;

public class TimelineBarRenderer implements TableCellRenderer {

	public static final Color defaultColorActive = new Color(87, 149, 16, 192);
	public static final Color defaultColorExpired = new Color(128, 0, 128, 192);
	public static final Color defaultColorUpcoming = new Color(53, 134, 159, 192);
	public static final Color defaultFontColor = new Color(240, 240, 240);
	private final TimelineBar bar = new TimelineBar();
	private final TableCellRenderer fallback = new DefaultTableCellRenderer();
	private final TimelineBarColorProvider colorProvider;

	public TimelineBarRenderer(TimelineBarColorProvider colorProvider) {
		this.colorProvider = colorProvider;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof VisualTimelineEntry entry) {
			Component baseLabel = fallback.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
			double percent;
			long actualMax = entry.max();
			long actualCurrent = entry.current();
			int effectiveMax;
			int effectiveCurrent;
			if (actualMax == 0 || actualMax < actualCurrent || actualMax == 1) {
				return emptyComponent(isSelected, table);
			}
			else {
				effectiveMax = (int) actualMax;
				effectiveCurrent = (int) actualCurrent;
			}
			percent = effectiveCurrent / (double) effectiveMax;


			Color barColor = getBarColor(percent, entry);
			bar.setColor1(barColor);
			Color originalBg = baseLabel.getBackground();
			bar.setColor3(new Color(originalBg.getRed(), originalBg.getGreen(), originalBg.getBlue(), 128));
			bar.setBorderColor(originalBg);


			bar.setPercent1(percent);
			bar.setTextColor(colorProvider.getFontColor());

			formatLabel(entry);

			return bar;
		}
		return emptyComponent(isSelected, table);

	}

	protected void formatLabel(@NotNull VisualTimelineEntry item) {
		double active = item.remainingActiveTime();
		bar.setLeftTextOptions(String.format("%s%s", item.originalTimelineEntry().name(), item.isCurrentSync() ? "*" : ""));
		bar.setRightText(String.format("%.1f", active > 0 ? active : item.timeUntil()));
		URL url = item.originalTimelineEntry().icon();
		if (url == null) {
			bar.seticon(null);
		}
		else {
			bar.seticon(IconTextRenderer.getIconOnly(new URLIcon(url)));
		}
	}

	protected Color getBarColor(double percent, @NotNull VisualTimelineEntry item) {
		if (item.remainingActiveTime() > 0) {
			return colorProvider.getActiveColor();
		}
		if (percent < 0.999d) {
			return colorProvider.getUpcomingColor();
		}
		return colorProvider.getExpiredColor();
	}

	private final EmptyRenderer empty = new EmptyRenderer();

	private Component emptyComponent(boolean isSelected, JTable table) {
		if (isSelected) {
			empty.setBackground(table.getSelectionBackground());
		}
		else {
			empty.setBackground(null);
		}
		return empty;
	}

}
