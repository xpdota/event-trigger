package gg.xp.telestosupport.gui;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.telestosupport.TelestoConnectionError;
import gg.xp.telestosupport.TelestoHttpError;
import gg.xp.telestosupport.TelestoGameCommand;
import gg.xp.telestosupport.TelestoMain;
import gg.xp.telestosupport.TelestoStatusUpdatedEvent;
import gg.xp.xivsupport.events.misc.EchoEvent;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tabs.TabAware;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.HttpURISettingGui;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class TelestoGui implements PluginTab {

	private static final String testEchoMsg = "Telesto Test Message";

	private final TelestoMain backend;
	private final EventMaster master;
	private final JLabel statusLabel;
	private JTextArea label;
	private TelestoGameCommand lastEvent;

	public TelestoGui(TelestoMain backend, EventMaster master) {
		this.backend = backend;
		this.master = master;
		this.statusLabel = new JLabel();
		label = new JTextArea("Press the 'Test' button");
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
		JPanel uriControl = new HttpURISettingGui(backend.getUriSetting(), "Telesto URI").getComponent();
		JPanel testPanel;
		JScrollPane scroll;
		{
			testPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			JButton test = new JButton("Test");
			test.addActionListener(l -> {
				sendTestEvent();
			});
			testPanel.add(test);
			scroll = new JScrollPane(label);
			scroll.setPreferredSize(new Dimension(400, 400));
//			testPanel.add(scroll);
			Dimension pref = testPanel.getPreferredSize();
			Dimension newPref = new Dimension(pref.width + 40, pref.height);
			testPanel.setPreferredSize(newPref);
			testPanel.setMinimumSize(newPref);
			testPanel.setMaximumSize(newPref);
		}
		JCheckBox partyListCb = new BooleanSettingGui(backend.getEnablePartyList(), "Enable Party List").getComponent();

		GuiUtil.simpleTopDownLayout(outer, 400, uriControl, scroll, testPanel, partyListCb);

		return outer;
	}

	private void sendTestEvent() {
		label.setText("Waiting for response...");
		lastEvent = new TelestoGameCommand("/e " + testEchoMsg);
		master.pushEvent(lastEvent);
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

	@HandleEvents
	public void handleError(EventContext context, TelestoHttpError error) {
		if (error.getThisOrParentOfType(TelestoGameCommand.class) == lastEvent) {
			label.setText("Error: " + error.getResponse().statusCode() + '\n' + error.getResponse().body());
		}
	}

	@HandleEvents
	public void handleError(EventContext context, TelestoConnectionError error) {
		if (error.getThisOrParentOfType(TelestoGameCommand.class) == lastEvent) {
			label.setText("Error:\n" + ExceptionUtils.getStackTrace(error.getError()));
		}
	}

	@Override
	public boolean asyncOk() {
		return false;
	}

	@Override
	public int getSortOrder() {
		return 99999;
	}
}
