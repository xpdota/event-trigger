package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.components.RearrangeableList;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.filters.ValidationError;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.JobRenderer;
import gg.xp.xivsupport.gui.util.EasyAction;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.settings.JobSortSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JobSortGui {

	private static final Logger log = LoggerFactory.getLogger(JobSortGui.class);
	private final JScrollPane jobListPane;
	private final JScrollPane partyPane;
	private final JButton resetButton;
	private final JButton congaButton;
	private final JButton exportButton;
	private final JButton importButton;
	private final RearrangeableList<Job> jobList;
	private final JobSortSetting sorter;

	private final CustomTableModel<XivPlayerCharacter> partyTableModel;

	public JobSortGui(JobSortSetting sorter) {

		this.sorter = sorter;
		// Defined first so we can trigger refresh
		partyTableModel = CustomTableModel.builder(
						sorter::partyOrderPreview)
				.addColumn(new CustomColumn<>("Name", XivEntity::getName))
				.addColumn(new CustomColumn<>("Job", XivPlayerCharacter::getJob, c -> c.setCellRenderer(new JobRenderer())))
				.setItemEquivalence((a, b) -> a.getId() == b.getId() && a.getJob() == b.getJob())
				.build();

		List<Job> items = sorter.getJobOrder();
		jobList = new RearrangeableList<>(items, l -> {
			sorter.setJobOrder(l);
			log.info("Changed jail prio: {}", l.stream().map(Enum::name).collect(Collectors.joining(", ")));
			partyTableModel.fullRefresh();
		});
		jobList.setCellRenderer(new JobRenderer());
		jobListPane = new JScrollPane(jobList);
		resetButton = new EasyAction("Reset", () -> {
			sorter.resetJobOrder();
			resetView();
		}).asButton();
		importButton = new EasyAction("Import", this::doImport).asButton();
		exportButton = new EasyAction("Export", this::doExport).asButton();
		congaButton = new EasyAction("Conga", this::doConga).asButton();
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

	private void resetView() {
		partyTableModel.fullRefresh();
		jobList.setValues(sorter.getJobOrder());
	}

	public JScrollPane getJobListPane() {
		return jobListPane;
	}

	@Deprecated // Use getJobListWithAllButtons() or getCombined() instead
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
		top.setLayout(new GridLayout(2, 2));
		top.add(exportButton);
		top.add(importButton);
		top.add(congaButton);
		top.add(resetButton);
		panel.add(top, BorderLayout.NORTH);
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

	private void doImport() {
		List<Job> newJobOrder = GuiUtil.doImportDialog("Import Job Order", s -> {
			try {
				List<Job> jobs = Arrays.stream(s.strip().split(","))
						.map(Job::valueOf)
						.toList();
				this.sorter.validateJobSortOrder(jobs);
				return jobs;
			}
			catch (Throwable t) {
				throw new ValidationError("Bad job order: " + t.getMessage(), t);
			}
		});
		this.sorter.setJobOrder(newJobOrder);
		resetView();
	}

	private void doExport() {
		String exportedJobList = this.sorter.getJobOrder().stream().map(Enum::name).collect(Collectors.joining(","));
		GuiUtil.copyTextToClipboard(exportedJobList);
		JOptionPane.showMessageDialog(jobList, "Copied to clipboard");
	}

	private void doConga() {
		int input = JOptionPane.showConfirmDialog(jobListPane, "Have your party line up from West to East in the desired priority order.", "Conga Line Instructions", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
		if (input != JOptionPane.OK_OPTION) {
			return;
		}
		List<XivPlayerCharacter> party = this.sorter.partyOrderPreview();
		Set<Job> seen = EnumSet.noneOf(Job.class);
		List<Job> jobs = party.stream()
				.sorted(Comparator.comparing(xpc -> xpc.getPos().x()))
				.map(XivPlayerCharacter::getJob)
				.filter(seen::add)
				.toList();
		JPanel jobIconList = new JPanel();
		jobIconList.setLayout(new BoxLayout(jobIconList, BoxLayout.LINE_AXIS));
		jobs.stream()
				.map(job -> IconTextRenderer.getIconOnly(job).cloneThis())
				.forEach(jobIconList::add);
		JPanel outer = new JPanel();
//		outer.setLayout(new BoxLayout(outer, BoxLayout.PAGE_AXIS));
		outer.setLayout(new BorderLayout());
		outer.add(new JLabel("Accept this priority?"), BorderLayout.NORTH);
		outer.add(jobIconList, BorderLayout.CENTER);
		int confirmation = JOptionPane.showConfirmDialog(jobListPane, outer, "Accept New Priority?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (confirmation == JOptionPane.YES_OPTION) {
			this.sorter.setJobOrderPartial(jobs);
		}
		resetView();
	}
}
