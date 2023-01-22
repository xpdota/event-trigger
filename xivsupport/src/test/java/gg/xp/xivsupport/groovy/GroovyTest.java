package gg.xp.xivsupport.groovy;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.sys.XivMain;
import groovy.lang.GroovyShell;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.GroovySandbox;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class GroovyTest {


	@Test
	void testBasicScript() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		pico.getComponent(EventDistributor.class).acceptEvent(new InitEvent());
		GroovyManager mgr = pico.getComponent(GroovyManager.class);
		GroovyShell shell = mgr.makeShell();
		GroovySandbox sbx = mgr.getSandbox();
		XivState state = pico.getComponent(XivState.class);
		Assert.assertNotNull(state);

		try (var ignored = sbx.enter()) {
			MatcherAssert.assertThat(shell.parse("1 + 2").run(), Matchers.equalTo(3));
			MatcherAssert.assertThat(shell.parse("xivState").run(), Matchers.equalTo(state));
			MatcherAssert.assertThat(shell.parse("AbilityCastStart.class").run(), Matchers.equalTo(AbilityCastStart.class));
		}
	}
}
