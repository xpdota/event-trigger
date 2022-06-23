package gg.xp.xivsupport.events.triggers.easytriggers;

import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.LogLineRegexFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ActLegacyImportTest {

	@Test
	void testImport() {
		EasyTrigger<ACTLogLineEvent> trigger = ActLegacyTriggerImport.parseTriggerXml("<Trigger R=\"asdf\" SD=\"&quot;qwer&quot;\" ST=\"2\" CR=\"F\" C=\"zxcv\" T=\"F\" TN=\"tyui\" Ta=\"T\" />");
		Assert.assertEquals(trigger.getEventType(), ACTLogLineEvent.class);
		Assert.assertEquals(trigger.getText(), "\"qwer\"");
		Assert.assertEquals(trigger.getTts(), "\"qwer\"");
		Assert.assertEquals(trigger.getName(), "asdf");
		Assert.assertEquals(trigger.getConditions().size(), 1);
		LogLineRegexFilter condition = (LogLineRegexFilter) trigger.getConditions().get(0);
		Assert.assertEquals(condition.regex.pattern(), "asdf");

	}

}