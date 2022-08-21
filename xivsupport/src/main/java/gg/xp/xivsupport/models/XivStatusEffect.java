package gg.xp.xivsupport.models;

import gg.xp.xivdata.data.StatusEffectInfo;
import gg.xp.xivdata.data.StatusEffectLibrary;
import gg.xp.xivsupport.events.actlines.events.NameIdPair;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class XivStatusEffect implements Serializable, NameIdPair {
	@Serial
	private static final long serialVersionUID = -408717295208496811L;
	private final long id;
	private final String name;

	public XivStatusEffect(long id) {
		this.id = id;
		StatusEffectInfo statusInfo = StatusEffectLibrary.forId(id);
		if (statusInfo == null) {
			name = String.format("Unknown_%x", id);
		}
		else {
			name = statusInfo.name();
		}
	}

	public XivStatusEffect(long id, String name) {
		this.id = id;
		this.name = name == null ? null : name.intern();
	}

	public @Nullable StatusEffectInfo getInfo() {
		return StatusEffectLibrary.forId(id);
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return String.format("XivStatusEffect(0x%X:%s)", id, name);
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		XivStatusEffect other = (XivStatusEffect) o;
		return id == other.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
