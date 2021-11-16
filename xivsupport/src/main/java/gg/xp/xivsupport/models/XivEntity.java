package gg.xp.xivsupport.models;

import gg.xp.xivsupport.events.actlines.events.NameIdPair;

import java.io.Serializable;

public class XivEntity implements Serializable , NameIdPair {

	private static final long serialVersionUID = 1282314870448740356L;
	// IMPORTANT: Annoyingly, these all must be 'long' instead of 'int' because the game treats them as
	// unsigned 32-bit, but Java treats them as signed, so values above 7FFFFFFF cause an overflow
	private final long id;
	private final String name;

	public XivEntity(long id, String name) {
		this.id = id;
		this.name = name;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		if (isEnvironment()) {
			return "ENVIRONMENT";
		}
		return name;
	}

	@Override
	public String toString() {
		if (isEnvironment()) {
			return "XivEntity(ENVIRONMENT)";
		}
		return String.format("XivEntity(0x%X:%s)", id, name);
	}

	public boolean isEnvironment() {
		return id == 0xE0000000L;
	}
}
