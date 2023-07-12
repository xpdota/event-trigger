package gg.xp.telestosupport.gui;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.telestosupport.TelestoConnectionError;
import gg.xp.telestosupport.TelestoGameCommand;
import gg.xp.telestosupport.TelestoHttpError;
import gg.xp.telestosupport.TelestoMain;
import gg.xp.telestosupport.TelestoStatusUpdatedEvent;
import gg.xp.telestosupport.doodle.DoodleProcessor;
import gg.xp.telestosupport.easytriggers.TelestoEasyTriggersAddons;
import gg.xp.telestosupport.rightclicks.TelestoRightClickOptions;
import gg.xp.xivsupport.events.misc.EchoEvent;
import gg.xp.xivsupport.gui.GuiMain;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.TitleBorderPanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.nav.GlobalUiRegistry;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.HttpURISettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@ScanMe
public class TelestoGui implements PluginTab {

	private static final String testEchoMsg = "Telesto Test Message";

	private final TelestoMain backend;
	private final EventMaster master;
	private final TelestoRightClickOptions trco;
	private final TelestoEasyTriggersAddons et;
	private final DoodleProcessor doodles;
	private final PicoContainer container;
	private final GlobalUiRegistry gur;
	private final JLabel statusLabel;
	private final BooleanSetting enablePortWarning;
	private JTextArea label;
	private TelestoGameCommand lastEvent;
	private TitleBorderFullsizePanel outer;
	private boolean warnedThisRun;

	public TelestoGui(TelestoMain backend, EventMaster master, TelestoRightClickOptions trco, TelestoEasyTriggersAddons et, DoodleProcessor doodles, PersistenceProvider pers, PicoContainer container, GlobalUiRegistry gur) {
		this.backend = backend;
		this.master = master;
		this.trco = trco;
		this.et = et;
		this.doodles = doodles;
		this.container = container;
		this.gur = gur;
		this.statusLabel = new JLabel();
		label = new JTextArea("Press the 'Test' button");
		updateLabel();
		enablePortWarning = new BooleanSetting(pers, "telesto-gui.warned-about-port-change", true);
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
		outer = new TitleBorderFullsizePanel("Telesto");
		JPanel uriControl = new HttpURISettingGui(backend.getUriSetting(), "Telesto URI", true).getComponent();
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
			scroll.setPreferredSize(new Dimension(400, 300));
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
		Component portWarning = new ReadOnlyText("""
				Note: in Telesto 1.0.0.0, the default port
				changed to 45678. Please check and verify
				that your port is correct.""");


		GuiUtil.simpleTopDownLayout(outer, 400, uriControl, scroll, testPanel, partyListCb, rightClicksCb, doodleCb, doodleAddonCb, delayPanel, portWarning);

		return outer;
	}

	private final ExecutorService exs = Executors.newCachedThreadPool();

	private void triggerMigrationCheck() {
		exs.submit(this::checkMigration);
	}

	private void checkMigration() {
		if (!enablePortWarning.get() || outer == null || warnedThisRun) {
			return;
		}
		URI uri = backend.getUriSetting().get();
		int port = uri.getPort();
		if (port == 51323 || port == 45678) {
			URI correctURI = findCorrectURI();
			if (correctURI != null) {
				JFrame frame = container.getComponent(GuiMain.class).getMainFrame();
				int result = JOptionPane.showOptionDialog(frame, "The default Telesto port changed in Telesto version 1.0.0.0. Would you like to update your settings now?", "Telesto Settings", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[]{"Yes", "No", "Don't Ask Again"}, "Yes");
				warnedThisRun = true;
				switch (result) {
					case JOptionPane.YES_OPTION -> {
						backend.getUriSetting().set(correctURI);
						gur.activateItem(TelestoGui.class);
						JOptionPane.showMessageDialog(frame, "The port has been changed. You should test your Telesto connection now.", "Please Test your Connection", JOptionPane.WARNING_MESSAGE);
					}
					case JOptionPane.NO_OPTION -> {
					}
					case JOptionPane.CANCEL_OPTION -> enablePortWarning.set(false);
				}
			}
		}
	}


	private final HttpClient http = HttpClient.newBuilder().build();

	private boolean checkPort(URI uri) {
		String body = """
				{
					"version": 1,
					"id": 1234,
					"type": "Bundle",
					"payload": []
				}""";
		try {
			HttpResponse<String> response = http.send(
					HttpRequest
							.newBuilder(uri)
							.POST(
									HttpRequest.BodyPublishers
											.ofString(
													body)).build(),
					HttpResponse.BodyHandlers.ofString());
			return true;
		}
		catch (Throwable t) {
			return false;
		}
	}

	private @Nullable URI findCorrectURI() {
		URI uri = backend.getUriSetting().get();
		URI withNewPort;
		URI withOldPort;
		try {
			withNewPort = new URIBuilder(uri).setPort(45678).build();
			withOldPort = new URIBuilder(uri).setPort(51323).build();
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		Future<Boolean> curPort = exs.submit(() -> checkPort(uri));
		Future<Boolean> newPort = exs.submit(() -> checkPort(withNewPort));
		Future<Boolean> oldPort = exs.submit(() -> checkPort(withOldPort));
		try {
			if (curPort.get()) {
				return null;
			}
			else if (newPort.get() && !oldPort.get()) {
				return withNewPort;
			}
			else if (oldPort.get() && !newPort.get()) {
				return withOldPort;
			}
		}
		catch (ExecutionException | InterruptedException e) {
			//
		}
		return null;
	}



	private void sendTestEvent() {
		label.setText("Waiting for response...\nPlease make sure you have /echo chat showing in-game.");
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
		triggerMigrationCheck();
	}

	@HandleEvents
	public void init(EventContext context, InitEvent init) {
		exs.submit(() -> {
			try {
				Thread.sleep(10_000);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			triggerMigrationCheck();
		});
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
