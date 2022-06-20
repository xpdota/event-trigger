package gg.xp.xivsupport.events.triggers.duties.ewult;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.JobSortGui;

import javax.swing.*;
import java.awt.*;
import java.util.List;

@ScanMe
public class DragonsongGui implements PluginTab {

	private final Dragonsong ds;
	private JobSortGui jsg;
	private JPanel inner;

	public DragonsongGui(Dragonsong ds) {
		this.ds = ds;
	}

	@Override
	public String getTabName() {
		return "DSR Automarks";
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("Dragonsong Automarks");
		outer.setLayout(new BorderLayout());
		jsg = new JobSortGui(ds.getP6_sortSetting());
		JCheckBox p6marks = new BooleanSettingGui(ds.getP6_useAutoMarks(), "P6 Wroth Flames Automarks").getComponent();
		outer.add(p6marks, BorderLayout.NORTH);
		GridBagConstraints c = new GridBagConstraints(0, 0, 2, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);

		inner = new JPanel();
		inner.setLayout(new GridBagLayout());
		JCheckBox p6altMark = new BooleanSettingGui(ds.getP6_altMarkMode(), "Alt Mode").getComponent();
		JCheckBox rotFirst = new BooleanSettingGui(ds.getP6_rotPrioHigh(), "Rot takes highest priority").getComponent();
		JCheckBox reverseSort = new BooleanSettingGui(ds.getP6_reverseSort(), "Reverse sort (higher priority gets larger number)").getComponent();
		ReadOnlyText helpText = new ReadOnlyText("""
				The four players with the 'spread' debuffs will receive 'attack' markers.
				In addition, if Telesto is in use:
				
				If "Alt Mode" is disabled:
				The two players with 'stack' debuffs will receive 'bind1' and 'bind2' markers,
				and the two players with nothing will receive 'ignore1 and ignore2' markers.
				The intent is that the '1' players will stack together, and '2' players stack together.
				
				If "Alt Mode" is enabled:
				The two players with 'stack' debuffs will receive 'bind1' and 'ignore1' markers,
				and the two players with nothing will receive 'bind2' and 'ignore2' markers.
				The intent is that the 'bind' players will stack together, and 'ignore' players stack together.
				
				In both cases, higher priority jobs will get lower number markers. To reverse this, check "Reverse Sort".
				""");


		inner.add(p6altMark, c);
		c.gridy++;
		inner.add(helpText, c);
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
		inner.add(jsg.getJobListPane(), c);
		c.gridx++;
		c.weightx = 1;
		inner.add(jsg.getPartyPane(), c);

		ds.getP6_useAutoMarks().addListener(this::checkVis);
		outer.add(inner, BorderLayout.CENTER);
		return outer;
	}

	private void checkVis() {
		boolean enabled = ds.getP6_useAutoMarks().get();
		inner.setVisible(enabled);
	}

	@Override
	public int getSortOrder() {
		return 101;
	}

	// TODO: this should only happen on a party/job/etc update, not a normal state recalc, but it's difficult to
	// determine exactly what should trigger it. Maybe better to just stick it on a timer that only applies when the
	// tab is visible?
	@HandleEvents(order = 20_000)
	public void updatePartyList(EventContext context, XivStateRecalculatedEvent event) {
		if (jsg != null) {
			jsg.externalRefresh();
		}
	}
}
