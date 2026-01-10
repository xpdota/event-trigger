package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.events.actlines.events.HasTargetIndex;

import java.awt.*;

public class AbilityHitIndexIndicator extends Component {

	private boolean isFirst;
	private boolean isLast;

	@Override
	public void paint(Graphics g) {
		if (g instanceof Graphics2D g2d) {
			g2d.setColor(RenderUtils.withAlpha(getForeground(), 128));
			int w = getWidth();
			int h = getHeight();
			if (isFirst) {
				g.drawLine(1, 1, w, 1);
				g.drawLine(0, 1, 0, h - 1);
			}
			else if (isLast) {
				g.drawLine(1, h - 3, w, h - 3);
				g.drawLine(0, 0, 0, h - 3);
			}
			else {
				g.drawLine(0, 0, 0, h - 1);
			}
		}
	}

	public void setValue(HasTargetIndex hti) {
		isFirst = hti.isFirstTarget();
		isLast = hti.isLastTarget();
	}
}
