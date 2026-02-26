package gg.xp.xivsupport.events.fflogs;

/**
 * Represents the /reportData/report node
 */
public record FflogsBaseData(
	String code,
	double startTime,
	double endTime
//	List<FflogsFightData> fights,
//	FflogsMasterData masterData,
//	List<FflogsEventsData> events
) {
}
