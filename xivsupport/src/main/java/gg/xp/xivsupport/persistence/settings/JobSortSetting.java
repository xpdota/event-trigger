package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Setting to hold an ordered list of jobs, typically used for job priority kind of stuff.
 */
public class JobSortSetting {

	private static final Logger log = LoggerFactory.getLogger(JobSortSetting.class);
	private final Set<Job> squelchWarningsForJobs = EnumSet.noneOf(Job.class);
	private final EnumListSetting<Job> sortSetting;
	private final Set<Job> allValidJobs;
	private final XivState state;
	private List<Job> jobSort;

	// TODO: custom default
	public JobSortSetting(PersistenceProvider pers, String settingKey, XivState state) {
		sortSetting = new EnumListSetting<>(Job.class, pers, settingKey, EnumListSetting.BadKeyBehavior.RETURN_DEFAULT, null);
		// TODO: add "upgrades to job X" field to Job so that we can just combine
		// jobs and base classes.
		allValidJobs = Arrays.stream(Job.values())
				.filter(Job::isCombatJob)
				.collect(Collectors.toUnmodifiableSet());
		List<Job> listFromSettings = sortSetting.get();
		if (listFromSettings != null) {
			try {
				validateJobSortOrder(listFromSettings);
				jobSort = listFromSettings;
			}
			catch (JobSortValidationException je) {
				if (je.isSilent()) {
					log.warn(je.getMessage());
				}
				else {
					log.error(je.getMessage(), je);
				}
			}
			catch (Throwable t) {
				log.error("Saved jail order did not pass validation ({})", settingKey, t);
			}
		}
		// Fall back to default
		if (jobSort == null) {
			jobSort = allValidJobs
					.stream()
					.sorted(getDefaultSort())
					.toList();
		}
		this.state = state;
	}

	/**
	 * @return A comparator which can be used to sort a list of players according to their jobs
	 */
	public Comparator<XivPlayerCharacter> getComparator() {
		return Comparator.<XivPlayerCharacter, Integer>comparing(player -> {
			Job job = player.getJob();
			int index = jobSort.indexOf(job);
			if (index == -1) {
				boolean firstWarning = squelchWarningsForJobs.add(job);
				if (firstWarning) {
					log.warn("Couldn't determine jail prio for player {}", player);
				}
				// Return a big value so it sorts last
				return 65536;
			}
			return index;
		}).thenComparing(XivPlayerCharacter::getName);
	}

	/**
	 * Deprecated, use {@link #getComparator()}
	 *
	 * @return A comparator which can be used to sort a list of players according to their jobs
	 */
	@Deprecated // use getComparator
	public Comparator<XivPlayerCharacter> getPlayerJailSortComparator() {
		return getComparator();
	}

	/**
	 * @return The current job sort order
	 */
	public List<Job> getJobOrder() {
		return Collections.unmodifiableList(jobSort);
	}

	/**
	 * Deprecated, use {@link #getJobOrder()}
	 *
	 * @return The current job sort order
	 */
	@Deprecated // use getJobOrder()
	public List<Job> getCurrentJailSort() {
		return Collections.unmodifiableList(jobSort);
	}

	/**
	 * Reset the job order to the default
	 */
	public void resetJobOrder() {
		this.jobSort = jobSort.stream()
				.sorted(getDefaultSort())
				.toList();
		sortSetting.delete();
	}

	protected Comparator<Job> getDefaultSort() {

		return Comparator.<Job, Integer>comparing(job -> {
			if (job.isMeleeDps()) {
				return 1;
			}
			if (job.isTank()) {
				return 2;
			}
			if (job.isCaster()) {
				return 3;
			}
			if (job.isPranged()) {
				return 4;
			}
			if (job.isHealer()) {
				return 5;
			}
			// Shouldn't happen
			log.warn("Couldn't determine jail prio for job {}", job);
			return 6;
		}).thenComparing(Enum::ordinal);
	}


