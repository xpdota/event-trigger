package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.util.GuiUtil;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class JobMultiSelectionGui extends JPanel {

	private final JobSelection jobs;

	public JobMultiSelectionGui(JobSelection jobs) {
		this.jobs = jobs;
		setLayout(new BorderLayout());

		JPanel mainPanel = new JPanel();
		{
			mainPanel.setLayout(new GridBagLayout());
			GridBagConstraints gbc = GuiUtil.defaultGbc();
			gbc.weighty = 0;
			for (JobType type : JobType.values()) {
				if (!type.isCombatJob()) {
					continue;
				}
				gbc.gridx = 0;
				JCheckBox categoryCheckBox = new JCheckBox(type.getFriendlyName());
				mainPanel.add(categoryCheckBox, gbc);
				gbc.gridx ++;
				mainPanel.add(Box.createHorizontalStrut(20), gbc);
				Arrays.stream(Job.values()).filter(job -> job.getCategory() == type)
						.forEach(job -> {
							gbc.gridx ++;
							JCheckBox jobCheck = new JCheckBox(job.getFriendlyName());
							mainPanel.add(jobCheck, gbc);
						});
				gbc.gridy ++;
			}
			gbc.weighty = 1;
			mainPanel.add(Box.createVerticalGlue(), gbc);
		}
		add(mainPanel, BorderLayout.CENTER);

		JCheckBox allCheck = new JCheckBox("All Jobs", jobs.enabledForAll) {
			@Override
			public boolean isSelected() {
				return super.isSelected();
//				return jobs.enabledForAll;
			}
		};
		allCheck.addActionListener(l -> {
			boolean b = allCheck.isSelected();
			jobs.enabledForAll = b;
			mainPanel.setVisible(!b);

		});
		mainPanel.setVisible(!jobs.enabledForAll);
		add(allCheck, BorderLayout.NORTH);
	}


}
