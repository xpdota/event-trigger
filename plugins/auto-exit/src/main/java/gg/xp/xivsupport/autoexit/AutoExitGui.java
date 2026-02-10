package gg.xp.xivsupport.autoexit;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.TopDownSimplePluginTab;
import gg.xp.xivsupport.gui.overlay.SimpleMultiLineText;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import gg.xp.xivsupport.persistence.gui.StringSettingGui;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class AutoExitGui extends TopDownSimplePluginTab {

	private final AutoExit backend;
	private final JLabel stateLabel = new JLabel();
	private final JLabel processLabel = new JLabel();

	public AutoExitGui(AutoExit backend) {
		super("Auto Exit", 600);
		this.backend = backend;
		backend.addStateListener(this::refreshLabels);
	}

	@Override
	protected Component[] provideChildren(JPanel outer) {
		refreshLabels();
		return new Component[]{
				new JLabel("Automatically exit when another process (e.g. FFXIV or ACT) exits."),
				new JLabel("Will not activate when importing/replaying, only live use."),
				new BooleanSettingGui(backend.getAutoExitEnabled(), "Enable Auto Exit").getComponent(),
				new StringSettingGui(backend.getProcessName(), "Process Name (e.g. ffxiv_dx11.exe)").getComponent(),
				new IntSettingSpinner(backend.getExitDelay(), "Exit Delay (seconds)").getComponent(),
				Box.createVerticalStrut(10),
				stateLabel,
				processLabel
		};
	}

	private void refreshLabels() {
		SwingUtilities.invokeLater(() -> {
			stateLabel.setText("Current State: " + backend.getState().getFriendlyName());
			ProcessHandle ph = backend.getDetectedProcess();
			if (ph == null) {
				processLabel.setText("Process Not Detected");
			}
			else {
				processLabel.setText("Detected Process: %s (Alive: %s)".formatted(ph.pid(), ph.isAlive()));
			}
		});
	}
}
