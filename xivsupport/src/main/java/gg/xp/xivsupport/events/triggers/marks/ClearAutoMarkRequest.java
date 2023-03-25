package gg.xp.xivsupport.events.triggers.marks;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.services.Handleable;

import java.io.Serial;

public class ClearAutoMarkRequest extends BaseEvent implements Handleable {
	@Serial
	private static final long serialVersionUID = 3326915962958188606L;
	private transient boolean handled;

	@Override
	public boolean isHandled() {
		return handled;
	}

	@Override
	public void setHandled() {
		handled = true;
	}
}
