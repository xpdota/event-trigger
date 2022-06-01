package gg.xp.telestosupport.gui;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.telestosupport.TelestoGameCommand;
import gg.xp.telestosupport.TelestoMain;
import gg.xp.telestosupport.TelestoStatusUpdatedEvent;
import gg.xp.xivsupport.events.misc.EchoEvent;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tabs.JavaPanel;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.HttpURISettingGui;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class TelestoGui implements PluginTab {

	private static final String testEchoMsg = "Telesto Test Message";

	private final TelestoMain backend;
	private final EventMaster master;
	private final JLabel statusLabel;
	private JLabel label;

	public TelestoGui(TelestoMain backend, EventMaster master) {
		this.backend = backend;
		this.master = master;
		this.statusLabel = new JLabel();
		label = new JLabel("Press the 'Test' button");
		updateLabel();
	}

	private void updateLabel() {
		statusLabel.setText("Status: " + backend.getStatus());
	}

	@Override
	public String getTabName() {
		return "Telesto";
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("Telesto");
		GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 5);
		outer.setLayout(new GridBagLayout());
		outer.add(new HttpURISettingGui(backend.getUriSetting(), "Telesto URI").getComponent(), c);
		c.gridy++;
		outer.add(statusLabel, c);

		c.gridy++;
		{
			JPanel testPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			JButton test = new JButton("Test");
			test.addActionListener(l -> {
				label.setText("Waiting for response...");
				master.pushEvent(new TelestoGameCommand("/e " + testEchoMsg));
			});
			testPanel.add(test);
			testPanel.add(label);
			outer.add(testPanel, c);
			Dimension pref = testPanel.getPreferredSize();
			Dimension newPref = new Dimension(pref.width + 40, pref.height);
			testPanel.setPreferredSize(newPref);
			testPanel.setMinimumSize(newPref);
			testPanel.setMaximumSize(newPref);
		}
		c.gridy++;
		outer.add(new BooleanSettingGui(backend.getEnablePartyList(), "Enable Party List").getComponent(), c);

		c.gridy++;
		c.weighty = 1;
		outer.add(Box.createGlue(), c);

		return outer;
	}

	@HandleEvents
	public void handleConnectionStatusChange(EventContext context, TelestoStatusUpdatedEvent event) {
		updateLabel();
	}

	@HandleEvents
	public void handleEchoEvent(EventContext context, EchoEvent event) {
		if (event.getLine().equals(testEchoMsg)) {
			label.setText("Success!");
		}
	}

	@Override
	public int getSortOrder() {
		return 99999;
	}
}
