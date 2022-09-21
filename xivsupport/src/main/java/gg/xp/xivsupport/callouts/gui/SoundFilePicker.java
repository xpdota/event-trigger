package gg.xp.xivsupport.callouts.gui;

import gg.xp.xivsupport.callouts.audio.SoundFile;
import gg.xp.xivsupport.callouts.audio.SoundFilesManager;
import gg.xp.xivsupport.callouts.audio.gui.SoundFileTab;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SoundFilePicker {

	private final JComboBox<String> filePicker;

	public SoundFilePicker(SoundFileTab sft, SoundFilesManager soundMgr, Supplier<String> getter, Consumer<String> setter) {
		// TODO: just plumb all this through SoundFilesManager
		filePicker = new JComboBox<>();
		ComboBoxModel<String> fpModel = new ComboBoxModel<>() {
			@Override
			public void setSelectedItem(Object item) {
				if (item.equals("Add New...")) {
					SoundFile soundFile = sft.addNew();
					if (soundFile != null) {
						setter.accept(soundFile.name);
					}
					else {
						return;
					}
				}
				else {
					if (item.equals("None")) {
						item = "";
					}
					setter.accept(item.toString());
				}
				SwingUtilities.invokeLater(filePicker::repaint);
			}

			@Override
			public Object getSelectedItem() {
				String s = getter.get();
				if (StringUtils.isEmpty(s)) {
					return "None";
				}
				return s;
			}

			@Override
			public int getSize() {
				return soundMgr.getSoundFilesAndNone().size() + 1;
			}

			@Override
			public String getElementAt(int index) {
				if (index == soundMgr.getSoundFilesAndNone().size()) {
					return "Add New...";
				}
				return soundMgr.getSoundFilesAndNone().get(index);
			}

			@Override
			public void addListDataListener(ListDataListener l) {

			}

			@Override
			public void removeListDataListener(ListDataListener l) {

			}
		};
		filePicker.setModel(fpModel);

	}

	public JComboBox<String> getPicker() {
		return filePicker;
	}
}
