package gg.xp.services;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public interface ServiceHandle extends HasFriendlyName {

	boolean enabled();

	void setEnabled();

	ServiceDescriptor descriptor();

	@Override
	default String getFriendlyName() {
		return descriptor().getFriendlyName();
	};
}
