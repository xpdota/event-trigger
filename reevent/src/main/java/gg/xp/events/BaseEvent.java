package gg.xp.events;

import org.jetbrains.annotations.Nullable;

public abstract class BaseEvent implements Event {

	private static final long serialVersionUID = 6147224373832437718L;
	private Event parent;
	
	public void setParent(Event parent) {
		if (this.parent != null) {
			throw new IllegalStateException("Event already has a parent");
		}
		this.parent = parent;
	}

	@Override
	public @Nullable Event getParent() {
		return parent;
	}
}
