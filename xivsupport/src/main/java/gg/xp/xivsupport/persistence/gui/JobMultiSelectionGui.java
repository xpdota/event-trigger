package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.tables.renderers.AutoHeightScalingIcon;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.RenderUtils;
import gg.xp.xivsupport.gui.tables.renderers.ScaledImageComponent;
import gg.xp.xivsupport.gui.util.ColorUtils;
import gg.xp.xivsupport.gui.util.GuiUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

public class JobMultiSelectionGui extends JPanel {

	private final JobSelection jobs;

	public JobMultiSelectionGui(JobSelection jobSel) {
		this.jobs = jobSel;
		setLayout(new BorderLayout());

		JPanel mainPanel = new JPanel();
		{
			mainPanel.setLayout(new GridBagLayout());
			GridBagConstraints gbc = GuiUtil.defaultGbc();
			gbc.weighty = 0;
			for (JobType type : JobType.values()) {
				if (!jobSel.isTypeAllowed(type)) {
					continue;
				}
				gbc.gridwidth = 1;
				gbc.weightx = 0;
				gbc.gridx = 0;
				JCheckBox categoryCheckBox = new BoundCheckbox(type.getFriendlyName(), () -> jobSel.stateForCategory(type) != JobSelectionState.NOT_SELECTED, value -> {
					jobSel.changeCategoryState(type, value);
					this.repaint();
				});
				mainPanel.add(categoryCheckBox, gbc);
				gbc.gridx++;
				mainPanel.add(Box.createHorizontalStrut(20), gbc);
				Arrays.stream(Job.values()).filter(job -> job.getCategory() == type)
						.forEach(job -> {
							if (!jobSel.isJobAllowed(job)) {
								return;
							}
							gbc.gridx++;
							JobIconToggleButton jobBox = new JobIconToggleButton(job, jobSel);
							mainPanel.add(jobBox, gbc);
						});
				gbc.gridx++;
				gbc.gridwidth = GridBagConstraints.REMAINDER;
				gbc.weightx = 1;
				mainPanel.add(Box.createHorizontalGlue(), gbc);
				gbc.gridy++;
			}
			gbc.weighty = 1;
			mainPanel.add(Box.createVerticalGlue(), gbc);
		}
		add(mainPanel, BorderLayout.CENTER);

		JCheckBox allCheck = new BoundCheckbox("All Jobs", jobSel::isEnabledForAll, enabledForAll -> {
			jobSel.setEnabledForAll(enabledForAll);
			this.repaint();
		});
		add(allCheck, BorderLayout.NORTH);
		revalidate();
	}

	private class JobIconToggleButton extends JComponent {

		private final Job job;
		private final JobSelection sel;

		JobIconToggleButton(Job job, JobSelection sel) {
			this.job = job;
			this.sel = sel;
			setLayout(new BorderLayout());
			int SIZE = 40;
			ScaledImageComponent icon = IconTextRenderer.getIconOnly(job).withNewSize(SIZE);
			add(icon, BorderLayout.CENTER);
			icon.setMinimumSize(new Dimension(SIZE, SIZE));
			icon.setPreferredSize(new Dimension(SIZE, SIZE));
			setMinimumSize(new Dimension(SIZE, SIZE));
			setPreferredSize(new Dimension(SIZE, SIZE));
			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					JobSelectionState state = getState();
					boolean desiredState = state == JobSelectionState.NOT_SELECTED;
					sel.changeJobState(job, desiredState);
					JobMultiSelectionGui.this.repaint();
				}
			});
		}

		public JobSelectionState getState() {
			return sel.stateForJob(job);
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			JobSelectionState state = getState();
			// Easier to just draw the background semi-transparently over the image than to apply an alpha
			// to the image.
			if (state == JobSelectionState.NOT_SELECTED) {
				g.setColor(RenderUtils.withAlpha(getBackground(), 192));
				g.fillRect(0, 0, getWidth(), getHeight());
			}
		}
	}


}
