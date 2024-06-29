package gg.xp.xivsupport.events.actlines;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.events.actlines.parsers.AbstractACTLineParser;
import gg.xp.xivsupport.sys.XivMain;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.picocontainer.MutablePicoContainer;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Locale;

// Class to test that you don't forget to update the line number when copying and pasting a LineXXParser class
public class ClassNameTest {

	@SuppressWarnings("rawtypes")
	@Test
	void testClassNames() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		pico.getComponent(EventDistributor.class).acceptEvent(new InitEvent());
		List<AbstractACTLineParser> parsers = pico.getComponents(AbstractACTLineParser.class);
		for (AbstractACTLineParser<?> parser : parsers) {
			int lineNumber = parser.getLineNumber();
			String actualName = parser.getClass().getSimpleName();
			// Ignore legacy classes
			if (actualName.toLowerCase(Locale.ROOT).contains("legacy")) {
				continue;
			}
			String expectedName;
			if (lineNumber < 100) {
				expectedName = String.format("Line%02dParser", lineNumber);
			}
			else {
				expectedName = String.format("Line%sParser", lineNumber);
			}
			MatcherAssert.assertThat("Line number should match class name", actualName, Matchers.equalTo(expectedName));
		}
	}

}
