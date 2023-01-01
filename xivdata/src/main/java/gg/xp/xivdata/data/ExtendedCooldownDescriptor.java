package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface ExtendedCooldownDescriptor extends BasicCooldownDescriptor {
	@Nullable Job getJob();

	@Nullable JobType getJobType();

	boolean defaultPersOverlay();

	String getSettingKeyStub();

	default int sortOrder() {
		Job job = getJob();
		// Sort job categories first
		if (job == null) {
			JobType jobType = getJobType();
			if (jobType == null) {
				// Put custom user-added CDs first
				return -2;
			}
			// Then categories
			return jobType.ordinal() + 5000;
		}
		// Then jobs
		return job.defaultPartySortOrder() + 10000;
	};

	Collection<Long> getAllRelevantAbilityIds();

}
