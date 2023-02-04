package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.TitleBorderPanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.DutyPluginTab;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.AutomarkSettingGui;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.JobSortGui;
import gg.xp.xivsupport.persistence.settings.AutomarkSetting;
import gg.xp.xivsupport.persistence.settings.MultiSlotAutomarkSetting;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class DragonsongAmGui implements DutyPluginTab {

	private final Dragonsong ds;
	private JobSortGui jsg;
	private JPanel inner;

	public DragonsongAmGui(Dragonsong ds) {
		this.ds = ds;
	}

	@Override
	public String getTabName() {
		return "DSR Wroth+Thunder Automarks";
	}

	@Override
	public Component getTabContents() {
		jsg = new JobSortGui(ds.getP6_sortSetting());
		RefreshLoop<JobSortGui> refresher = new RefreshLoop<>("DsrAmRefresh", jsg, JobSortGui::externalRefresh, unused -> 10_000L);
		TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("Dragonsong Automarks") {
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
		JCheckBox p5marks = new BooleanSettingGui(ds.getP5_thunderstruckAutoMarks(), "P5 Thunderstruck Automarks").getComponent();
		JCheckBox p6marks = new BooleanSettingGui(ds.getP6_useAutoMarks(), "P6 Wroth Flames Automarks").getComponent();
		topCheckboxes.add(p5marks);
		topCheckboxes.add(p6marks);
		outer.add(topCheckboxes, BorderLayout.NORTH);
		GridBagConstraints c = new GridBagConstraints(0, 0, 2, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);

		inner = new JPanel();
		inner.setLayout(new GridBagLayout());
		JCheckBox rotFirst = new BooleanSettingGui(ds.getP6_rotPrioHigh(), "Rot takes highest priority").getComponent();
		JCheckBox reverseSort = new BooleanSettingGui(ds.getP6_reverseSort(), "Reverse sort (higher priority gets larger number)").getComponent();
		ReadOnlyText helpText = new ReadOnlyText("""
				Configure your priority using the job list below. Jobs higher on the list receive a higher priority.
				The "Reverse sort" setting configures whether "high priority" means they get a higher marker number,
				or a lower number. The "Rot takes highest priority" checkbox will cause the current rot holder to
				receive the lowest possible marker (or highest, if "Reverse sort" is enabled).

				If you are using classic macros rather than Telesto, you MUST use the basic "Next Available Attack"
				macros.
				""");


		inner.add(helpText, c);
		c.gridy++;
		{
			TitleBorderPanel mappingPanel = new TitleBorderPanel("Wroth Marker Mapping");
			mappingPanel.setLayout(new GridBagLayout());
			GridBagConstraints mc = GuiUtil.defaultGbc();
			mc.ipadx = 5;
			mc.gridy = 0;

			MultiSlotAutomarkSetting<DragonsongWrothAssignments> markSettings = ds.getP6_amAssignments();
			for (int row = 0; row < 4; row++) {
				mc.gridx = 0;
				mappingPanel.add(new JLabel("Spread #" + (row + 1)), mc);

				AutomarkSetting g1setting = markSettings.getSettings().get(DragonsongWrothAssignments.values()[row]);
				mc.gridx++;
				mappingPanel.add(new AutomarkSettingGui(g1setting, null).getCombined(), mc);

				mc.gridx++;
				mappingPanel.add(Box.createHorizontalStrut(10));

				mc.gridx++;
				mappingPanel.add(new JLabel(switch (row) {
					case 0 -> "Stack #1";
					case 1 -> "Stack #2";
					case 2 -> "Nothing #1";
					case 3 -> "Nothing #2";
					default -> "?";
				}), mc);


				AutomarkSetting g2setting = markSettings.getSettings().get(DragonsongWrothAssignments.values()[row + 4]);
				mc.gridx++;
				mappingPanel.add(new AutomarkSettingGui(g2setting, null).getCombined(), mc);
				mc.gridy++;
			}
			markSettings.addListener(mappingPanel::repaint);
			inner.add(mappingPanel, c);
		}
		c.gridy++;
		inner.add(rotFirst, c);
		c.gridy++;
		inner.add(reverseSort, c);
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

		ds.getP6_useAutoMarks().addAndRunListener(this::checkVis);
		outer.add(inner, BorderLayout.CENTER);
		return outer;
	}

	@Override
	public KnownDuty getDuty() {
		return KnownDuty.Dragonsong;
	}

	private void checkVis() {
		boolean enabled = ds.getP6_useAutoMarks().get();
		inner.setVisible(enabled);
	}

	@Override
	public int getSortOrder() {
		return 101;
	}

//	// TODO: this should only happen on a party/job/etc update, not a normal state recalc, but it's difficult to
//	// determine exactly what should trigger it. Maybe better to just stick it on a timer that only applies when the
//	// tab is visible?
//	@HandleEvents(order = 20_000)
//	public void updatePartyList(EventContext context, XivStateRecalculatedEvent event) {
//		if (jsg != null) {
//			jsg.externalRefresh();
//		}
//	}

	public void tryBringToFront() {
		if (inner != null) {
			GuiUtil.bringToFront(inner);
		}
	}
}
