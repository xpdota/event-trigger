package gg.xp.telestosupport.easytriggers.gui;

import gg.xp.telestosupport.easytriggers.BaseTelestoDoodleAction;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui.GenericFieldEditor;
import gg.xp.xivsupport.gui.TitleBorderPanel;
import org.picocontainer.PicoContainer;

import javax.swing.*;
import java.lang.reflect.Field;

public abstract class BaseTelestoDoodleActionEditor extends TitleBorderPanel {
	protected BaseTelestoDoodleActionEditor(String title, BaseTelestoDoodleAction action, PicoContainer container) {
		super(title);
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		try {
			add(new GenericFieldEditor(action, container, new Field[]{
					BaseTelestoDoodleAction.class.getField("color"),
					BaseTelestoDoodleAction.class.getField("name"),
					BaseTelestoDoodleAction.class.getField("duration")
			}));
		}
		catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
}
