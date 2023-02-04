package gg.xp.xivsupport.events.triggers.duties.ewult;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.TitleBorderPanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.DutyPluginTab;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.models.groupmodels.PsMarkerGroup;
import gg.xp.xivsupport.models.groupmodels.TwoGroupsOfFour;
import gg.xp.xivsupport.persistence.gui.AutomarkSettingGui;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.JobSortGui;
import gg.xp.xivsupport.persistence.settings.AutomarkSetting;
import gg.xp.xivsupport.persistence.settings.MultiSlotAutomarkSetting;

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
		return "Group Swap Priority/Automarks";
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

		{
			JCheckBox looperMark = new BooleanSettingGui(backend.getLooperAM(), "Looper Automark").getComponent();
			inner.add(looperMark, c);
			c.gridy++;
		}

		{
			JCheckBox pantoMark = new BooleanSettingGui(backend.getPantoAmEnable(), "Panto Automark").getComponent();
			inner.add(pantoMark, c);
			c.gridy++;
		}

		{
			JCheckBox psMark = new BooleanSettingGui(backend.getPsAmEnable(), "P2 Headmarker Automark").getComponent();
			inner.add(psMark, c);
			c.gridy++;
		}

		{
			JCheckBox sniperMark = new BooleanSettingGui(backend.getSniperAmEnable(), "Sniper Cannon Automark (Attack = Spread Debuff, Bind = Stack Debuff, Ignore = Nothing)").getComponent();
			inner.add(sniperMark, c);
			c.gridy++;
		}

		{
			JCheckBox monitorMark = new BooleanSettingGui(backend.getMonitorAmEnable(), "Monitor Automark (Attack = Nothing, Bind = Monitor)").getComponent();
			inner.add(monitorMark, c);
			c.gridy++;
		}

		ReadOnlyText helpText = new ReadOnlyText("""
				Instructions:
				Jobs higher on the list will be preferred for group 1.
				Jobs lower on the list will be preferred for group 2.
				The easiest way to set this up is to simply put your group 1 jobs at the top,
				and your group 2 jobs at the bottom.
				""");


		inner.add(helpText, c);
		c.gridy++;

		{
			TitleBorderPanel mappingPanel = new TitleBorderPanel("Looper/Panto Markers");
			mappingPanel.setLayout(new GridBagLayout());
			GridBagConstraints mc = GuiUtil.defaultGbc();
			mc.ipadx = 10;
			mc.gridx = 1;
			mc.gridy = 0;
			JLabel g1l = new JLabel("Group 1");
			mappingPanel.add(g1l, mc);
			mc.gridx++;
			mappingPanel.add(new JLabel("Group 2"), mc);

			{
				MultiSlotAutomarkSetting<TwoGroupsOfFour> markSettings = backend.getMarkSettings();
				for (int row = 0; row < 4; row++) {
					mc.gridy++;
					mc.gridx = 0;
					mappingPanel.add(new JLabel("#" + (row + 1)), mc);
					AutomarkSetting g1setting = markSettings.getSettings().get(TwoGroupsOfFour.values()[row]);
					mc.gridx = 1;
					mappingPanel.add(new AutomarkSettingGui(g1setting, null).getCombined(), mc);
					AutomarkSetting g2setting = markSettings.getSettings().get(TwoGroupsOfFour.values()[row + 4]);
					mc.gridx = 2;
					mappingPanel.add(new AutomarkSettingGui(g2setting, null).getCombined(), mc);
				}
				markSettings.addListener(mappingPanel::repaint);
			}
			inner.add(mappingPanel, c);
		}

		{
			TitleBorderPanel mappingPanel = new TitleBorderPanel("P2 PS Markers");
			mappingPanel.setLayout(new GridBagLayout());
			GridBagConstraints mc = GuiUtil.defaultGbc();
			mc.ipadx = 10;
			mc.gridx = 1;
			mc.gridy = 0;
			mappingPanel.add(new JLabel("Group 1, Mid"), mc);
			mc.gridx++;
			mappingPanel.add(new JLabel("Group 2, Mid"), mc);
			mc.gridx++;
			mappingPanel.add(new JLabel("Group 1, Far"), mc);
			mc.gridx++;
			mappingPanel.add(new JLabel("Group 2, Far"), mc);
			{
				MultiSlotAutomarkSetting<PsMarkerGroup> psMarkSettings = backend.getPsMarkSettings();
				MultiSlotAutomarkSetting<PsMarkerGroup> psMarkFarSettings = backend.getPsMarkSettingsFarGlitch();
				for (int row = 0; row < 4; row++) {
					mc.gridy++;
					mc.gridx = 0;
					String shape = switch (row) {
						case 0 -> "Circle";
						case 1 -> "Triangle";
						case 2 -> "Square";
						case 3 -> "X";
						default -> "";
					};
					mappingPanel.add(new JLabel(shape), mc);
					AutomarkSetting g1setting = psMarkSettings.getSettings().get(PsMarkerGroup.values()[row]);
					mc.gridx++;
					mappingPanel.add(new AutomarkSettingGui(g1setting, null).getCombined(), mc);
					AutomarkSetting g2setting = psMarkSettings.getSettings().get(PsMarkerGroup.values()[row + 4]);
					mc.gridx++;
					mappingPanel.add(new AutomarkSettingGui(g2setting, null).getCombined(), mc);
					AutomarkSetting g1farSetting = psMarkFarSettings.getSettings().get(PsMarkerGroup.values()[row]);
					mc.gridx++;
					mappingPanel.add(new AutomarkSettingGui(g1farSetting, null).getCombined(), mc);
					AutomarkSetting g2farSetting = psMarkFarSettings.getSettings().get(PsMarkerGroup.values()[row + 4]);
					mc.gridx++;
					mappingPanel.add(new AutomarkSettingGui(g2farSetting, null).getCombined(), mc);
				}
				psMarkSettings.addListener(mappingPanel::repaint);
			}
//			c.gridx++;
			c.gridy++;
			inner.add(mappingPanel, c);
//			c.gridx = 0;

		}


//		c.gridy++;
//		inner.add(rotFirst, c);
//		c.gridy++;
//		inner.add(reverseSort, c);
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
