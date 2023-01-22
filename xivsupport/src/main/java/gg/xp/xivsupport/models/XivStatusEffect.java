package gg.xp.xivsupport.models;

import gg.xp.xivdata.data.*;
import gg.xp.xivdata.data.rsv.*;
import gg.xp.xivsupport.events.actlines.events.NameIdPair;
import gg.xp.xivsupport.util.RsvLookupUtil;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class XivStatusEffect implements Serializable, NameIdPair {
	@Serial
	private static final long serialVersionUID = -408717295208496811L;
	private final long id;
	private final String name;

	// TODO: worth caching these?
	public XivStatusEffect(long id) {
		this.id = id;
		this.name = findName(id, null);
	}

	public XivStatusEffect(long id, String name) {
		this.id = id;
		this.name = findName(id, name);
	}

	private static String findName(long id, @Nullable String givenName) {
		return RsvLookupUtil.lookup(id, givenName, StatusEffectLibrary::forId, StatusEffectInfo::name);
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
