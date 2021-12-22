package gg.xp.xivsupport.models;

import gg.xp.xivsupport.events.actlines.events.NameIdPair;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class XivEntity implements Serializable, NameIdPair {

	@Serial
	private static final long serialVersionUID = 1282314870448740356L;
	// IMPORTANT: Annoyingly, these all must be 'long' instead of 'int' because the game treats them as
	// unsigned 32-bit, but Java treats them as signed, so values above 7FFFFFFF cause an overflow
	private final long id;
	private final String name;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		XivEntity xivEntity = (XivEntity) o;
		return id == xivEntity.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	public XivEntity(long id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
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
