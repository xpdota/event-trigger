package gg.xp.xivsupport.events.triggers.jails.gui;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.events.triggers.jails.JailSolver;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.components.RearrangeableList;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.renderers.JobRenderer;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ScanMe
public class JailGui implements PluginTab {
	private static final Logger log = LoggerFactory.getLogger(JailGui.class);

	private final JailSolver jails;
	private CustomTableModel<XivPlayerCharacter> partyTableModel;

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
		partyTableModel = CustomTableModel.builder(
						jails::partyOrderPreview)
				.addColumn(new CustomColumn<>("Name", XivEntity::getName))
				.addColumn(new CustomColumn<>("Job", XivPlayerCharacter::getJob, c -> c.setCellRenderer(new JobRenderer())))
				.setItemEquivalence((a, b) -> a.getId() == b.getId() && a.getJob() == b.getJob())
				.build();

		List<Job> items = jails.getCurrentJailSort();
		RearrangeableList<Job> jobList = new RearrangeableList<>(items, l -> {
			jails.setJailSort(l);
			log.info("Changed jail prio: {}", l.stream().map(Enum::name).collect(Collectors.joining(", ")));
			partyTableModel.fullRefresh();
		});

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
		JButton resetButton = new JButton("Reset Order");
		resetButton.addActionListener(l -> {
			jails.resetJailSort();
			partyTableModel.fullRefresh();
			jobList.setValues(jails.getCurrentJailSort());
		});
		toggles.add(resetButton);
		JButton helpButton = new JButton("Help");
		helpButton.addActionListener(l -> {
			JOptionPane.showMessageDialog(SwingUtilities.getRoot(helpButton), helpText);
		});
		toggles.add(helpButton);
		toggles.add(new BooleanSettingGui(jails.getOverrideZoneLock(), "Override Zone Lock (for testing)").getComponent());


		panel.add(toggles, c);

		c.gridy++;
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
		jobList.setCellRenderer(new JobRenderer());
		JScrollPane scroll = new JScrollPane(jobList);
		Dimension size = new Dimension(100, 50);
		scroll.setMinimumSize(size);
		scroll.setPreferredSize(size);
		dragger.add(scroll);
		c.gridwidth = 1;
		c.weighty = 1;
		c.gridy++;
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
		c.gridx++;
		c.weightx = 1;
		panel.add(new JScrollPane(partyMembersTable), c);

		return panel;
	}

	@HandleEvents
	public void updatePartyList(EventContext context, XivStateRecalculatedEvent event) {
		SwingUtilities.invokeLater(() -> {
			if (partyTableModel != null) {
				partyTableModel.fullRefresh();
			}
		});
	}


	private static final String helpText = "Instructions:\n" +
			"\n" +
			"1. Check boxes for whether you would like personal callouts and/or automarks.\n" +
			"2. If you will be using automarks, be sure to configure automark hotkeys on the Automarks tab.\n" +
			"3. Drag jobs in the list to configure your priority. The party list on the right shows what the effective priority will be.\n" +
			"4. If using personal callouts, make sure everyone has the same priority (using the defaults makes this easy).\n" +
			"\n" +
			"To test automarks, you must in inside the UWU instance. Then, use the command 'amtest' like so:\n" +
			"\n" +
			"/e c:amtest 4 2 7\n" +
			"\n" +
			"where the numbers are which party slots you would like to put markers on. Make sure this works. Note that they will be marked in the order you put in the command, not the priority order.\n" +
			"\n" +
			"To test jails in general, use the command:\n" +
			"\n" +
			"/e c:jailtest 4 2 7\n" +
			"\n" +
			"where the numbers are which party slots you would like to simulate being jailed. Note that they will be marked in the chosen priority order, not the order they are written in the command. This will follow your settings for whether you want automarks and/or personal callouts (you should make sure '1' is chosen as one of the player IDs if you want to hear a personal callout).\n" +
			"\n" +
			"Finally, be sure to use '/e c:jailreset' to simulate a wipe.\n";
}
