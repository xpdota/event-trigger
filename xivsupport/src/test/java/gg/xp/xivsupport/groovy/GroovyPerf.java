package gg.xp.xivsupport.groovy;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.ManaPoints;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivWorld;
import gg.xp.xivsupport.sys.XivMain;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.GroovySandbox;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

public class GroovyPerf {

	private static final Logger log = LoggerFactory.getLogger(GroovyPerf.class);

	@Test
	void testBasicScript() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		pico.getComponent(EventDistributor.class).acceptEvent(new InitEvent());
		GroovyManager mgr = pico.getComponent(GroovyManager.class);
		GroovyShell shell = mgr.makeShell();
		GroovySandbox sbx = mgr.getSandbox();
		XivState state = pico.getComponent(XivState.class);
		Assert.assertNotNull(state);

		XivAbility ability = new XivAbility(123, "Foo Ability");
		XivPlayerCharacter player = new XivPlayerCharacter(0x10000001, "Me, The Player", Job.GNB, XivWorld.of(), true, 1, new HitPoints(123, 123), ManaPoints.of(123, 123), new Position(0, 0, 0, 0), 0, 0, 1, 80, 0, 0);
		XivPlayerCharacter otherCharInParty = new XivPlayerCharacter(0x10000002, "Someone Else In My Party", Job.GNB, XivWorld.of(), false, 1, new HitPoints(123, 123), ManaPoints.of(123, 123), new Position(0, 0, 0, 0), 0, 0, 1, 80, 0, 0);
		AbilityCastStart event = new AbilityCastStart(ability, player, otherCharInParty, 6.0);

		event.getAbility();
		event.abilityIdMatches(0x5EF8);
		event.getSource().getName();
		event.getTarget().getName();

		try (var ignored = sbx.enter()) {
			Script script = shell.parse("\"${event.ability}; ${event.abilityIdMatches(0x5EF8)}; ${event.source.name}; ${event.target.name}\"");
			script.getBinding().setVariable("event", event);
			timeIt("First", script::run);
			timeIt("Second", script::run);
			timeIt("Third", script::run);


			log.info("Result: {}", script.run());

		}
	}

	private static void timeIt(String label, Runnable run) {
		log.info("Starting {}", label);
		long before = System.currentTimeMillis();
		run.run();
		long after = System.currentTimeMillis();
		long delta = after - before;
		log.info("Timing for {}: {}", label, delta);
	}


}
