package gg.xp.xivsupport.events.state;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivdata.jobs.JobType;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.EnumListSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@ScanMe
public class PartySortOrder {
	private static final Logger log = LoggerFactory.getLogger(PartySortOrder.class);
	private final EnumListSetting<JobType> categorySortSetting;
	private final Map<JobType, EnumListSetting<Job>> sortWithinCategory;
	// Replaced atomically for thread safety
	private static Map<Job, Integer> cachedSortOrder = Collections.emptyMap();

	public PartySortOrder(PersistenceProvider pers) {
		List<JobType> defaultCategorySort = List.of(JobType.TANK, JobType.HEALER, JobType.MELEE_DPS, JobType.PRANGED, JobType.CASTER);
		categorySortSetting = new EnumListSetting<>(JobType.class, pers, "party-sort.categories.order", EnumListSetting.BadKeyBehavior.RETURN_DEFAULT, defaultCategorySort);

		sortWithinCategory = new EnumMap<>(JobType.class);

		for (JobType jobType : defaultCategorySort) {
			List<Job> defaultSortForThisCategory = Arrays.stream(Job.values()).filter(j -> j.getCategory() == jobType).sorted(Comparator.comparing(Job::defaultPartySortOrder)).toList();
			EnumListSetting<Job> setting = new EnumListSetting<>(Job.class, pers, String.format("party-sort.categories.%s.order", jobType), EnumListSetting.BadKeyBehavior.RETURN_DEFAULT, defaultSortForThisCategory);
			sortWithinCategory.put(jobType, setting);
			boolean dirty = false;
			List<Job> currentSortForThisCategory = new ArrayList<>(setting.get());
			for (Job job : defaultSortForThisCategory) {
				if (!currentSortForThisCategory.contains(job)) {
					currentSortForThisCategory.add(job);
					dirty = true;
				}
			}
			if (dirty) {
				setting.set(currentSortForThisCategory);
			}
		}
		updateSortOrderCache();
	}

	@SuppressWarnings("Convert2streamapi") // Just looks worse and is probably slower
	private void updateSortOrderCache() {
		List<Job> jobs = new ArrayList<>(Arrays.asList(Job.values()));
		jobs.sort(Comparator.comparing(job -> {
			int catSort;
			JobType cat = job.getCategory();
			int catIndex = categorySortSetting.get().indexOf(cat);
			if (catIndex >= 0) {
				// Known category - catSort by cat first
				catSort = catIndex << 8;
				EnumListSetting<Job> settingForCat = sortWithinCategory.get(cat);
				if (settingForCat == null) {
					// This shouldn't happen, but try to handle it in a reasonable way.
					log.warn("Unusual sorting condition #1 for job {} - please report as bug", job);
					return 999_999;
				}
				else {
					int sortWithinCat = settingForCat.get().indexOf(job);
					if (sortWithinCat >= 0) {
						return catSort + sortWithinCat;
					}
					else {
						log.warn("Unusual sorting condition #2 for job {} - please report as bug", job);
						return catSort + 128;
					}
				}
			}
			else {
				// DOH/DOL/unknown/etc, catSort last
				return 1_000_000;
			}
		}));
		Map<Job, Integer> newCachedSortOrder = new EnumMap<>(Job.class);
		for (int i = 0; i < jobs.size(); i++) {
			newCachedSortOrder.put(jobs.get(i), i);
		}
		cachedSortOrder = newCachedSortOrder;
	}

	public Comparator<Job> compare() {
		return Comparator.comparing(cachedSortOrder::get);
	}

	public int getSortOrder(Job job) {
		return cachedSortOrder.get(job);
	}

}
