package gg.xp.xivsupport.timelines;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MakeTimelines {

	private static final Logger log = LoggerFactory.getLogger(MakeTimelines.class);

	private MakeTimelines() {
	}

	public static void main(String[] args) {
		WebDriverManager.chromedriver().setup();
		ChromeOptions opts = new ChromeOptions();
		opts.setHeadless(true);
		ChromeDriver driver = new ChromeDriver(opts);
		try {
			driver.get("https://quisquous.github.io/cactbot/ui/raidboss/raidboss.html?OVERLAY_WS=wss://127.0.0.1:10501");
//			Thread.sleep(1000);
			driver.executeScript("""
					await new Promise((resolve) => {
					    const id = 'fakeId' + Math.random();
					    window['webpackChunkcactbot'].push([[id], null, (req) => resolve(req)]);
					}).then((req) => {
					    const manifest = req(parseInt(Object.keys(webpackChunkcactbot[0].find(a=>!Array.isArray(a)))[0])).Z;
					window['manifest'] = manifest;
					window['out'] = (
					        Object.keys(manifest)
					              .filter(fp=>typeof(manifest[fp])==='object')
					              .map(fp => {return {...manifest[fp], fp: fp};})
					              .filter(e=>e.zoneId)
					              .filter(e=>e.timelineFile)
					              .map(e=>`${e.zoneId},"${e.timelineFile}"`)
					              .join("\\n")
					    );
					});
					console.log('Done');
					""");
//			Thread.sleep(200);
			Object out = driver.executeScript("return window['out']");
			log.info("{}", out);
			Files.writeString(Path.of("timelines", "src", "main", "resources", "timelines.csv"), out.toString(), StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		finally {
			driver.quit();
		}
	}


}
