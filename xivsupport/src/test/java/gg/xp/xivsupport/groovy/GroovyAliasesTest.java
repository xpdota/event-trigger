package gg.xp.xivsupport.groovy;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.sys.XivMain;
import groovy.lang.Binding;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.picocontainer.MutablePicoContainer;
import org.testng.annotations.Test;

public class GroovyAliasesTest {

	@Test
	void testAliases() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		pico.getComponent(EventDistributor.class).acceptEvent(new InitEvent());
		GroovyManager gm = pico.getComponent(GroovyManager.class);
		Binding binding = gm.makeBinding();
		MatcherAssert.assertThat(binding.getVariable("pico"), Matchers.notNullValue());
		MatcherAssert.assertThat(binding.getVariable("container"), Matchers.notNullValue());
		MatcherAssert.assertThat(binding.getVariable("picoContainer"), Matchers.notNullValue());
		MatcherAssert.assertThat(binding.getVariable("state"), Matchers.notNullValue());
		MatcherAssert.assertThat(binding.getVariable("xivState"), Matchers.notNullValue());
		MatcherAssert.assertThat(binding.getVariable("master"), Matchers.notNullValue());
		MatcherAssert.assertThat(binding.getVariable("buffs"), Matchers.notNullValue());
		MatcherAssert.assertThat(binding.getVariable("statuses"), Matchers.notNullValue());
		MatcherAssert.assertThat(binding.getVariable("casts"), Matchers.notNullValue());
		MatcherAssert.assertThat(binding.getVariable("log"), Matchers.notNullValue());
		// Test normal stuff too
		MatcherAssert.assertThat(binding.getVariable("statusEffectRepository"), Matchers.notNullValue());
	}

}
