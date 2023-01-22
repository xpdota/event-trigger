package gg.xp.xivsupport.models;

import gg.xp.xivdata.data.ActionInfo;
import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivdata.data.rsv.*;
import gg.xp.xivsupport.events.actlines.events.NameIdPair;
import gg.xp.xivsupport.util.RsvLookupUtil;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class XivAbility implements Serializable, NameIdPair {
	@Serial
	private static final long serialVersionUID = -6170494857373031360L;
	private final long id;
	private final String name;

	public XivAbility(long id) {
		this.id = id;
		this.name = findName(id, null);
	}

	public XivAbility(long id, String name) {
		this.id = id;
		this.name = findName(id, name);
	}

	private static String findName(long id, @Nullable String givenName) {
		return RsvLookupUtil.lookup(id, givenName, ActionLibrary::forId, ActionInfo::name);
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
		return String.format("XivAbility(0x%X:%s)", id, name);
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		XivAbility ability = (XivAbility) o;
		return id == ability.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
