package gg.xp.telestosupport.easytriggers.gui;

import gg.xp.telestosupport.easytriggers.TelestoTextDoodleAction;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui.GenericFieldEditor;
import org.picocontainer.PicoContainer;

import java.lang.reflect.Field;

public class TelestoTextDoodleEditor extends BaseTelestoActionEditor {

	public TelestoTextDoodleEditor(TelestoTextDoodleAction action, PicoContainer pico) {
		super("Telesto Text Doodle", action, pico);
		try {
			add(new GenericFieldEditor(action, pico, new Field[]{
					TelestoTextDoodleAction.class.getField("textSize"),
					TelestoTextDoodleAction.class.getField("textScript")
			}));
		}
		catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
		add(new TelestoLocationEditor("Location", action.location));
	}

}
