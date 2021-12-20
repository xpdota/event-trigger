package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivdata.jobs.Cooldown;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivdata.jobs.JobType;
import gg.xp.xivsupport.gui.NoCellEditor;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.JobRenderer;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import gg.xp.xivsupport.persistence.gui.LongSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.persistence.settings.LongSetting;

import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseCdTrackerGui implements PluginTab {

	protected abstract LongSetting cdAdvance();

	protected abstract BooleanSetting enableTts();

	protected abstract IntSetting overlayMax();

	protected abstract Map<Cooldown, BooleanSetting> cds();

	// TODO: here's how I can do settings in a reasonable way:
	// Have a gui with 5 or so checkboxes (all on/off, this, DPS, healer, tank)
	// Let each callout be selectable (including ctrl/shift)
	// Then cascade changes to multiple callouts


	// New idea: list them in a table, also have icons for job and ability

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel outerPanel = new TitleBorderFullsizePanel("Party Cooldowns");
		outerPanel.setLayout(new BorderLayout());

		JPanel settingsPanel = new JPanel();
		settingsPanel.setLayout(new WrapLayout());

		JPanel preTimeBox = new LongSettingGui(cdAdvance(), "Time before expiry to call out (milliseconds)").getComponent();
		settingsPanel.add(preTimeBox);
		JCheckBox enableTts = new BooleanSettingGui(enableTts(), "Enable TTS").getComponent();
		settingsPanel.add(enableTts);
		JPanel numSetting = new IntSettingSpinner(overlayMax(), "Max in Overlay").getComponent();
		settingsPanel.add(numSetting);

		outerPanel.add(settingsPanel, BorderLayout.PAGE_START);

		JPanel innerPanel = new JPanel();
		innerPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		Map<Cooldown, BooleanSetting> cooldowns = cds();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.ipadx = 50;
		c.gridy = 0;
		// TODO: idea for how to do separate TTS/visual plus icons
		// Instead of one checkbox per ability, just have one for TTS, and one for visual, and then
		// have a label with icon and text.
		// Alternatively, have a table with a bunch of checkbox columns

		List<Cooldown> sortedCds = new ArrayList<>(cds().keySet());
		sortedCds.sort(Comparator.comparing(cd -> {
			Job job = cd.getJob();
			// Sort job categories first
			if (job == null) {
				return cd.getJobType().ordinal();
			}
			return job.defaultPartySortOrder();
		}));
		JTable table = new JTable() {
			@Override
			public boolean isCellEditable(int row, int column) {
				// TODO: make this more official
				if (getCellEditor(row, column) instanceof NoCellEditor) {
					return false;
				}
				return true;
			}
		};
		CustomTableModel<Cooldown> model = CustomTableModel.builder(() -> sortedCds)
				.addColumn(StandardColumns.booleanSettingColumn("TTS", cd -> cooldowns.get(cd)))
				.addColumn(new CustomColumn<>("Job", Function.identity(),
						col -> {
							col.setCellRenderer(new JobRenderer());
							col.setMinWidth(60);
							col.setMaxWidth(60);
						}))
				.build();

		table.setModel(model);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		model.configureColumns(table);

//		jobTypeKeys.forEach((job) -> {
//			List<Cooldown> cooldownsForJob = byJobType.get(job);
//			c.gridwidth = 1;
//			c.gridx = 0;
//			c.weightx = 0;
//			// left filler
//			innerPanel.add(new JPanel());
//			c.gridx ++;
//
//			JLabel label = new JLabel(job.getFriendlyName());
//			innerPanel.add(label, c);
//			cooldownsForJob.forEach(dot -> {
//				c.gridx++;
//
//				BooleanSetting setting = cooldowns.get(dot);
//				JCheckBox checkbox = new BooleanSettingGui(setting, dot.getLabel()).getComponent();
//				innerPanel.add(checkbox, c);
//			});
//			c.gridx++;
//			c.weightx = 1;
//			c.gridwidth = GridBagConstraints.REMAINDER;
//			// Add dummy to pad out the right side
//			JPanel dummyPanel = new JPanel();
//			innerPanel.add(dummyPanel, c);
//			c.gridy++;
//		});
//		JLabel templateLabel = new JLabel();
//		jobKeys.forEach((job) -> {
//			List<Cooldown> cooldownsForJob = byJob.get(job);
//			c.gridwidth = 1;
//			c.gridx = 0;
//			c.weightx = 0;
//			// left filler
//			innerPanel.add(new JPanel());
//			c.gridx ++;
//			innerPanel.add(new IconTextRenderer.getIconOnly(job))
////			JLabel label = new JLabel(job.getFriendlyName());
////			innerPanel.add(label, c);
//			cooldownsForJob.forEach(dot -> {
//				c.gridx++;
//
//				BooleanSetting setting = cooldowns.get(dot);
//				JCheckBox checkbox = new BooleanSettingGui(setting, dot.getLabel()).getComponent();
//				innerPanel.add(checkbox, c);
//			});
//			c.gridx++;
//			c.weightx = 1;
//			c.gridwidth = GridBagConstraints.REMAINDER;
//			// Add dummy to pad out the right side
//			JPanel dummyPanel = new JPanel();
//			innerPanel.add(dummyPanel, c);
//			c.gridy++;
//		});
//		c.weighty = 1;
//		innerPanel.add(new JPanel(), c);
//		innerPanel.setPreferredSize(innerPanel.getMinimumSize());
//		JScrollPane scroll = new JScrollPane(innerPanel);
		JScrollPane scroll = new JScrollPane(table);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		outerPanel.add(scroll);
		return outerPanel;
	}
}
