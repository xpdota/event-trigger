package gg.xp.xivsupport.gui.tables.renderers;

import java.awt.*;

public class EmptyRenderer extends Component {
	private Color bg;

	public void reset() {
		bg = null;
	}

	@Override
	public void setBackground(Color bg) {
		this.bg = bg;
	}

	@Override
	public void paint(Graphics g) {
		if (bg != null) {
			Rectangle bounds = getBounds();
			int cellHeight = bounds.height;
			int cellWidth = bounds.width;
			g.setColor(bg);
			g.fillRect(0, 0, cellWidth, cellHeight);
		}
	}


	@Override
	public boolean isOpaque() {
		return getBackground() != null;
	}

	@Override
	public void validate() {
//		super.validate();
	}

	@Override
	public void revalidate() {
//		super.revalidate();
	}

	@Override
	public void invalidate() {
//		super.invalidate();
	}


}
