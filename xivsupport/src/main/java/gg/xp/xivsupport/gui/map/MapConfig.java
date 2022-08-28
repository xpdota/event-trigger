package gg.xp.xivsupport.gui.map;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.TitleBorderPanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class MapConfig {

	private final MapDataController mdc;

	public MapConfig(MapDataController mdc) {
		this.mdc = mdc;
	}

	public Component makeComponent() {
		TitleBorderPanel panel = new TitleBorderPanel("Map/Replay Settings");
		JCheckBox recording = new BooleanSettingGui(mdc.getEnableCapture(), "Recording", true).getComponent();
		IntSettingSpinner max = new IntSettingSpinner(mdc.getMaxCaptures(), "Max Snapshots");
		IntSettingSpinner minInterval = new IntSettingSpinner(mdc.getMsBetweenCaptures(), "Min Snap Interval (ms)");

		Component maxSpinner = max.getSpinnerOnly();
		maxSpinner.setPreferredSize(new Dimension(160, 25));
		Component minSpinner = minInterval.getSpinnerOnly();
		minSpinner.setPreferredSize(new Dimension(160, 25));

		ReadOnlyText help = new ReadOnlyText(helpText);

		GuiUtil.simpleTopDownLayout(panel,
				recording,
				Box.createVerticalStrut(10),
				max.getLabelOnly(),
				maxSpinner,
				Box.createVerticalStrut(10),
				minInterval.getLabelOnly(),
				minSpinner,
				Box.createVerticalStrut(10),
				help
		);
		panel.setMinimumSize(new Dimension(200, 40));
		panel.setPreferredSize(new Dimension(200, 4_000));
		panel.setMaximumSize(new Dimension(200, 4_000));

		return panel;
	}

	private static final String helpText = """
			What these numbers mean:

			The replay system will capture up to your chosen number of snapshots. The minimum interval determines how often snapshots can be taken.

			For example, if you set the max to 1000, and the interval to 500ms, it would keep up to 1000 snapshots, no closer than 500ms. Thus, you would have a minimum of 500 seconds of replay.

			Do note that setting too high of a maximum or too low of an interval can cause absurd memory usage.
						""";
}
