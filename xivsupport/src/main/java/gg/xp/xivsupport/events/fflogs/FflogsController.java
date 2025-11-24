package gg.xp.xivsupport.events.fflogs;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.StringSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ScanMe
public class FflogsController {

	private static final ObjectMapper mapper = new ObjectMapper();
	private final StringSetting clientId;
	private final StringSetting secret;
	private FflogsClient client;

	public FflogsController(PersistenceProvider pers) {
		clientId = new StringSetting(pers, "fflogs-support.client-id", "");
		secret = new StringSetting(pers, "fflogs-support.secret", "");
	}

	public StringSetting clientId() {
		return clientId;
	}

	public StringSetting clientSecret() {
		return secret;
	}

	private FflogsClient getClient() {
		if (client == null) {
			client = new FflogsClient(this);
			client.init();
		}
		return client;
	}

	public List<JsonNode> downloadReport(FflogsReportLocator reportLocator) {
		List<JsonNode> out = new ArrayList<>();
		long startTime = 0;
		FflogsClient client = getClient();
		Integer fight = reportLocator.fight();
		String report = reportLocator.report();
		if (fight == null) {
			throw new IllegalArgumentException("A fight number must be specified");
		}
		else if (fight == -1) {
			fight = getLastFightId(report);
		}

		while (true) {
			JsonNode jsonNode = client.queryV2internal(queryFull, Map.of(
					"start", startTime,
					"report", report,
					"fight", fight
			));
			out.add(jsonNode);
			JsonNode next = jsonNode.at("/reportData/report/events/nextPageTimestamp");
			if (next.isNull()) {
				break;
			}
			else {
				startTime = next.longValue();
			}
		}

		return out;
	}

	private int getLastFightId(String report) {
		List<FflogsFight> fflogsFights = getFights(report);
		// Starts at 1 - don't subtract
		return fflogsFights.get(fflogsFights.size() - 1).id();
	}

	public List<FflogsFight> getFights(String report) {
		JsonNode fightsRoot = getClient().queryV2internal(queryFightsOnly, Map.of("report", report));
		JsonNode fights = fightsRoot.at("/reportData/report/fights");
		return mapper.convertValue(fights, new TypeReference<>() {
		});
	}


	private static final String queryFull = """
			query GetReport($report: String, $fight: Int, $start: Float) {
				reportData {
					report(code: $report) {
						code
						startTime
						endTime
						fights(fightIDs:[$fight]) {
							maps {
								id
							}
							gameZone {
								id
								name
							}
						}
						masterData {
							actors {
								gameID
								id
								name
								petOwner
								type
								subType
							}
						}
						events(
							startTime: $start
							fightIDs: [$fight]
							endTime: 9639365786158
							useAbilityIDs: true
							includeResources: true
							limit: 10000
						) {
							data
							nextPageTimestamp
						}
					}
				}
			}

							""";
	private static final String queryFightsOnly = """
			query GetReport($report: String) {
				reportData {
					report(code: $report) {
						code
						fights {
							startTime
							endTime
							fightPercentage
							kill
							id
							gameZone {
								id
								name
							}
						}
					}
				}
			}
												""";
}
