package gg.xp.xivdata.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
		List<String[]> csvRaw = ReadCsv.cellsFromResource("/xiv/actions/Action.csv");
		Map<Long, String[]> csvData = new HashMap<>(csvRaw.size());
		csvRaw.forEach(row -> {
			try {
				long id = Long.parseLong(row[0]);
				csvData.put(id, row);
			}
			catch (NumberFormatException nfe) {
				// Ignore non-numeric
			}
		});

		List<Map<String, Object>> out = maps.stream()
				.filter(m -> m.get("cooldown") != null)
				.sorted(Comparator.comparing(d -> (Integer) d.get("id")))
				.collect(Collectors.toList());

		log.info("Number of cooldowns: {}", out.size());

		Map<String, CdData> data = new LinkedHashMap<>();

		out.forEach(item -> {
			CdData cdData = new CdData();
			cdData.fullName = item.get("name").toString();
			String jobRaw = item.get("job").toString().toUpperCase(Locale.ROOT);
			switch (jobRaw) {
				case "TANK" -> cdData.jobType = JobType.TANK;

				// TODO: zeffUI has "Caster" which means healer and caster DPS
				// Maybe just ignore it for healers since they would be less likely
				// to use it as part of their normal rotation?
				case "CASTER", "CASTERDPS" -> cdData.jobType = JobType.CASTER;
				case "MELEEDPS" -> cdData.jobType = JobType.MELEE_DPS;
				default -> cdData.job = Job.valueOf(jobRaw);
			}

			String typeRaw = item.get("type").toString();
			switch (typeRaw) {
				// TODO: these don't really match up
				case "RaidBuff":
					cdData.cdType = Cooldown.CooldownType.PARTY_BUFF;
					break;
				case "Mitigation":
					cdData.cdType = Cooldown.CooldownType.PERSONAL_MIT;
					break;
				case "Buff":
					cdData.cdType = Cooldown.CooldownType.PERSONAL_BURST;
					break;
				case "Party":
				default:
					cdData.cdType = Cooldown.CooldownType.PARTY_MIT;
					break;
			}
			if (cdData.fullName.endsWith(" Finish")) {
				return;
			}
			cdData.enumName = cdData.fullName.replaceAll("[^A-Za-z]", "");
			cdData.abilityId = (int) item.get("id");
			// TODO: combine
//			cdData.cooldown = (int) item.get("cooldown");
			cdData.cooldown = Double.parseDouble(csvData.get(cdData.abilityId)[39]) / 10;
			Object durationRaw = item.get("duration");
			cdData.duration = durationRaw == null ? null : (double) ((Integer) durationRaw);

			if (cdData.cooldown < 15) {
				return;
			}
			data.putIfAbsent(cdData.enumName, cdData);
		});

		// TODO: we already have Action.csv, so pull the name from that
//		data.sort(Comparator.comparing(c -> c.enumName));
		data.forEach((name, d) -> {
			String jobOrType;
			if (d.job == null) {
				jobOrType = d.jobType.toString();
			}
			else {
				jobOrType = d.job.toString();
			}
			System.out.printf("%s(%s, %s, \"%s\", CooldownType.%s, 0x%x, %s),%n", d.enumName, jobOrType, d.cooldown, d.fullName, d.cdType, d.abilityId, -1);
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
