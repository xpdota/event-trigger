package gg.xp.telestosupport.easytriggers.gui;

import gg.xp.telestosupport.easytriggers.TelestoImageDoodleAction;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui.GenericFieldEditor;
import org.picocontainer.PicoContainer;

import java.lang.reflect.Field;

public class TelestoImageDoodleEditor extends BaseTelestoDoodleActionEditor {

	public TelestoImageDoodleEditor(TelestoImageDoodleAction action, PicoContainer pico) {
		super("Telesto Image Doodle", action, pico);
		try {
			add(new GenericFieldEditor(action, pico, new Field[]{
					TelestoImageDoodleAction.class.getField("hAlign"),
					TelestoImageDoodleAction.class.getField("vAlign"),
			}));
		}
		catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
		add(new TelestoImageSpecEditor("Icon", action.iconSpec));
		add(new TelestoLocationEditor("Location", action.location, pico));
	}

}
