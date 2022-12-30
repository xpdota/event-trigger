package gg.xp.xivsupport.events.triggers.easytriggers.actions.gui;

import gg.xp.xivsupport.callouts.audio.SoundFilesManager;
import gg.xp.xivsupport.callouts.audio.gui.SoundFileTab;
import gg.xp.xivsupport.callouts.gui.SoundFilePicker;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.SoundAction;

import javax.swing.*;
import java.awt.*;

public class SoundActionEditor extends JPanel {

	public SoundActionEditor(SoundFilesManager sfm, SoundFileTab sft, SoundAction action) {
		setLayout(new BorderLayout());
		JComboBox<String> filePicker = new SoundFilePicker(sft, sfm, () -> action.sound, file -> action.sound = file).getPicker();
		add(filePicker, BorderLayout.WEST);
//		add(Box.createHorizontalGlue());
	}


}
