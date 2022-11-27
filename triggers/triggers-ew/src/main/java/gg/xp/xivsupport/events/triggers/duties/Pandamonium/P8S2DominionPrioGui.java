package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.DutyPluginTab;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.JobSortGui;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class P8S2DominionPrioGui implements DutyPluginTab {
	private final P8S2DominionPrio backend;
	private JobSortGui jsg;
	private JPanel inner;

	public P8S2DominionPrioGui(P8S2DominionPrio prio) {
		this.backend = prio;
	}

	@Override
	public String getTabName() {
		return "Dominion Priority";
	}

	@Override
	public Component getTabContents() {
		jsg = new JobSortGui(backend.getSortSetting());
		RefreshLoop<JobSortGui> refresher = new RefreshLoop<>("DominionPrioRefresh", jsg, JobSortGui::externalRefresh, unused -> 10_000L);
		TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("Dominion Priority") {
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
		JCheckBox enabled = new BooleanSettingGui(backend.getEnabledSetting(), "Enable Priority").getComponent();
		outer.add(enabled, BorderLayout.NORTH);
		GridBagConstraints c = new GridBagConstraints(0, 0, 2, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);

		inner = new JPanel(new GridBagLayout());
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

		backend.getEnabledSetting().addAndRunListener(this::checkVis);
		outer.add(inner, BorderLayout.CENTER);
		return outer;
	}

	private void checkVis() {
		boolean enabled = backend.getEnabledSetting().get();
		inner.setVisible(enabled);
	}

	@Override
	public int getSortOrder() {
		return 101;
	}

	@Override
	public KnownDuty getDuty() {
		return KnownDuty.P8S;
	}
}
