package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.components.RearrangeableList;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.filters.ValidationError;
import gg.xp.xivsupport.gui.tables.renderers.JobRenderer;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.settings.JobSortSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JobSortGui {

	private static final Logger log = LoggerFactory.getLogger(JobSortGui.class);
	private final JScrollPane jobListPane;
	private final JScrollPane partyPane;
	private final JButton resetButton;
	private final JButton exportButton;
	private final JButton importButton;

	private CustomTableModel<XivPlayerCharacter> partyTableModel;

	public JobSortGui(JobSortSetting sorter) {

		// Defined first so we can trigger refresh
		partyTableModel = CustomTableModel.builder(
						sorter::partyOrderPreview)
				.addColumn(new CustomColumn<>("Name", XivEntity::getName))
				.addColumn(new CustomColumn<>("Job", XivPlayerCharacter::getJob, c -> c.setCellRenderer(new JobRenderer())))
				.setItemEquivalence((a, b) -> a.getId() == b.getId() && a.getJob() == b.getJob())
				.build();

		List<Job> items = sorter.getJobOrder();
		RearrangeableList<Job> jobList = new RearrangeableList<>(items, l -> {
			sorter.setJobOrder(l);
			log.info("Changed jail prio: {}", l.stream().map(Enum::name).collect(Collectors.joining(", ")));
			partyTableModel.fullRefresh();
		});
		resetButton = new JButton("Reset Order");
		resetButton.addActionListener(l -> {
			sorter.resetJobOrder();
			partyTableModel.fullRefresh();
			jobList.setValues(sorter.getJobOrder());
		});
		importButton = new JButton("Import");
		importButton.addActionListener(l -> {
			List<Job> newJobOrder = GuiUtil.doImportDialog("Import Job Order", s -> {
				try {
					List<Job> jobs = Arrays.stream(s.strip().split(","))
							.map(Job::valueOf)
							.toList();
					sorter.validateJobSortOrder(jobs);
					return jobs;
				}
				catch (Throwable t) {
					throw new ValidationError("Bad job order: " + t.getMessage(), t);
				}
			});
			sorter.setJobOrder(newJobOrder);
			partyTableModel.fullRefresh();
			jobList.setValues(sorter.getJobOrder());
		});
		exportButton = new JButton("Export");
		exportButton.addActionListener(l -> {
			String exportedJobList = sorter.getJobOrder().stream().map(Enum::name).collect(Collectors.joining(","));
			GuiUtil.copyTextToClipboard(exportedJobList);
			JOptionPane.showMessageDialog(exportButton, "Copied to clipboard");
		});
		jobList.setCellRenderer(new JobRenderer());
		jobListPane = new JScrollPane(jobList);
		Dimension size = new Dimension(100, 50);
		jobListPane.setMinimumSize(size);
		jobListPane.setPreferredSize(size);
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
		this.partyPane = new JScrollPane(partyMembersTable);
	}

	public JScrollPane getJobListPane() {
		return jobListPane;
	}

	public JPanel getJobListWithButtons() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(jobListPane, BorderLayout.CENTER);
		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));
		top.add(exportButton);
		top.add(importButton);
		panel.add(top, BorderLayout.NORTH);
		return panel;
	}

	public JPanel getJobListWithAllButtons() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(jobListPane, BorderLayout.CENTER);
		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));
		top.add(exportButton);
		top.add(importButton);
		panel.add(top, BorderLayout.NORTH);
		panel.add(resetButton, BorderLayout.SOUTH);
		return panel;
	}

	public JScrollPane getPartyPane() {
		return partyPane;
	}

	public JButton getResetButton() {
		return resetButton;
	}

	public void externalRefresh() {
		SwingUtilities.invokeLater(() -> {
			if (partyTableModel != null) {
				partyTableModel.fullRefresh();
			}
		});
	}

	public JPanel getCombined() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(getJobListWithAllButtons(), BorderLayout.WEST);
		panel.add(getPartyPane(), BorderLayout.CENTER);
		return panel;
	}
}
