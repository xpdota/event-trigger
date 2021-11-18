package gg.xp.xivsupport.events.state;

import gg.xp.xivsupport.events.actlines.AbstractACTLineTest;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.BarrierUpEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyInitialCommenceEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyRecommenceEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.FadeInEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.FadeOutEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.VictoryEvent;
import org.testng.annotations.Test;

public class ActorControlTests {
	private static final String lineTemplate = "33|2021-04-26T17:23:28.6780000-04:00|80034E6C|%x|B5D|1234|5678|ABCD|f777621829447c53c82c9a24aa25348f";

	private static String lineForCmd(long cmd) {
		return String.format(lineTemplate, cmd);
	}

	@Test
	void testDutyInitialCommence() {
		new AbstractACTLineTest<>(DutyInitialCommenceEvent.class)
				.expectEvent(lineForCmd(0x40000001L));
	}

	@Test
	void testDutyRecommence() {
		new AbstractACTLineTest<>(DutyRecommenceEvent.class)
				.expectEvent(lineForCmd(0x40000006L));
	}

	@Test
	void testFadeOut() {
		new AbstractACTLineTest<>(FadeOutEvent.class)
				.expectEvent(lineForCmd(0x40000005L));
	}

	@Test
	void testFadeIn() {
		new AbstractACTLineTest<>(FadeInEvent.class)
				.expectEvent(lineForCmd(0x40000010L));
		new AbstractACTLineTest<>(WipeEvent.class)
				.expectEvent(lineForCmd(0x40000010L));
	}

	@Test
	void testBarrierUp() {
		new AbstractACTLineTest<>(BarrierUpEvent.class)
				.expectEvent(lineForCmd(0x40000012L));
	}

	@Test
	void testVictory() {
		new AbstractACTLineTest<>(VictoryEvent.class)
				.expectEvent(lineForCmd(0x40000003L));
	}

}
