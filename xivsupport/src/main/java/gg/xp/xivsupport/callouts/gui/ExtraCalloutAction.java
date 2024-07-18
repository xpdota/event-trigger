package gg.xp.xivsupport.callouts.gui;

import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;

import javax.annotation.Nullable;
import javax.swing.*;

public interface ExtraCalloutAction {

	@Nullable ExtraCalloutActionInstance getInstance(ModifiableCallout<?> callout, ModifiedCalloutHandle handle);

}
