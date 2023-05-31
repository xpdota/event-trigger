package gg.xp.xivsupport.events.triggers.duties.ewult;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.BooleanSettingHidingPanel;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.DynamisDeltaAssignment;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.DynamisOmegaAssignment;
import gg.xp.xivsupport.events.triggers.duties.ewult.omega.DynamisSigmaAssignment;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.TitleBorderPanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.DutyPluginTab;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.tabs.SmartTabbedPane;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.models.groupmodels.PsMarkerGroup;
import gg.xp.xivsupport.models.groupmodels.TwoGroupsOfFour;
import gg.xp.xivsupport.models.groupmodels.WrothStyleAssignment;
import gg.xp.xivsupport.persistence.gui.AutomarkSettingGui;
import gg.xp.xivsupport.persistence.gui.BasicAutomarkSettingGroupGui;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import gg.xp.xivsupport.persistence.gui.JobSortGui;
import gg.xp.xivsupport.persistence.gui.JobSortOverrideGui;
import gg.xp.xivsupport.persistence.settings.AutomarkSetting;
import gg.xp.xivsupport.persistence.settings.JobSortOverrideSetting;
import gg.xp.xivsupport.persistence.settings.MultiSlotAutomarkSetting;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ScanMe
public class OmegaUltimateGroupPrioGui implements DutyPluginTab {

	private final OmegaUltimate backend;
	private final List<Runnable> toRefresh = new CopyOnWriteArrayList<>();
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

	private void refresh() {
		toRefresh.forEach(Runnable::run);
	}

