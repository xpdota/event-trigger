package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JobSortSetting {

	private static final Logger log = LoggerFactory.getLogger(JobSortSetting.class);
	private final Set<Job> squelchWarningsForJobs = new HashSet<>();
	private final EnumListSetting<Job> sortSetting;
	private final Set<Job> allValidJobs;
	private final XivState state;
	private List<Job> currentJailSort;

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
				currentJailSort = listFromSettings;
			}
			catch (Throwable t) {
				log.error("Saved jail order did not pass validation", t);
			}
		}
		// Fall back to default
		if (currentJailSort == null) {
			currentJailSort = allValidJobs
					.stream()
					.sorted(defaultJailSortComparator)
					.toList();
		}
		this.state = state;
	}

	private final Comparator<Job> defaultJailSortComparator = Comparator.<Job, Integer>comparing(job -> {
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

	public Comparator<XivPlayerCharacter> getPlayerJailSortComparator() {
		return Comparator.<XivPlayerCharacter, Integer>comparing(player -> {
			Job job = player.getJob();
			int index = currentJailSort.indexOf(job);
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

	public Comparator<XivPlayerCharacter> getComparator() {
		return getPlayerJailSortComparator();
	}

	public List<Job> getJobOrder() {
		return Collections.unmodifiableList(currentJailSort);
	}

	public List<Job> getCurrentJailSort() {
		return Collections.unmodifiableList(currentJailSort);
	}

	public void resetJailSort() {
		this.currentJailSort = currentJailSort.stream()
				.sorted(defaultJailSortComparator)
				.toList();
		sortSetting.delete();
	}

	public void validateJobSortOrder(List<Job> newSort) {
		int expectedNumberOfJobs = allValidJobs.size();
		int actualNumberOfJobs = newSort.size();
		if (expectedNumberOfJobs != actualNumberOfJobs) {
			throw new IllegalArgumentException(String.format("New jail sort order was not the same size! %s -> %s", expectedNumberOfJobs, actualNumberOfJobs));
		}
		EnumSet<Job> newSortAsSet = EnumSet.copyOf(newSort);
		int newUniqueSize = newSortAsSet.size();
		if (newUniqueSize != actualNumberOfJobs) {
			throw new IllegalArgumentException("New jail sort had duplicates!");
		}
	}

	public void setJailSort(List<Job> newSort) {
		validateJobSortOrder(newSort);
		this.currentJailSort = List.copyOf(newSort);
		saveJailSort();
	}

	private void saveJailSort() {
		this.sortSetting.set(currentJailSort);
	}

	public List<XivPlayerCharacter> partyOrderPreview() {
		return state.getPartyList().stream().sorted(getPlayerJailSortComparator()).collect(Collectors.toList());
	}
}
