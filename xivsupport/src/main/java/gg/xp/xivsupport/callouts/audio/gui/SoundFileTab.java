package gg.xp.xivsupport.callouts.audio.gui;

import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.callouts.audio.PlaySoundFileRequest;
import gg.xp.xivsupport.callouts.audio.SoundFile;
import gg.xp.xivsupport.callouts.audio.SoundFilesManager;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@ScanMe
public class SoundFileTab implements PluginTab {

	private final SoundFilesManager backend;
	private final EventMaster master;
	private CustomTableModel<SoundFile> model;
	private SoundFile selection;
	private List<SoundFile> multiSelections = Collections.emptyList();
	private Component outer;

	public SoundFileTab(SoundFilesManager backend, EventMaster master) {
		this.backend = backend;
		this.master = master;
	}

	@Override
	public String getTabName() {
		return "Audio Files";
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("Audio Files");
		outer.setLayout(new BorderLayout());
		this.outer = outer;
		model = CustomTableModel.builder(backend::getSoundFiles)
				.addColumn(new CustomColumn<>("Name", item -> item.name))
				.addColumn(new CustomColumn<>("File", item -> item.file))
				.build();
		JTable table = model.makeTable();
		table.getSelectionModel().addListSelectionListener(l -> {
			refreshSelection();
		});
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		outer.add(new JScrollPane(table), BorderLayout.CENTER);

		{
			JPanel buttonsPanel = new JPanel(new WrapLayout());
			JButton newButton = new JButton("Add New");
			newButton.addActionListener(l -> addNew());
			JButton deleteButton = new JButton("Delete") {
				@Override
				public boolean isEnabled() {
					return !multiSelections.isEmpty();
				}
			};
			deleteButton.addActionListener(l -> deleteCurrent());
			JButton changeButton = new JButton("Change File") {
				@Override
				public boolean isEnabled() {
					return singleItemSelected();
				}
			};
			changeButton.addActionListener(l -> changeFileCurrent());
			JButton playButton = new JButton("Play") {
				@Override
				public boolean isEnabled() {
					return singleItemSelected();
				}
			};
			playButton.addActionListener(l -> playFileCurrent());

			buttonsPanel.add(newButton);
			buttonsPanel.add(deleteButton);
			buttonsPanel.add(changeButton);
			buttonsPanel.add(playButton);
			outer.add(buttonsPanel, BorderLayout.SOUTH);
		}
		SwingUtilities.invokeLater(this::refresh);


		return outer;
	}

	private void playFileCurrent() {
		master.pushEvent(new PlaySoundFileRequest(selection.file.toFile()));
	}

	private boolean singleItemSelected() {
		return selection != null;
	}

	private void refresh() {
		model.signalNewData();
	}


	private void refreshSelection() {
		this.multiSelections = model.getSelectedValues();
		this.selection = multiSelections.size() == 1 ? multiSelections.get(0) : null;
		outer.repaint();
	}

	private @Nullable SoundFile makeNew() {
		String name = JOptionPane.showInputDialog("Give this sound a name");
		if (name == null || name.isBlank()) {
			return null;
		}
		// TODO: reuse instance of JFileChooser so it keeps directory?
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith(".wav");
			}

			@Override
			public String getDescription() {
				return ".wav files";
			}
		});
		fileChooser.setPreferredSize(new Dimension(800, 600));
		fileChooser.showDialog(outer, "Choose File");

		File file = fileChooser.getSelectedFile();
		if (file != null) {
			SoundFile soundFile = new SoundFile();
			soundFile.name = name;
			soundFile.file = file.toPath();
			return soundFile;
		}
		else {
			return null;
		}
	}


	public @Nullable SoundFile addNew() {
		SoundFile newFile = makeNew();
		if (newFile != null) {
			backend.addSoundFile(newFile);
			refresh();
			SwingUtilities.invokeLater(() -> {
				model.setSelectedValue(newFile);
				refreshSelection();
			});
			return newFile;
		}
		return null;
	}

	private void deleteCurrent() {
		multiSelections.forEach(backend::removeSoundFile);
		refresh();
		model.setSelectedValue(null);
	}

	private void changeFileCurrent() {
		JOptionPane.showMessageDialog(outer, "Not Implemented Yet");
	}


}
