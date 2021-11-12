package gg.xp.events.models;

import java.io.Serializable;

public class XivCombatant extends XivEntity implements Serializable {

	private final boolean isPc;
	private final boolean isThePlayer;
	// TODO: location/heading
	// TODO: hp info

	public XivCombatant(long id, String name, boolean isPc, boolean isThePlayer) {
		super(id, name);
		this.isPc = isPc;
		this.isThePlayer = isThePlayer;
	}

	public boolean isPc() {
		return isPc;
	}

	public boolean isThePlayer() {
		return isThePlayer;
	}

	@Override
	public String toString() {
		if (isEnvironment()) {
			return super.toString();
		}
		return String.format("XivCombatant(0x%X:%s)", getId(), getName());
	}
}
