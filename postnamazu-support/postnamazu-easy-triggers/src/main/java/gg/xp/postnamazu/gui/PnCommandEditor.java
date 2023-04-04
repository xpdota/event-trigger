package gg.xp.postnamazu.gui;

import gg.xp.postnamazu.PnCommandAction;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui.GenericFieldEditor;
import gg.xp.xivsupport.gui.TitleBorderPanel;
import org.picocontainer.PicoContainer;

import javax.swing.*;
import java.lang.reflect.Field;

public class PnCommandEditor extends TitleBorderPanel {
	public PnCommandEditor(PnCommandAction action, PicoContainer pico) {
		super("PostNamazu Game Command");
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		try {
			add(new GenericFieldEditor(action, pico, new Field[]{
					PnCommandAction.class.getField("textScript"),
			}));
			add(new GenericFieldEditor(action, pico, new Field[]{
					PnCommandAction.class.getField("calloutReplacements"),
					PnCommandAction.class.getField("globalReplacements"),
			}));
		}
		catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
}
