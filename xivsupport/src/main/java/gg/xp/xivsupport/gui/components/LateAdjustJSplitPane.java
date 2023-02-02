package gg.xp.xivsupport.gui.components;

import javax.swing.*;
import java.awt.*;

/**
 * JSplitPane but fixes an annoyance where setting the divider location proportion (e.g. 0.5 to have it be an even
 * split) doesn't work if the component is not visible yet because the dimensions are still 0,0.
 */
public class LateAdjustJSplitPane extends JSplitPane {

	public LateAdjustJSplitPane() {
	}

	public LateAdjustJSplitPane(int newOrientation) {
		super(newOrientation);
	}

	public LateAdjustJSplitPane(int newOrientation, boolean newContinuousLayout) {
		super(newOrientation, newContinuousLayout);
	}

	public LateAdjustJSplitPane(int newOrientation, Component newLeftComponent, Component newRightComponent) {
		super(newOrientation, newLeftComponent, newRightComponent);
	}

	public LateAdjustJSplitPane(int newOrientation, boolean newContinuousLayout, Component newLeftComponent, Component newRightComponent) {
		super(newOrientation, newContinuousLayout, newLeftComponent, newRightComponent);
	}

	private Double locationOverride;

	@Override
	public void setDividerLocation(double proportionalLocation) {
		super.setDividerLocation(proportionalLocation);
		locationOverride = proportionalLocation;
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		Dimension oldSize = getSize();
		boolean adjust = (oldSize.width == 0 || oldSize.height == 0) && (width > 0 && height > 0);
		super.setBounds(x, y, width, height);
		if (adjust) {
			if (locationOverride != null) {
				setDividerLocation(locationOverride);
			}
		}
	}
}
