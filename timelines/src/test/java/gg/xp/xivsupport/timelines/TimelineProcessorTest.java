package gg.xp.xivsupport.timelines;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.parsers.FakeACTTimeSource;
import gg.xp.xivsupport.events.actlines.parsers.FakeTimeSource;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.lang.GameLanguage;
import gg.xp.xivsupport.lang.GameLanguageInfoEvent;
import gg.xp.xivsupport.lang.LanguageController;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.sys.KnownLogSource;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import gg.xp.xivsupport.sys.XivMain;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class TimelineProcessorTest {
	@Test
	void basicTest() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		pico.addComponent(new FakeACTTimeSource());

		pico.getComponent(PrimaryLogSource.class).setLogSource(KnownLogSource.ACT_LOG_FILE);

		EventDistributor dist = pico.getComponent(EventDistributor.class);
		dist.acceptEvent(new InitEvent());

		TimelineManager tm = pico.getComponent(TimelineManager.class);
		XivState state = pico.getComponent(XivState.class);
		dist.acceptEvent(new ACTLogLineEvent("01|2022-04-19T17:33:36.9650000-07:00|3CD|The Dead Ends|b66c88c402200f35"));

		Assert.assertEquals(state.getZone().getId(), 0x3CD);
		MatcherAssert.assertThat(tm.getCurrentDisplayEntries(), Matchers.empty());

		dist.acceptEvent(new ACTLogLineEvent("00|2022-04-19T17:36:39.0000000-07:00|0839||The shell mound will be sealed off in 15 seconds!|a5fb68c9fda6fe87"));
		{
			List<VisualTimelineEntry> currentEntries = tm.getCurrentDisplayEntries();
			VisualTimelineEntry firstEntry = currentEntries.get(0);
			MatcherAssert.assertThat(firstEntry.timeUntil(), Matchers.closeTo(18.1, 0.11));
		}
	}

	@Test
	void translationTest() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		pico.addComponent(new FakeACTTimeSource());

		pico.getComponent(PrimaryLogSource.class).setLogSource(KnownLogSource.ACT_LOG_FILE);

		EventDistributor dist = pico.getComponent(EventDistributor.class);
		dist.acceptEvent(new InitEvent());
		dist.acceptEvent(new GameLanguageInfoEvent(GameLanguage.German));

		TimelineManager tm = pico.getComponent(TimelineManager.class);
		XivState state = pico.getComponent(XivState.class);
		dist.acceptEvent(new ZoneChangeEvent(new XivZone(1081, "p5n")));

		Assert.assertEquals(state.getZone().getId(), 1081);
		MatcherAssert.assertThat(tm.getCurrentDisplayEntries(), Matchers.empty());

		//  1[56]:[^:]*:Proto-Karfunkel:76D6:
		dist.acceptEvent(new ACTLogLineEvent("21|2021-07-27T12:48:22.4630000-04:00|40024FD1|Proto-Karfunkel|76D6|Aetherochemical Laser|10FF0001|Tini Poutini|750003|4620000|1B|F678000|0|0|0|0|0|0|0|0|0|0|0|0|36022|36022|5200|10000|0|1000|1.846313|-12.31409|10.60608|-2.264526|16000|16000|8840|10000|0|1000|-9.079163|-14.02307|18.7095|1.416605|0000DE1F|0|5d60825d70bb46d7fcc8fc0339849e8e"));
		{
			List<VisualTimelineEntry> currentEntries = tm.getCurrentDisplayEntries();
			VisualTimelineEntry firstEntry = currentEntries.get(0);
			MatcherAssert.assertThat(firstEntry.timeUntil(), Matchers.closeTo(8.8, 0.10));
			MatcherAssert.assertThat(firstEntry.getLabel(), Matchers.equalTo("Sengender Strahl"));
		}
	}
}
