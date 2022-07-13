package gg.xp.xivsupport.cdsupport;

import gg.xp.xivdata.data.CdBuilder;
import gg.xp.xivdata.data.CooldownType;
import gg.xp.xivdata.data.ExtendedCooldownDescriptor;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

public class CustomCooldown {

	public @Nullable String nameOverride;
	public long primaryAbilityId;
	public long[] secondaryAbilityIds = {};
	public long[] buffIds = {};
	public boolean autoBuffs = true;
	public @Nullable Integer maxCharges;
	public @Nullable Double cooldown;
	public @Nullable Double duration;
	public CooldownType type;

	private ExtendedCooldownDescriptor cached;

	public ExtendedCooldownDescriptor buildCd() {
		// Not technically thread safe but shouldn't realistically be a problem
		if (cached != null) {
			return cached;
		}
		// TODO: type
		long[] primaryAbilityArray = {primaryAbilityId};
		long[] abilityIds = ArrayUtils.addAll(primaryAbilityArray, secondaryAbilityIds);
		CdBuilder builder = new CdBuilder(CooldownType.UNCATEGORIZED, true, abilityIds);
		if (!autoBuffs) {
			builder.noAutoBuffs();
			builder.buffIds(buffIds);
		}
		if (maxCharges != null) {
			builder.maxCharges(maxCharges);
		}
		if (cooldown != null) {
			builder.cooldown(cooldown);
		}
		if (duration != null) {
			builder.duration(duration);
		}
		if (nameOverride != null) {
			builder.name(nameOverride);
		}
		return builder.build();
	}

	public void invalidate() {
		cached = null;
	}

	;
}
