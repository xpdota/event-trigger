package gg.xp.xivsupport.timelines;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import gg.xp.xivsupport.timelines.intl.LanguageReplacements;
import gg.xp.xivsupport.timelines.intl.TimelineReplacements;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("NewClassNamingConvention")
//@Ignore
public final class MakeTimelines {

	private static final Logger log = LoggerFactory.getLogger(MakeTimelines.class);
	private static final ObjectMapper mapper = JsonMapper.builder()
			// Need deterministic map key ordering, otherwise all the translation files will change every time you
			// run the script, despite the actual substance being the same.
			.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
			.build();

	private MakeTimelines() {
	}

	private static String stripFileName(String input) {
		int splitPoint = input.lastIndexOf('/');
		return input.substring(0, splitPoint + 1);
	}

	// TODO: automate - this is from common_replacements.ts
	private static final Map<String, Map<String, String>> commonReplacementsRaw = Map.of(
			"(?<=00:0839::|00\\|[^|]*\\|0839\\|\\||\\\\\\|0839\\\\\\|\\[\\^\\|\\]\\*\\\\\\||^)([^|:]*) will be sealed off(?: in (?:[0-9]+ seconds)?)?", Map.of(
					"en", "$1 will be sealed off",
					"de", "Noch 15 Sekunden, bis sich (?:(?:der|die|das) )?(?:Zugang zu(?:[rm]| den)? )?$1 schließt",
					"fr", "Fermeture d(?:e|u|es) (?:l'|la |les? )?$1 dans",
					"ja", "$1の封鎖まであと",
					"cn", "距$1被封锁还有",
					"ko", "15초 후에 $1[이가] 봉쇄됩니다"
			),
			"is no longer sealed", Map.of(
					"en", "is no longer sealed",
					"de", "öffnet sich (?:wieder|erneut)",
					"fr", "Ouverture ",
					"ja", "の封鎖が解かれた",
					"cn", "的封锁解除了",
					"ko", "의 봉쇄가 해제되었습니다"
			),
			"Engage!", Map.of(
					"en", "Engage!",
					"de", "Start!",
					"fr", "À l'attaque",
					"ja", "戦闘開始！",
					"cn", "战斗开始！",
					"ko", "전투 시작!"
			)
	);

	@Test
	void makeTimelines() {
		main(new String[]{});
	}

	private static boolean foundGeneral;


