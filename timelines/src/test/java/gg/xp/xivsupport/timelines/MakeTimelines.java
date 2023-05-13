package gg.xp.xivsupport.timelines;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import gg.xp.xivsupport.timelines.intl.LanguageReplacements;
import gg.xp.xivsupport.timelines.intl.TimelineReplacements;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

	@Test
	void makeTimelines() {
		main(new String[]{});
	}

	private static boolean foundGeneral;


	public static void main(String[] args) {
		log.info("Beginning timeline extraction");
		log.info("Working directory: {}", Path.of(".").toAbsolutePath());
		if (System.getProperty("make-timelines.use-driver-helper", "true").equals("true")) {
			WebDriverManager.chromedriver().setup();
		}
		ChromeOptions opts = new ChromeOptions();
		opts.setHeadless(true);
		opts.addArguments("--remote-allow-origins=*");
		opts.addArguments("--disable-dev-shm-usage");
		ChromeDriver driver = new ChromeDriver(opts);
		Map<Long, String> zoneToFile = new HashMap<>();
		String timelineBaseDir = System.getProperty("timelinedir", "timelines/src/main/resources");
		Path timelineBasePath = Path.of(timelineBaseDir);
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
							if (foundGeneral) {
								// TODO: does this need to be supported?
								throw new RuntimeException("Found more than one general timeline replacement set!");
							}
							foundGeneral = true;
							isGeneral = true;
							timelineFileRaw = "global_timeline_replacements.txt";
						}
						else {
							return;
						}
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
			String inCsvFormat = zoneToFile.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> e.getKey() + ",\"" + e.getValue() + '"').collect(Collectors.joining("\n"));
			Files.writeString(timelineBasePath.resolve("timelines.csv"), inCsvFormat, StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		finally {
			driver.quit();
		}
	}

	private static <K extends Comparable<K>, V> Map<K, V> ordered(Map<K, V> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}
}
