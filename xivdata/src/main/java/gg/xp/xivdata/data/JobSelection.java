package gg.xp.xivdata.data;

import java.util.EnumSet;
import java.util.Set;

public class JobSelection {

	public boolean enabledForAll;
	public Set<JobType> enabledTypes = EnumSet.noneOf(JobType.class);
	public Set<Job> enabledJobs = EnumSet.noneOf(Job.class);

	public boolean enabledForJob(Job job) {
		return enabledForAll
				|| enabledJobs.contains(job)
				|| enabledTypes.contains(job.getCategory());
	}

	public static JobSelection none() {
		return new JobSelection();
	}

	public static JobSelection allCombatJobs() {
		JobSelection js = new JobSelection();
		js.enabledTypes.add(JobType.TANK);
		js.enabledTypes.add(JobType.HEALER);
		js.enabledTypes.add(JobType.CASTER);
		js.enabledTypes.add(JobType.MELEE_DPS);
		js.enabledTypes.add(JobType.PRANGED);
		return js;
	}

	public static JobSelection all() {
		JobSelection js = new JobSelection();
		js.enabledForAll = true;
		return js;
	}
}