	@Override
	public Component getTabContents() {
		jsg = new JobSortGui(backend.getGroupPrioJobSort());
		toRefresh.add(jsg::externalRefresh);
		RefreshLoop<OmegaUltimateGroupPrioGui> refresher = new RefreshLoop<>("OmegaAmRefresh", this, OmegaUltimateGroupPrioGui::refresh, unused -> 10_000L);
		TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("Group Prio") {
			@Override
			public void setVisible(boolean aFlag) {
				super.setVisible(aFlag);
				if (aFlag) {
					refresh();
					refresher.startIfNotStarted();
				}
			}
		};
		outer.setLayout(new BorderLayout());
		SmartTabbedPane tabs = new SmartTabbedPane();
		ReadOnlyText helpText = new ReadOnlyText("""
				Instructions:
				The first tab lets you pick your default priority. The tabs for individual markers allow you to further
				customize each mark. Some of them allow you to override the priority for that AM specifically.
				For light party mechanics:
				Jobs higher on the list will be preferred for group 1.
				Jobs lower on the list will be preferred for group 2.
				The easiest way to set this up is to simply put your group 1 jobs at the top, and your group 2 jobs at the bottom.""");
		{
			JPanel combined = jsg.getCombined();
			tabs.add("Default Prio", combined);
		}

		{
			JCheckBox looperMark = new BooleanSettingGui(backend.getLooperAM(), "Looper Automark (Configure Below)").getComponent();
			JCheckBox pantoMark = new BooleanSettingGui(backend.getPantoAmEnable(), "Panto Automark (Configure Below)").getComponent();
			JPanel looperPantoPanel = new JPanel();
			looperPantoPanel.setLayout(new GridBagLayout());
			GridBagConstraints c = GuiUtil.defaultGbc();
			c.anchor = GridBagConstraints.NORTHWEST;
			c.fill = GridBagConstraints.NONE;
			c.weighty = 0;
			looperPantoPanel.add(looperMark, c);
			c.gridy++;
			looperPantoPanel.add(pantoMark, c);
			c.gridy++;
			looperPantoPanel.add(makeLooperPantoPanel(), c);
			c.gridy++;
			c.weighty = 1;
			c.weightx = 1;
			looperPantoPanel.add(Box.createGlue(), c);
			JPanel combined = makeAmPanel(looperPantoPanel, backend.getP1prio());
			tabs.addTab("Looper", combined);
		}
		{
			tabs.addTab("P2", makeAmPanel(new BooleanSettingHidingPanel(backend.getPsAmEnable(), "P2 Playstation Automark", makePsMarkersPanel(), true), backend.getPsPrio()));
		}
		{
			MultiSlotAutomarkSetting<WrothStyleAssignment> markSettings = backend.getSniperAmSettings();
			BasicAutomarkSettingGroupGui<WrothStyleAssignment> sniperSettings = new BasicAutomarkSettingGroupGui<>("Sniper (P3 Transition)", markSettings, 4, true);
			tabs.addTab("Transition", makeAmPanel(new BooleanSettingHidingPanel(backend.getSniperAmEnable(), "P3 Transition Automark", sniperSettings, true), backend.getSniperPrio()));
		}
		{
			tabs.addTab("Monitor", makeAmPanel(new BooleanSettingHidingPanel(backend.getMonitorAmEnable(), "Monitor Automark", new JPanel(), true), backend.getMonitorPrio()));
		}
		{
			BasicAutomarkSettingGroupGui<DynamisDeltaAssignment> deltaSettings = new BasicAutomarkSettingGroupGui<>("Run: Dynamis (Delta)", backend.getDeltaAmSettings(), 2, false);
			tabs.addTab("Delta", new BooleanSettingHidingPanel(backend.getDeltaAmEnable(), "Delta Automark", deltaSettings, true));
		}
		{
			BasicAutomarkSettingGroupGui<DynamisSigmaAssignment> sigmaSettings = new BasicAutomarkSettingGroupGui<>("Run: Dynamis (Sigma)", backend.getSigmaAmSettings(), 4, true);
			JPanel panel = new JPanel();
			sigmaSettings.setAlignmentX(0);
			panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
			panel.add(sigmaSettings, BorderLayout.CENTER);
			JPanel delay = new IntSettingSpinner(backend.getSigmaAmDelay(), "AM Delay (seconds) - 15+ recommended").getComponent();
			delay.setAlignmentX(0);
			panel.add(delay, BorderLayout.SOUTH);
			tabs.addTab("Sigma", makeAmPanel(new BooleanSettingHidingPanel(backend.getSigmaAmEnable(), "Sigma Automark", panel, true), backend.getSigmaPsPrio()));
		}
		{
			BasicAutomarkSettingGroupGui<DynamisOmegaAssignment> omegaSettings = new BasicAutomarkSettingGroupGui<>("Run: Dynamis (Omega)", backend.getOmegaAmSettings(), 4, true);
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
			omegaSettings.setAlignmentX(0);
			panel.add(omegaSettings);
			JPanel firstSetDelay = new IntSettingSpinner(backend.getOmegaFirstSetDelay(), "First Set Delay").getComponent();
			firstSetDelay.setAlignmentX(0);
			panel.add(firstSetDelay);
			JPanel secondSetDelay = new IntSettingSpinner(backend.getOmegaSecondSetDelay(), "Second Set Delay").getComponent();
			secondSetDelay.setAlignmentX(0);
			panel.add(secondSetDelay);
			tabs.addTab("Omega", makeAmPanel(new BooleanSettingHidingPanel(backend.getOmegaAmEnable(), "Omega Automark", panel, true), backend.getOmegaPsPrio()));
		}
		outer.add(tabs, BorderLayout.CENTER);
		outer.add(helpText, BorderLayout.NORTH);

		return outer;
	}

	private JPanel makeAmPanel(Component top, JobSortOverrideSetting override) {
		JobSortOverrideGui overrideGui = new JobSortOverrideGui(override);
		JPanel panel = new JPanel(new BorderLayout());
		int HEIGHT = 250;
		top.setPreferredSize(new Dimension(32767, HEIGHT));
		top.setMinimumSize(new Dimension(1, HEIGHT));
		top.setMaximumSize(new Dimension(32767, HEIGHT));
		panel.add(top, BorderLayout.NORTH);
		toRefresh.add(overrideGui::externalRefresh);
		JPanel overrideGuiPanel = overrideGui.getCombined();
		panel.add(overrideGuiPanel, BorderLayout.CENTER);
		panel.revalidate();
		return panel;
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
