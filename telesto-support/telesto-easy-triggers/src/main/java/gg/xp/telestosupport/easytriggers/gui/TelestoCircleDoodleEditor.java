package gg.xp.telestosupport.easytriggers.gui;

import gg.xp.telestosupport.easytriggers.TelestoCircleDoodleAction;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui.GenericFieldEditor;
import org.picocontainer.PicoContainer;

import java.lang.reflect.Field;

public class TelestoCircleDoodleEditor extends BaseTelestoDoodleActionEditor {

	public TelestoCircleDoodleEditor(TelestoCircleDoodleAction action, PicoContainer pico) {
		super("Telesto Circle Doodle", action, pico);
		try {
			add(new GenericFieldEditor(action, pico, new Field[]{
					TelestoCircleDoodleAction.class.getField("radius"),
					TelestoCircleDoodleAction.class.getField("filled"),
					TelestoCircleDoodleAction.class.getField("system")
			}));
		}
		catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
		add(new TelestoLocationEditor("Location", action.location, pico));
	}

}
