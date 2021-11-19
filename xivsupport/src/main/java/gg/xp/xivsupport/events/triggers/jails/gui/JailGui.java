package gg.xp.xivsupport.events.triggers.jails.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.jails.JailSolver;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.components.RearrangeableList;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.renderers.HpRenderer;
import gg.xp.xivsupport.gui.tables.renderers.JobRenderer;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ScanMe
public class JailGui implements PluginTab {
	private static final Logger log = LoggerFactory.getLogger(JailGui.class);

	private final JailSolver jails;

	public JailGui(JailSolver jails) {

		this.jails = jails;
	}

	@Override
	public String getTabName() {
		return "Titan Gaols";
	}

	@Override
	public Component getTabContents() {
		// Defined first so we can trigger refresh
		CustomTableModel<XivPlayerCharacter> partyTableModel;
		partyTableModel = CustomTableModel.builder(
						jails::partyOrderPreview)
				.addColumn(new CustomColumn<>("Name", XivEntity::getName))
				.addColumn(new CustomColumn<>("Job", XivPlayerCharacter::getJob, c -> c.setCellRenderer(new JobRenderer())))
				.build();
		TitleBorderFullsizePanel panel = new TitleBorderFullsizePanel("Jails");


		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;

		JPanel toggles = new JPanel();
		toggles.setAlignmentX(0);
		toggles.setLayout(new WrapLayout(FlowLayout.LEFT));
		toggles.add(new BooleanSettingGui(jails.getEnableTts(), "Enable Personal Callout").getComponent());
		toggles.add(new BooleanSettingGui(jails.getEnableAutomark(), "Enable Automarks").getComponent());
		panel.add(toggles, c);

		c.gridy ++;
		JTextArea instructions = new JTextArea();
		instructions.setText("Instructions: Drag Jobs/Classes on the left to the desired order.\nThe list on the right reflects the current party with your custom order applied.");
		instructions.setEditable(false);
		instructions.setBorder(null);
		instructions.setOpaque(false);
		instructions.setWrapStyleWord(true);
		instructions.setLineWrap(true);
		instructions.setFocusable(false);
		panel.add(instructions, c);


		JPanel dragger = new JPanel();
		dragger.setBorder(new LineBorder(Color.PINK));
		dragger.setAlignmentX(0);
		dragger.setLayout(new FlowLayout(FlowLayout.LEFT));
		List<Job> items = jails.getCurrentJailSort();
		RearrangeableList<Job> jobList = new RearrangeableList<>(items, l -> {
			jails.setCurrentJailSort(l);
			log.info("Changed jail prio: {}", l.stream().map(Enum::name).collect(Collectors.joining(", ")));
			partyTableModel.fullRefresh();
		});
		jobList.setCellRenderer(new JobRenderer());
		JScrollPane scroll = new JScrollPane(jobList);
		Dimension size = new Dimension(100, 50);
		scroll.setMinimumSize(size);
		scroll.setPreferredSize(size);
		dragger.add(scroll);
		c.gridwidth = 1;
		c.weighty = 1;
		c.gridy ++;
		c.weightx = 0;
		panel.add(scroll, c);



		JTable partyMembersTable = new JTable(8, 3);
		partyMembersTable.setModel(partyTableModel);
		partyTableModel.configureColumns(partyMembersTable);
		partyMembersTable.getSelectionModel().addListSelectionListener(l -> {
			int[] selectedRows = partyMembersTable.getSelectedRows();
			jobList.clearSelection();
			Arrays.stream(selectedRows).forEach(partyRow -> {
				XivPlayerCharacter player = partyTableModel.getValueForRow(partyRow);
				int jobRow = jobList.getValues().indexOf(player.getJob());
				jobList.addSelectionInterval(jobRow, jobRow);

			});
		});
		c.gridx ++;
		c.weightx = 1;
		panel.add(new JScrollPane(partyMembersTable), c);

		return panel;
	}
}
