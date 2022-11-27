package gg.xp.xivdata.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JobSelection {

	protected boolean enabledForAll;
	protected final Set<JobType> enabledTypes = EnumSet.noneOf(JobType.class);
	protected final Set<Job> enabledJobs = EnumSet.noneOf(Job.class);

	public boolean enabledForJob(Job job) {
		JobSelectionState state = stateForJob(job);
		return state.countsAsEnabled();
	}

	public boolean isTypeAllowed(JobType type) {
		return true;
	}

	public boolean isJobAllowed(Job job) {
		if (!isTypeAllowed(job.getCategory())) {
			return false;
		}
		return true;
	}

	public static JobSelection none() {
		return new JobSelection();
	}


	public static JobSelection all() {
		JobSelection js = new JobSelection();
		js.setEnabledForAll(true);
		return js;
	}

	public JobSelectionState stateForJob(Job job) {
		if (!isJobAllowed(job)) {
			return JobSelectionState.NOT_SELECTED;
		}
		if (enabledJobs.contains(job)) {
			return JobSelectionState.SELECTED;
		}
		if (stateForCategory(job.getCategory()).countsAsEnabled()) {
			return JobSelectionState.SELECTED_FROM_PARENT;
		}
		return JobSelectionState.NOT_SELECTED;
	}

	public JobSelectionState stateForCategory(JobType type) {
		if (!isTypeAllowed(type)) {
			return JobSelectionState.NOT_SELECTED;
		}
		if (enabledTypes.contains(type)) {
			return JobSelectionState.SELECTED;
		}
		if (enabledForAll) {
			return JobSelectionState.SELECTED_FROM_PARENT;
		}
		return JobSelectionState.NOT_SELECTED;
	}

	public void changeCategoryState(JobType type, boolean enabled) {
		if (enabled) {
			if (enabledForAll || enabledTypes.contains(type)) {
				return;
			}
			enabledTypes.add(type);
			enabledJobs.removeIf(job -> job.getCategory() == type);
		}
		else {
			explodeEnabledForAll();
			enabledTypes.remove(type);
		}
	}

	public void changeJobState(Job job, boolean enabled) {
		if (enabled) {
			// Get rid of no-ops
			if (enabledForJob(job)) {
				return;
			}
			enabledJobs.add(job);
		}

		if (!enabled) {
			// Get rid of no-ops
			if (!enabledForJob(job)) {
				return;
			}
			JobType jobType = job.getCategory();
			explodeEnabledForAll();
			explodeEnabledForType(jobType);
			enabledJobs.remove(job);
		}
	}

	private void explodeEnabledForAll() {
		if (enabledForAll) {
			enabledTypes.addAll(Arrays.stream(JobType.values()).filter(this::isTypeAllowed).toList());
			enabledForAll = false;
		}
	}

	private void explodeEnabledForType(JobType type) {
		if (enabledTypes.remove(type)) {
			enabledJobs.addAll(Arrays.stream(Job.values()).filter(otherJob -> otherJob.getCategory() == type && isJobAllowed(otherJob)).toList());
		}
	}

	public boolean isEnabledForAll() {
		return enabledForAll;
	}

	public void setEnabledForAll(boolean enabledForAll) {
		if (enabledForAll == this.enabledForAll) {
			return;
		}
		this.enabledForAll = enabledForAll;
		enabledJobs.clear();
		enabledTypes.clear();
	}

	public Set<JobType> getEnabledTypes() {
		return Collections.unmodifiableSet(enabledTypes);
	}

	public Set<Job> getEnabledJobs() {
		return Collections.unmodifiableSet(enabledJobs);
	}

	public String describeSelection() {
		if (enabledForAll) {
			return "All";
		}
		String result = Stream.concat(enabledTypes.stream().map(JobType::getFriendlyName), enabledJobs.stream().map(Job::getFriendlyName)).collect(Collectors.joining(", "));
		if (result.isBlank()) {
			return "None";
		}
		return result;
	}

	@JsonIgnore
	public boolean isEmpty() {
		return !enabledForAll && enabledTypes.isEmpty() && enabledJobs.isEmpty();
	}
}
