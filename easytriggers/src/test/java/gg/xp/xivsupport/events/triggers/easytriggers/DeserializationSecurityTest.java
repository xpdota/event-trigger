package gg.xp.xivsupport.events.triggers.easytriggers;

import gg.xp.xivsupport.events.ExampleSetup;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.picocontainer.MutablePicoContainer;
import org.testng.annotations.Test;

public class DeserializationSecurityTest {

	/**
	 * Test that if someone tries to import an object where we use Jackson @class to determine the type,
	 * you can't maliciously (or accidentally) put something inappropriate in its place.
	 */
	@Test
	void testSecurity() {
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		pers.save("easy-triggers.my-triggers", triggerDataNew);

		MutablePicoContainer pico = ExampleSetup.setup(pers);

		EasyTriggers ez = pico.getComponent(EasyTriggers.class);
		MatcherAssert.assertThat(ez.getTriggers(), Matchers.empty());
	}

	private static final String triggerDataNew = """
			[
			  {
			    "enabled": true,
			    "eventType": "gg.xp.xivsupport.events.actlines.events.AbilityCastStart",
			    "conditions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.DeserializationDummyClass"
			      }
			    ]
			  }
						]
						""";
}
