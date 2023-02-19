package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.events.triggers.duties.ewult.omega.BooleanSettingHidingPanel;
import gg.xp.xivsupport.persistence.settings.JobSortOverrideSetting;

import javax.swing.*;

public class JobSortOverrideGui {

	private final JobSortOverrideSetting setting;
	private final JobSortGui jobSortGui;

	public JobSortOverrideGui(JobSortOverrideSetting setting) {
		this.setting = setting;
		this.jobSortGui = new JobSortGui(setting);
	}

	public void externalRefresh() {
		jobSortGui.externalRefresh();
	}

	public JPanel getCombined() {
		return new BooleanSettingHidingPanel(setting.getEnabled(), "Override Job Priority", jobSortGui.getCombined(), false);
	}
}
