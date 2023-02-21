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
import gg.xp.telestosupport.doodle.DoodleProcessor;
import gg.xp.telestosupport.easytriggers.TelestoEasyTriggersAddons;
import gg.xp.telestosupport.rightclicks.TelestoRightClickOptions;
import gg.xp.xivsupport.events.misc.EchoEvent;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.TitleBorderPanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tabs.TabAware;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.HttpURISettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class TelestoGui implements PluginTab {

	private static final String testEchoMsg = "Telesto Test Message";

	private final TelestoMain backend;
	private final EventMaster master;
	private final TelestoRightClickOptions trco;
	private final TelestoEasyTriggersAddons et;
	private final DoodleProcessor doodles;
	private final JLabel statusLabel;
	private JTextArea label;
	private TelestoGameCommand lastEvent;

	public TelestoGui(TelestoMain backend, EventMaster master, TelestoRightClickOptions trco, TelestoEasyTriggersAddons et, DoodleProcessor doodles) {
		this.backend = backend;
		this.master = master;
		this.trco = trco;
		this.et = et;
		this.doodles = doodles;
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
		JCheckBox rightClicksCb = new BooleanSettingGui(trco.getEnableExtraOptions(), "Install Right Click Debug Options").getComponent();
		BooleanSetting doodles = this.doodles.enableDoodles();
		JCheckBox doodleCb = new BooleanSettingGui(doodles, "Enable Doodle Support").getComponent();
		JCheckBox doodleAddonCb = new BooleanSettingGui(et.getEnableAddons(), "Install Easy Trigger Doodle Actions").getComponent();
		doodleCb.addActionListener(l -> {
			boolean selected = doodleCb.isSelected();
			if (selected) {
				int result = JOptionPane.showConfirmDialog(outer, "WARNING: Please do not abuse this functionality! By enabling this, you agree to only use these for debugging purposes and not in the course of real gameplay.", "WARNING", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (result != JOptionPane.OK_OPTION) {
					doodleCb.setSelected(false);
				}
			}
			else {
				et.getEnableAddons().set(false);
			}
			doodleAddonCb.setEnabled(selected);

		});
		doodleAddonCb.addActionListener(l -> {
			if (doodleAddonCb.isSelected()) {
				int result = JOptionPane.showConfirmDialog(outer, "WARNING: Please do not abuse this functionality! By enabling this, you agree to only use these for debugging purposes and not in the course of real gameplay.", "WARNING", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (result != JOptionPane.OK_OPTION) {
					doodleAddonCb.setSelected(false);
				}
			}
		});

		TitleBorderPanel delayPanel = new TitleBorderPanel("Delays");
		{
			delayPanel.setLayout(new WrapLayout());
			delayPanel.add(new JLabel("Between commands, delay "));
			delayPanel.add(new IntSettingSpinner(backend.getCommandDelayBase(), "ms").getSpinnerOnly());
			delayPanel.add(new JLabel("ms,"));
			delayPanel.add(new JLabel("plus a random delay up to "));
			delayPanel.add(new IntSettingSpinner(backend.getCommandDelayPlus(), "").getSpinnerOnly());
			delayPanel.add(new JLabel("ms."));
		}


		GuiUtil.simpleTopDownLayout(outer, 400, uriControl, scroll, testPanel, partyListCb, rightClicksCb, doodleCb, doodleAddonCb, delayPanel);

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
