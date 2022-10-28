package gg.xp.xivsupport.events.triggers.jails.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.events.triggers.jails.JailSolver;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.extra.DutyPluginTab;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.JobSortGui;
import gg.xp.xivsupport.persistence.settings.JobSortSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class JailGui implements DutyPluginTab {
	private static final Logger log = LoggerFactory.getLogger(JailGui.class);

	private final JailSolver jails;
	private final JobSortSetting sorter;
	private JobSortGui jobSortGui;
	private JPanel panel;

	public JailGui(JailSolver jails) {

		this.jails = jails;
		this.sorter = jails.getSort();
	}

	@Override
	public String getTabName() {
		return "Titan Gaols";
	}

	@Override
	public Component getTabContents() {

		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;

		jobSortGui = new JobSortGui(sorter);
		RefreshLoop<JobSortGui> refresher = new RefreshLoop<>("JailRefresh", jobSortGui, JobSortGui::externalRefresh, unused -> 10_000L);
		TitleBorderFullsizePanel panel = new TitleBorderFullsizePanel("Jails") {
			@Override
			public void setVisible(boolean aFlag) {
				super.setVisible(aFlag);
				if (aFlag) {
					jobSortGui.externalRefresh();
					refresher.startIfNotStarted();
				}
			}
		};

		panel.setLayout(new GridBagLayout());

		JPanel toggles = new JPanel();
		toggles.setAlignmentX(0);
		toggles.setLayout(new WrapLayout(FlowLayout.LEFT));
		toggles.add(new BooleanSettingGui(jails.getEnableTts(), "Enable Personal Callout").getComponent());
		toggles.add(new BooleanSettingGui(jails.getEnableAutomark(), "Enable Automarks").getComponent());
		toggles.add(jobSortGui.getResetButton());
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

		c.gridwidth = 1;
		c.weighty = 1;
		c.gridy++;
		c.weightx = 0;
		panel.add(jobSortGui.getJobListWithButtons(), c);


		c.gridx++;
		c.weightx = 1;
		panel.add(jobSortGui.getPartyPane(), c);

		this.panel = panel;

		return panel;
	}

	@Override
	public KnownDuty getDuty() {
		return KnownDuty.UWU;
	}

//	// TODO: this should only happen on a party/job/etc update, not a normal state recalc, but it's difficult to
//	// determine exactly what should trigger it. Maybe better to just stick it on a timer that only applies when the
//	// tab is visible?
//	@HandleEvents(order = 20_000)
//	public void updatePartyList(EventContext context, XivStateRecalculatedEvent event) {
//		if (jobSortGui != null) {
//			jobSortGui.externalRefresh();
//		}
//	}


	private static final String helpText = """
			Instructions:

			1. Check boxes for whether you would like personal callouts and/or automarks.
			2. If you will be using automarks, be sure to configure automark hotkeys on the Automarks tab.
			3. Drag jobs in the list to configure your priority. The party list on the right shows what the effective priority will be.
			4. If using personal callouts, make sure everyone has the same priority (using the defaults makes this easy).
			5. Also be sure to check Advanced > Party to make sure the party sort is correct (it should match the in-game party list, not your Gaol priority).
			6. If using Automarks, also check the Help button on the Automarks if you need help setting up the macros.

			To test automarks, you must in inside the UWU instance, or check the 'Override Zone Lock' option. Then, use the command 'amtest' like so:

			/e c:amtest 4 2 7

			where the numbers are which party slots you would like to put markers on. Make sure this works. Note that they will be marked in the order you put in the command, not the priority order.

			To test jails in general, use the command:

			/e c:jailtest 4 2 7

			where the numbers are which party slots you would like to simulate being jailed. Note that they will be marked in the chosen priority order, not the order they are written in the command. This will follow your settings for whether you want automarks and/or personal callouts (you should make sure '1' is chosen as one of the player IDs if you want to hear a personal callout).

			Finally, be sure to use '/e c:jailreset' to simulate a wipe.

			If you are not in-instance, you can check the 'Override Zone Lock' checkbox, and then do /e c:jailtest 1 1 1 to simulate getting all three jails on yourself.""";
}
