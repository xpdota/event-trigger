package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

public interface ExtendedCooldownDescriptor extends BasicCooldownDescriptor {
	@Nullable Job getJob();

	@Nullable JobType getJobType();

	boolean defaultPersOverlay();
}
