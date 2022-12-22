package gg.xp.xivsupport.timelines;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("NewClassNamingConvention")
//@Ignore
public final class MakeTimelines {

	private static final Logger log = LoggerFactory.getLogger(MakeTimelines.class);

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


	public static void main(String[] args) {
		log.info("Beginning timeline extraction");
		log.info("Working directory: {}", Path.of(".").toAbsolutePath());
		if (System.getProperty("make-timelines.use-driver-helper", "true").equals("true")) {
			WebDriverManager.chromedriver().setup();
		}
		ChromeOptions opts = new ChromeOptions();
		opts.setHeadless(true);
		ChromeDriver driver = new ChromeDriver(opts);
		Map<Long, String> zoneToFile = new HashMap<>();
		String timelineBaseDir = System.getProperty("timelinedir", "timelines/src/main/resources");
		Path timelineBasePath = Path.of(timelineBaseDir);
		try {
			driver.get("https://quisquous.github.io/cactbot/ui/raidboss/raidboss.html?OVERLAY_WS=wss://127.0.0.1:10501");

			Map<?, ?> out = (Map<?, ?>) driver.executeScript("""
					return await new Promise((resolve) => {
					    const id = 'fakeId' + Math.random();
					    window['webpackChunkcactbot'].push([[id], null, (req) => resolve(req)]);
					}).then((req) => {
					    return req(parseInt(Object.keys(webpackChunkcactbot[0].find(a=>!Array.isArray(a)))[0])).Z;
					});
					""");

			out.forEach((file, content) -> {
				if (content instanceof Map contentMap) {
					Object timelineFileRaw = contentMap.get("timelineFile");
					if (timelineFileRaw == null) {
						return;
					}
					String timelineFileName = timelineFileRaw.toString();
					Object zoneIdRaw = contentMap.get("zoneId");
					Long zoneId = (Long) zoneIdRaw;
					String fullTimelineFilePath = stripFileName(file.toString()) + timelineFileName;
					zoneToFile.put(zoneId, timelineFileName);
					String fileContents = out.get(fullTimelineFilePath).toString().replaceAll("\r\n", "\n");
					try {
						Files.writeString(timelineBasePath.resolve("timeline").resolve(timelineFileName), fileContents);
					}
					catch (IOException e) {
						throw new RuntimeException("Error processing timeline for '" + file + '\'', e);
					}
					/*
						TODO: timeline translations
						Should be able to grab the "timelineReplace" array, which has one object for each language
					 */
					Object timelineReplace = contentMap.get("timelineReplace");
					if (timelineReplace instanceof List timelineReplaceList) {
						for (Object o : timelineReplaceList) {
							if (o instanceof Map timelineReplaceMap) {
								// This works - just need to figure out how to work it in
//								log.info("Timeline Replacement Map: zone {} -> {}", zoneId, timelineReplaceMap);
							}
						}
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



}