	/**
	 * Deprecated, see {@link #resetJobOrder()}
	 */
	@Deprecated
	public void resetJailSort() {
		resetJobOrder();
	}

	/**
	 * @return True if this setting has actually been set by the user, false if it is using default values.
	 */
	public boolean isSet() {
		return sortSetting.isSet();
	}

	public void validateJobSortOrder(List<Job> newSort) {
		// First, check for duplicates
		// Convert the list to a set and check that the size is still the same.
		if (newSort.isEmpty()) {
			throw new JobSortValidationException("New sort order was empty!", false, allValidJobs, newSort);
		}
		Set<Job> newSortAsSet = EnumSet.copyOf(newSort);
		int newUniqueSize = newSortAsSet.size();
		int actualNumberOfJobs = newSort.size();
		if (newUniqueSize != actualNumberOfJobs) {
			List<Job> tmpJobs = new ArrayList<>(newSort);
			allValidJobs.forEach(tmpJobs::remove);
			throw new JobSortValidationException("New jail sort had duplicates! Extras: %s".formatted(tmpJobs), false, allValidJobs, newSort);
		}
		if (!newSortAsSet.equals(allValidJobs)) {
			// Jobs present in base list but not in the new order
			Set<Job> jobsMissingFromNewSort = EnumSet.copyOf(allValidJobs);
			jobsMissingFromNewSort.removeAll(newSortAsSet);
			// Jobs present in new order but not in base list
			Set<Job> extraneousJobs = EnumSet.copyOf(newSort);
			extraneousJobs.removeAll(allValidJobs);

			boolean silentFail = true;
			StringBuilder sb = new StringBuilder("Job sort did not pass validation!");

			if (!jobsMissingFromNewSort.isEmpty()) {
				sb.append("\nJobs missing from new sort: ").append(jobsMissingFromNewSort);
			}
			if (!extraneousJobs.isEmpty()) {
				sb.append("\nInvalid jobs found: ").append(extraneousJobs);
				silentFail = false;
			}
			throw new JobSortValidationException(sb.toString(), silentFail, allValidJobs, newSort);
		}
	}


	/**
	 * Set a new job order
	 *
	 * @param newSort The new job order. MUST contain EVERY JOB. If you wish to only supply some jobs, see
	 *                {@link #setJobOrderPartial(List)}
	 */
	public void setJobOrder(List<Job> newSort) {
		validateJobSortOrder(newSort);
		this.jobSort = List.copyOf(newSort);
		saveJailSort();
	}

	/**
	 * Deprecated, use {@link #setJobOrder(List)}
	 *
	 * @param newSort The new job order. MUST contain EVERY JOB. If you wish to only supply some jobs, see
	 *                {@link #setJobOrderPartial(List)}
	 */
	@Deprecated
	public void setJailSort(List<Job> newSort) {
		setJobOrder(newSort);
	}

	/**
	 * Like {@link #setJobOrder(List)}, but does not require you to have every single combat job in your list.
	 * <p>
	 * In the event that you do not specify all jobs, they will be appended to the end of the list in whatever order
	 * they currently appear.
	 *
	 * @param partial The list of jobs. May be non-exhaustive.
	 */
	public void setJobOrderPartial(List<Job> partial) {
		List<Job> newSort = new ArrayList<>(partial);
		jobSort.forEach(j -> {
			if (!newSort.contains(j)) {
				newSort.add(j);
			}
		});
		setJobOrder(newSort);
	}
	// TODO: add setSortPartial method
	// TODO: rename methods so they aren't "Jail"

	private void saveJailSort() {
		this.sortSetting.set(jobSort);
	}

	/**
	 * @return The current party list, sorted by this job priority.
	 */
	public List<XivPlayerCharacter> partyOrderPreview() {
		return state.getPartyList().stream().sorted(getComparator()).toList();
	}
}
