package gg.xp.services;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public interface ServiceDescriptor extends HasFriendlyName {

	String name();

	String id();

	default int priority() {
		return 0;
	}

	@Override
	default String getFriendlyName() {
		return name();
	}

	static ServiceDescriptor of(String id, String name) {
		return of(id, name, 0);
	}

	static ServiceDescriptor of(String id, String name, int prio) {
		return new ServiceDescriptor() {
			@Override
			public String name() {
				return name;
			}

			@Override
			public String id() {
				return id;
			}

			@Override
			public int priority() {
				return prio;
			}
		};
	}
}
