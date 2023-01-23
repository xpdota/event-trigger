package gg.xp.telestosupport.easytriggers.gui;

import gg.xp.telestosupport.easytriggers.TelestoCircleDoodleAction;
import gg.xp.telestosupport.easytriggers.TelestoLineDoodleAction;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui.GenericFieldEditor;
import org.picocontainer.PicoContainer;

import java.lang.reflect.Field;

public class TelestoLineDoodleEditor extends BaseTelestoActionEditor {

	public TelestoLineDoodleEditor(TelestoLineDoodleAction action, PicoContainer pico) {
		super("Telesto Line Doodle", action, pico);
		try {
			add(new GenericFieldEditor(action, pico, new Field[]{
					TelestoLineDoodleAction.class.getField("thickness")
//					TelestoLineDoodleAction.class.getField("filled")
			}));
		}
		catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
		add(new TelestoLocationEditor("Start", action.start));
		add(new TelestoLocationEditor("End", action.start));
	}

}
