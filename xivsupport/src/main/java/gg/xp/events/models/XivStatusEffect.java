package gg.xp.events.models;

import java.io.Serializable;

public class XivStatusEffect implements Serializable {
	private static final long serialVersionUID = -408717295208496811L;
	private final long id;
	private final String name;

	public XivStatusEffect(long id, String name) {
		this.id = id;
		this.name = name;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return String.format("XivStatusEffect(0x%X:%s)", id, name);
	}

}
