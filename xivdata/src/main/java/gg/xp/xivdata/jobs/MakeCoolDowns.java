package gg.xp.xivdata.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class MakeCoolDowns {
	private static final Logger log = LoggerFactory.getLogger(MakeCoolDowns.class);

	private MakeCoolDowns() {
	}

	public static void main(String[] args) throws IOException, URISyntaxException {

		List<Map<String, Object>> maps = ReadData.readData();

		List<Map<String, Object>> out = maps.stream()
				.filter(m -> m.get("cooldown") != null)
				.sorted(Comparator.comparing(d -> (Integer) d.get("id")))
				.collect(Collectors.toList());

		log.info("Number of cooldowns: {}", out.size());

		List<CdData> data = new ArrayList<>();

		out.forEach(item -> {
			CdData cdData = new CdData();
			cdData.fullName = item.get("name").toString();
			String jobRaw = item.get("job").toString().toUpperCase(Locale.ROOT);
			switch (jobRaw) {
				case "TANK":
					cdData.jobType = JobType.TANK;
					break;
				// TODO: zeffUI has "Caster" which means healer and caster DPS
				// Maybe just ignore it for healers since they would be less likely
				// to use it as part of their normal rotation?
				case "CASTER":
				case "CASTERDPS":
					cdData.jobType = JobType.CASTER;
					break;
				case "MELEEDPS":
					cdData.jobType = JobType.MELEE_DPS;
					break;
				default:
					cdData.job = Job.valueOf(jobRaw);
					break;
			}

			String typeRaw = item.get("type").toString();
			switch (typeRaw) {
				case "RaidBuff":
					cdData.cdType = Cooldown.CooldownType.OFFENSIVE;
					break;
				case "Mitigation":
					cdData.cdType = Cooldown.CooldownType.DEFENSIVE;
					break;
				case "Buff":
					cdData.cdType = Cooldown.CooldownType.PERSONAL;
			}
			cdData.enumName = cdData.fullName.replaceAll("[^A-Za-z]", "");
			cdData.cooldown = (int) item.get("cooldown");
			Object durationRaw = item.get("duration");
			cdData.duration = durationRaw == null ? null : (double) ((Integer) durationRaw);
			cdData.abilityId = (int) item.get("id");
			data.add(cdData);
		});

		// TODO: we already have Action.csv, so pull the name from that
		data.sort(Comparator.comparing(c -> c.enumName));
		data.forEach(d -> {
			String jobOrType;
			if (d.job == null) {
				jobOrType = d.jobType.toString();
			}
			else {
				jobOrType = d.job.toString();
			}
			System.out.printf("%s(%s, %s, \"%s\", %s, 0x%x, %s),%n", d.enumName, jobOrType, d.cooldown, d.fullName, d.cdType, d.abilityId, -1);
		});


	}

	private static class CdData {
		private String enumName;
		private String fullName;
		private Cooldown.CooldownType cdType;
		private Job job;
		private JobType jobType;
		private long abilityId;
		private double cooldown;
		private Double duration;
	}
}
