package gg.xp.xivsupport.callouts.gui;

import gg.xp.xivsupport.callouts.ModifiableCallout;

public interface ExtraCalloutActionInstance {
	String getLabel();

	boolean isVisible();
	boolean isEnabled();

	void doAction();
}
