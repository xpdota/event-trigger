olackage gg.xp.xivsupport.events.triggers.duties.ewult;

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
import gg.xp.xivsupport.models.groupmodels.WrothStyleAssignment;
import gg.xp.xivsupport.persistence.gui.AutomarkSettingGui;
import gg.xp.xivsupport.persistence.gui.BasicAutomarkSettingGroupGui;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.JobSortGui;
import gg.xp.xivsupport.persistence.settings.AutomarkSetting;
import gg.xp.xivsupport.persistence.settings.MultiSlotAutomarkSetting;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class OmegaUltimateGroupPrioGui implements DutyPluginTab {

	private final OmegaUltimate backend;
	private JobSortGui jsg;

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


		{
			GridBagConstraints c = new GridBagConstraints(0, 0, GridBagConstraints.REMAINDER, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
			JPanel upper = new JPanel();
			upper.setLayout(new GridBagLayout());

			{
				JCheckBox looperMark = new BooleanSettingGui(backend.getLooperAM(), "Looper Automark").getComponent();
				upper.add(looperMark, c);
				c.gridy++;
			}

			{
				JCheckBox pantoMark = new BooleanSettingGui(backend.getPantoAmEnable(), "Panto Automark").getComponent();
				upper.add(pantoMark, c);
				c.gridy++;
			}

			{
				JCheckBox psMark = new BooleanSettingGui(backend.getPsAmEnable(), "P2 Headmarker Automark").getComponent();
				upper.add(psMark, c);
				c.gridy++;
			}

			{
				JCheckBox sniperMark = new BooleanSettingGui(backend.getSniperAmEnable(), "Sniper Cannon Automark (Attack = Spread Debuff, Bind = Stack Debuff, Ignore = Nothing)").getComponent();
				upper.add(sniperMark, c);
				c.gridy++;
			}

			{
				JCheckBox monitorMark = new BooleanSettingGui(backend.getMonitorAmEnable(), "Monitor Automark (Attack = Nothing, Bind = Monitor)").getComponent();
				upper.add(monitorMark, c);
				c.gridy++;
			}

			ReadOnlyText helpText = new ReadOnlyText("""
					Instructions:
					Jobs higher on the list will be preferred for group 1.
					Jobs lower on the list will be preferred for group 2.
					The easiest way to set this up is to simply put your group 1 jobs at the top,
					and your group 2 jobs at the bottom.
					""");

			upper.add(helpText, c);
			c.gridy++;

			{
				TitleBorderPanel mappingPanel = makeLooperPantoPanel();
				c.weightx = 0;
				c.gridwidth = 1;
				upper.add(mappingPanel, c);
			}
			{
				MultiSlotAutomarkSetting<WrothStyleAssignment> markSettings = backend.getSniperAmSettings();
				BasicAutomarkSettingGroupGui<WrothStyleAssignment> sniperSettings = new BasicAutomarkSettingGroupGui<>("Sniper (P3 Transition)", markSettings, 4, true);
				c.gridx++;
				upper.add(sniperSettings, c);
				c.gridx++;
				c.gridwidth = GridBagConstraints.REMAINDER;
				upper.add(Box.createHorizontalGlue(), c);
				c.gridx = 0;
				c.gridy++;
			}

			{
				TitleBorderPanel mappingPanel = makePsMarkersPanel();
				upper.add(mappingPanel, c);
			}


			outer.add(upper, BorderLayout.NORTH);
		}
		{
			outer.add(jsg.getCombined(), BorderLayout.CENTER);
		}
		return outer;
	}

	@NotNull
	private TitleBorderPanel makeLooperPantoPanel() {
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
		return mappingPanel;
	}

	@NotNull
	private TitleBorderPanel makePsMarkersPanel() {
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
		return mappingPanel;
	}
}
