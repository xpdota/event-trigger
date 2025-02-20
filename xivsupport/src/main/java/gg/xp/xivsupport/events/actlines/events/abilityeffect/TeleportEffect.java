package gg.xp.xivsupport.events.actlines.events.abilityeffect;

import gg.xp.xivdata.data.*;
import org.jetbrains.annotations.Nullable;

public class TeleportEffect extends AbilityEffect {
	private final long zoneId;

	public TeleportEffect(long flags, long value) {
		super(flags, value, AbilityEffectType.TELEPORT);
		this.zoneId = value >> 16;
	}

	public @Nullable ZoneInfo getZone() {
		return ZoneLibrary.infoForZone((int) this.zoneId);
	}

	@Override
	protected String getBaseDescription() {
		String zoneName = ZoneLibrary.nameForZone((int) this.zoneId);
		return "Teleport to " + (zoneName == null ? "unknown zone" : zoneName);
	}

	@Override
	public String toString() {
		String zoneName = ZoneLibrary.nameForZone((int) this.zoneId);
		return "Teleport(0x%X: %s)".formatted(zoneId, zoneName == null ? "unknown" : zoneName);
	}
}
