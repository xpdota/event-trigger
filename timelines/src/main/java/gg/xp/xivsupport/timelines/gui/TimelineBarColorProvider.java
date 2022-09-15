package gg.xp.xivsupport.timelines.gui;

import java.awt.*;

public interface TimelineBarColorProvider {
	Color getFontColor();
	Color getUpcomingColor();
	Color getActiveColor();
	Color getExpiredColor();
}
