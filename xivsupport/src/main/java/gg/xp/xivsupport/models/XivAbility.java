package gg.xp.xivsupport.models;

import gg.xp.xivdata.data.ActionInfo;
import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivsupport.events.actlines.events.NameIdPair;

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
		ActionInfo actionInfo = ActionLibrary.forId(id);
		if (actionInfo == null) {
			name = String.format("Unknown_%x", id);
		}
		else {
			name = actionInfo.name();
		}
	}

	public XivAbility(long id, String name) {
		this.id = id;
		this.name = name;
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
