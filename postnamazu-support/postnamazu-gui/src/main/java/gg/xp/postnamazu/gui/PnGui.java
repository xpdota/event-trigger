package gg.xp.postnamazu.gui;

import gg.xp.postnamazu.PnGameCommand;
import gg.xp.postnamazu.PnMain;
import gg.xp.postnamazu.PnMode;
import gg.xp.postnamazu.PnStatusUpdatedEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.misc.EchoEvent;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.TitleBorderPanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.EnumSettingGui;
import gg.xp.xivsupport.persistence.gui.HttpURISettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import gg.xp.xivsupport.persistence.settings.EnumSetting;

import javax.swing.*;
import java.awt.*;

public class PnGui implements PluginTab {

	private static final String testEchoMsg = "PN Test Message";

	private final EventMaster master;
	private final PnMain backend;
	private final JLabel statusLabel;
	private final JTextArea textArea;
	private PnGameCommand lastEvent;

	public PnGui(EventMaster master, PnMain backend) {
		this.master = master;
		this.backend = backend;
		this.statusLabel = new JLabel();
		textArea = new JTextArea("Press the 'Test' button");
		updateLabel();
	}

	private void updateLabel() {
		statusLabel.setText("Status: " + backend.getStatus());
	}

	@Override
	public String getTabName() {
		return "PostNamazu";
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("PostNamazu");
		EnumSetting<PnMode> modeSetting = backend.getModeSetting();
		var modeSettingGui = new EnumSettingGui<>(modeSetting, "Mode", () -> true, true);
		JPanel uriControl = new HttpURISettingGui(backend.getUriSetting(), "Base URI", false).getComponent();
		modeSetting.addAndRunListener(() -> uriControl.setEnabled(modeSetting.get() == PnMode.HTTP));
		JPanel testPanel;
		JScrollPane scroll;
		{
			testPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			JButton test = new JButton("Test");
			test.addActionListener(l -> {
				sendTestEvent();
			});
			testPanel.add(test);
			scroll = new JScrollPane(textArea);
			scroll.setMinimumSize(new Dimension(390, 390));
			scroll.setPreferredSize(new Dimension(390, 390));
//			testPanel.add(scroll);
			Dimension pref = testPanel.getPreferredSize();
			Dimension newPref = new Dimension(pref.width + 40, pref.height);
			testPanel.setPreferredSize(newPref);
			testPanel.setMinimumSize(newPref);
			testPanel.setMaximumSize(newPref);
		}
//		JCheckBox partyListCb = new BooleanSettingGui(backend.getEnablePartyList(), "Enable Party List").getComponent();
//		JCheckBox rightClicksCb = new BooleanSettingGui(trco.getEnableExtraOptions(), "Install Right Click Debug Options").getComponent();
		TitleBorderPanel amDelayPanel = new TitleBorderPanel("Marker Delays");
		{
			amDelayPanel.setLayout(new WrapLayout());
			amDelayPanel.add(new JLabel("Between marks, delay "));
			amDelayPanel.add(new IntSettingSpinner(backend.getAmDelayBase(), "ms").getSpinnerOnly());
			amDelayPanel.add(new JLabel("ms,"));
			amDelayPanel.add(new JLabel("plus a random delay up to "));
			amDelayPanel.add(new IntSettingSpinner(backend.getAmDelayPlus(), "").getSpinnerOnly());
			amDelayPanel.add(new JLabel("ms."));
		}
		TitleBorderPanel cmdDelayPanel = new TitleBorderPanel("Command Delays");
		{
			cmdDelayPanel.setLayout(new WrapLayout());
			cmdDelayPanel.add(new JLabel("Between commands, delay "));
			cmdDelayPanel.add(new IntSettingSpinner(backend.getCmdDelayBase(), "ms").getSpinnerOnly());
			cmdDelayPanel.add(new JLabel("ms,"));
			cmdDelayPanel.add(new JLabel("plus a random delay up to "));
			cmdDelayPanel.add(new IntSettingSpinner(backend.getCmdDelayPlus(), "").getSpinnerOnly());
			cmdDelayPanel.add(new JLabel("ms."));
		}


		GuiUtil.simpleTopDownLayout(outer, 400, modeSettingGui.getComponent(), uriControl, Box.createRigidArea(new Dimension(390, 5)), scroll, testPanel, amDelayPanel, cmdDelayPanel);
		outer.invalidate();

		return outer;
	}

	private void sendTestEvent() {
		textArea.setText("Waiting for response...");
		lastEvent = new PnGameCommand("/e " + testEchoMsg);
		master.pushEvent(lastEvent);
	}

	@HandleEvents
	public void handleConnectionStatusChange(EventContext context, PnStatusUpdatedEvent event) {
		updateLabel();
	}

	@HandleEvents
	public void handleEchoEvent(EventContext context, EchoEvent event) {
		if (event.getLine().equals(testEchoMsg)) {
			textArea.setText("Success!");
		}
	}
}
