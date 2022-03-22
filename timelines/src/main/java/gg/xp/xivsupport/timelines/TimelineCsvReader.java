package gg.xp.xivsupport.timelines;

import gg.xp.xivdata.data.ReadCsv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TimelineCsvReader {

	private static boolean loaded;
	private static List<TimelineInfo> csvValues = Collections.emptyList();

	public static List<TimelineInfo> readCsv() {
		if (loaded) {
			return csvValues;
		}
		List<String[]> rows = ReadCsv.cellsFromResource("/timelines.csv");

		List<TimelineInfo> out = new ArrayList<>();
		rows.forEach(row -> {
			long zoneId = Long.parseLong(row[0]);
			String filename = row[1];
			out.add(new TimelineInfo(zoneId, filename));
		});
		csvValues = Collections.unmodifiableList(out);
		return csvValues;

	}
}
