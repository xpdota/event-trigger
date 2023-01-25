package gg.xp.xivsupport.events.triggers.duties.ewult;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.DutyPluginTab;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.persistence.gui.JobSortGui;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class OmegaUltimateGroupPrioGui implements DutyPluginTab {

	private final OmegaUltimate backend;
	private JobSortGui jsg;
	private JPanel inner;

	public OmegaUltimateGroupPrioGui(OmegaUltimate backend) {
		this.backend = backend;
	}

	@Override
	public KnownDuty getDuty() {
		return KnownDuty.OmegaProtocol;
	}

	@Override
	public String getTabName() {
		return "Group Swap Priority";
	}

	@Override
	public int getSortOrder() {
		return 101;
	}

	@Override
	public Component getTabContents() {
		jsg = new JobSortGui(backend.getGroupPrioJobSort());
		RefreshLoop<JobSortGui> refresher = new RefreshLoop<>("OmegaAmRefresh", jsg, JobSortGui::externalRefresh, unused -> 10_000L);
		TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("Group Prio") {
			@Override
			public void setVisible(boolean aFlag) {
				super.setVisible(aFlag);
				if (aFlag) {
					jsg.externalRefresh();
					refresher.startIfNotStarted();
				}
			}
		};
		outer.setLayout(new BorderLayout());
		JPanel topCheckboxes = new JPanel();
		topCheckboxes.setLayout(new BoxLayout(topCheckboxes, BoxLayout.PAGE_AXIS));
//		JCheckBox p5marks = new BooleanSettingGui(ds.getP5_thunderstruckAutoMarks(), "P5 Thunderstruck Automarks").getComponent();
//		JCheckBox p6marks = new BooleanSettingGui(ds.getP6_useAutoMarks(), "P6 Wroth Flames Automarks").getComponent();
//		topCheckboxes.add(p5marks);
//		topCheckboxes.add(p6marks);

		outer.add(topCheckboxes, BorderLayout.NORTH);
		GridBagConstraints c = new GridBagConstraints(0, 0, 2, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);

		inner = new JPanel();
		inner.setLayout(new GridBagLayout());
//		JCheckBox p6altMark = new BooleanSettingGui(ds.getP6_altMarkMode(), "Alt Mode").getComponent();
//		JCheckBox rotFirst = new BooleanSettingGui(ds.getP6_rotPrioHigh(), "Rot takes highest priority").getComponent();
//		JCheckBox reverseSort = new BooleanSettingGui(ds.getP6_reverseSort(), "Reverse sort (higher priority gets larger number)").getComponent();
		ReadOnlyText helpText = new ReadOnlyText("""
				Instructions:
				Jobs higher on the list will be preferred for group 1.
				Jobs lower on the list will be preferred for group 2.
				The easiest way to set this up is to simply put your group 1 jobs at the top,
				and your group 2 jobs at the bottom.
				""");


//		inner.add(p6altMark, c);
//		c.gridy++;
		inner.add(helpText, c);
//		c.gridy++;
//		inner.add(rotFirst, c);
//		c.gridy++;
//		inner.add(reverseSort, c);
		c.gridy++;
		c.fill = GridBagConstraints.NONE;
		inner.add(jsg.getResetButton(), c);
		c.fill = GridBagConstraints.BOTH;
		c.gridy++;
		c.weightx = 0;
		c.gridwidth = 1;
		c.weighty = 1;
		inner.add(jsg.getJobListWithButtons(), c);
		c.gridx++;
		c.weightx = 1;
		inner.add(jsg.getPartyPane(), c);

//		ds.getP6_useAutoMarks().addAndRunListener(this::checkVis);
		outer.add(inner, BorderLayout.CENTER);
		return outer;
	}
}
