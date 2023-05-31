package gg.xp.telestosupport.easytriggers.gui;

import gg.xp.telestosupport.easytriggers.TelestoCommandAction;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui.GenericFieldEditor;
import gg.xp.xivsupport.gui.TitleBorderPanel;
import org.picocontainer.PicoContainer;

import javax.swing.*;
import java.lang.reflect.Field;

public class TelestoCommandEditor extends TitleBorderPanel {
	public TelestoCommandEditor(TelestoCommandAction action, PicoContainer pico) {
		super("Telesto Game Command");
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		try {
			add(new GenericFieldEditor(action, pico, new Field[]{
					TelestoCommandAction.class.getField("textScript"),
			}));
			add(new GenericFieldEditor(action, pico, new Field[]{
					TelestoCommandAction.class.getField("calloutReplacements"),
					TelestoCommandAction.class.getField("globalReplacements"),
			}));
		}
		catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
}
