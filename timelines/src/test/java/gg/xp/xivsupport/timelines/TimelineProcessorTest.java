package gg.xp.xivsupport.timelines;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.parsers.FakeACTTimeSource;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.sys.KnownLogSource;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import gg.xp.xivsupport.sys.XivMain;
import org.hamcrest.CoreMatchers;
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
			MatcherAssert.assertThat(firstEntry.timeUntil(), Matchers.closeTo(18.1, 0.1));
		}
	}
}
