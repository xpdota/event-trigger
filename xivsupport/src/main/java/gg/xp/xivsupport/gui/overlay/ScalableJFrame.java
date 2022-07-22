package gg.xp.xivsupport.gui.overlay;

import javax.swing.*;
import java.awt.*;

public abstract class ScalableJFrame extends JFrame implements Scaled {
	public ScalableJFrame(String title) throws HeadlessException {
		super(title);
	}

	public abstract void setScaleFactor(double scaleFactor);

	@Override
	public abstract double getScaleFactor();

	public abstract void setClickThrough(boolean clickThrough);
}