	public static void main(String[] args) {
		log.info("Beginning timeline extraction");
		log.info("Working directory: {}", Path.of(".").toAbsolutePath());
		if (System.getProperty("make-timelines.use-driver-helper", "true").equals("true")) {
			log.info("Setting up ChromeDriver");
			WebDriverManager.chromiumdriver().setup();
			log.info("Set up ChromeDriver");
		}
		ChromeOptions opts = new ChromeOptions();
		opts.addArguments("--headless=new");
		opts.addArguments("remote-allow-origins=*");
		opts.addArguments("disable-dev-shm-usage");
		opts.addArguments("disable-gpu");
		opts.addArguments("remote-debugging-port=60922");
		log.info("Starting ChromeDriver");
		ChromeDriver driver = new ChromeDriver(opts);
		log.info("Started ChromeDriver");
		Map<Long, String> zoneToFile = new HashMap<>();
		String timelineBaseDir = System.getProperty("timelinedir", "./src/main/resources");
		Path timelineBasePath = Path.of(timelineBaseDir);
		log.info("Writing timelines to {}", timelineBasePath.toAbsolutePath());
		Path translationsDir = timelineBasePath.resolve("timeline").resolve("translations");
		translationsDir.toFile().mkdirs();
		try {
			// Go to hosted Cactbot
			driver.get("https://quisquous.github.io/cactbot/ui/raidboss/raidboss.html?OVERLAY_WS=wss://127.0.0.1:10501");

			// Dump the contents of the webpack
			Map<?, ?> out = (Map<?, ?>) driver.executeScript("""
					return await new Promise((resolve) => {
					    const id = 'fakeId' + Math.random();
					    window['webpackChunkcactbot'].push([[id], null, (req) => resolve(req)]);
					}).then((req) => {
					    return req(parseInt(Object.keys(webpackChunkcactbot[0].find(a=>!Array.isArray(a)))[0])).Z;
					});
					""");

			out.forEach((file, content) -> {
				// Ignore flat files and other junk
				if (content instanceof Map contentMap) {
					Object timelineFileRaw = contentMap.get("timelineFile");
					boolean isGeneral = false;
					if (timelineFileRaw == null) {
						if (contentMap.containsKey("timelineReplace") && contentMap.get("zoneId") == null) {
							foundGeneral = true;
//							// TODO: "General" is not what it seems - global replacements are in common_replacements.ts
//							if (foundGeneral) {
//								// TODO: does this need to be supported?
//								throw new RuntimeException("Found more than one general timeline replacement set!");
//							}
//							foundGeneral = true;
//							isGeneral = true;
//							timelineFileRaw = "global_timeline_replacements.txt";
						}
						return;
					}
					String timelineFileName = timelineFileRaw.toString();
					Object zoneIdRaw = contentMap.get("zoneId");
					Long zoneId = (Long) zoneIdRaw;
					if (!isGeneral) {
						// Get the timeline file name
						String fullTimelineFilePath = stripFileName(file.toString()) + timelineFileName;
						zoneToFile.put(zoneId, timelineFileName);
						// Get the actual timeline file contents
						String fileContents = out.get(fullTimelineFilePath).toString().replaceAll("\r\n", "\n");
						try {
							Files.writeString(timelineBasePath.resolve("timeline").resolve(timelineFileName), fileContents);
						}
						catch (IOException e) {
							throw new RuntimeException("Error processing timeline for '" + file + '\'', e);
						}
					}
					// Get the timeline translations
					Object timelineReplace = contentMap.get("timelineReplace");
					Map<String, LanguageReplacements> allLangs = new LinkedHashMap<>();
					if (timelineReplace instanceof List timelineReplaceList) {
						for (Object o : timelineReplaceList) {
							if (o instanceof Map timelineReplaceMap) {
								// This works - just need to figure out how to work it in
								Object locale = timelineReplaceMap.get("locale");
								Object replaceSync = timelineReplaceMap.get("replaceSync");
								Object replaceText = timelineReplaceMap.get("replaceText");
								if (locale != null) {
									@SuppressWarnings("unchecked")
									LanguageReplacements reps = LanguageReplacements.fromRaw(
											replaceSync instanceof Map rsm ? ordered(rsm) : Collections.<String, String>emptyMap(),
											replaceText instanceof Map rtm ? ordered(rtm) : Collections.<String, String>emptyMap()
									);
									allLangs.put(locale.toString(), reps);
								}
								if (log.isTraceEnabled()) {
									log.trace("Timeline Replacement Map: zone {} -> {}", zoneId, timelineReplaceMap);
								}
							}
						}
					}
					TimelineReplacements tr = new TimelineReplacements(allLangs);
					// Write them
					try {
						mapper.writeValue(translationsDir.resolve(timelineFileName + ".json").toFile(), tr);
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			});
			{
				Map<String, LanguageReplacements> commonReplacements = new LinkedHashMap<>();
				Collection<String> langs = commonReplacementsRaw.values().stream().flatMap(m -> m.keySet().stream()).distinct().collect(Collectors.toList());
				langs.forEach(lang -> {
					Map<String, String> replacementsForThisLang = new LinkedHashMap<>();
					commonReplacementsRaw.forEach((syncKey, replacementsMap) -> {
						String replacementForLang = replacementsMap.get(lang);
						if (replacementForLang != null) {
							replacementsForThisLang.put(syncKey, replacementForLang);
						}
					});
					commonReplacements.put(lang, LanguageReplacements.fromRaw(replacementsForThisLang, Map.of()));
				});
				TimelineReplacements globalReplacements = new TimelineReplacements(commonReplacements);
				try {
					mapper.writeValue(translationsDir.resolve("global_timeline_replacements.txt.json").toFile(), globalReplacements);
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			String inCsvFormat = zoneToFile.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> e.getKey() + ",\"" + e.getValue() + '"').collect(Collectors.joining("\n"));
			Files.writeString(timelineBasePath.resolve("timelines.csv"), inCsvFormat, StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		finally {
			driver.quit();
		}
		log.info("Done making timelines");
	}

	private static <K extends Comparable<K>, V> Map<K, V> ordered(Map<K, V> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}
}
