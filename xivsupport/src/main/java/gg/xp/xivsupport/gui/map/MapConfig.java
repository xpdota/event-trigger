package gg.xp.xivsupport.gui.map;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.map.omen.OmenDisplayMode;
import gg.xp.xivsupport.gui.tabs.SmartTabbedPane;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.EnumSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class MapConfig {

	private static final Logger log = LoggerFactory.getLogger(MapConfig.class);
	private final MapDataController mdc;
	private final MapDisplayConfig displayConf;
	private final MapColorSettings mcs;

	public MapConfig(MapDataController mdc, MapDisplayConfig displayConf, MapColorSettings mcs) {
		this.mdc = mdc;
		this.displayConf = displayConf;
		this.mcs = mcs;
	}

	private static final class InnerPanel extends JPanel implements Scrollable {

		{
			setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			setMinimumSize(new Dimension(200, 40));
			setPreferredSize(new Dimension(200, 1_000));
			setMaximumSize(new Dimension(200, 1_000));
		}

		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 20;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			return (int) (getVisibleRect().height / 2.5);
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
	}

	public Component makeComponent() {
		SmartTabbedPane tabs = new SmartTabbedPane();

		JPanel displaySettings = new InnerPanel();
		{
			EnumSettingGui<NameDisplayMode> nameSetting = new EnumSettingGui<>(displayConf.getNameDisplayMode(), "Player Names", () -> true);
			EnumSettingGui<OmenDisplayMode> omenSetting = new EnumSettingGui<>(displayConf.getOmenDisplayMode(), "AoEs (BETA)", () -> true);
			BooleanSettingGui hpBars = new BooleanSettingGui(displayConf.getHpBars(), "HP Bars");
			BooleanSettingGui castBars = new BooleanSettingGui(displayConf.getCastBars(), "Cast Bars");
			BooleanSettingGui ids = new BooleanSettingGui(displayConf.getIds(), "IDs");
			BooleanSettingGui tethers = new BooleanSettingGui(displayConf.getTethers(), "Tethers");
			BooleanSettingGui hitboxes = new BooleanSettingGui(displayConf.getDisplayHitboxes(), "Hitboxes");

			GuiUtil.simpleTopDownLayout(displaySettings,
					nameSetting.getLabel(),
					nameSetting.getComboBoxOnly(),
					Box.createVerticalStrut(5),
					omenSetting.getLabel(),
					omenSetting.getComboBoxOnly(),
					new ReadOnlyText("This feature is beta. Do not report bugs with regards to the display (or lack thereof) of a particular ability."),
					Box.createVerticalStrut(10),
					ids.getComponent(),
					hpBars.getComponent(),
					castBars.getComponent(),
					tethers.getComponent(),
					hitboxes.getComponent()
			);
			tabs.addTabEager("Display", displaySettings);
		}

		JPanel recordingSettings = new InnerPanel();
		{
			JCheckBox recording = new BooleanSettingGui(mdc.getEnableCapture(), "Recording", true).getComponent();
			IntSettingSpinner max = new IntSettingSpinner(mdc.getMaxCaptures(), "Max Snapshots");
			IntSettingSpinner minInterval = new IntSettingSpinner(mdc.getMsBetweenCaptures(), "Min Snap Interval (ms)");

			Component maxSpinner = max.getSpinnerOnly();
			maxSpinner.setPreferredSize(new Dimension(160, 25));
			Component minSpinner = minInterval.getSpinnerOnly();
			minSpinner.setPreferredSize(new Dimension(160, 25));

			ReadOnlyText help = new ReadOnlyText(helpText);

			GuiUtil.simpleTopDownLayout(recordingSettings,
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
			tabs.addTabEager("Recording", recordingSettings);
		}

		JPanel colorSettings = new InnerPanel();
		{
			GuiUtil.simpleTopDownLayout(colorSettings,
					new ColorSettingGui(mcs.selectionColor, "Selection").getComponentReversed(),
					new ColorSettingGui(mcs.selectedBackground, "Selection BG").getComponentReversed(),
					new ColorSettingGui(mcs.localPcColor, "The Player").getComponentReversed(),
					new ColorSettingGui(mcs.partyMemberColor, "Party Member").getComponentReversed(),
					new ColorSettingGui(mcs.otherPlayerColor, "Other PC").getComponentReversed(),
					new ColorSettingGui(mcs.enemyColor, "Enemy").getComponentReversed(),
					new ColorSettingGui(mcs.fakeEnemyColor, "Fake Enemy").getComponentReversed(),
					new ColorSettingGui(mcs.otherColor, "Other Entity").getComponentReversed(),
					new ColorSettingGui(mcs.playerHitboxColor, "Player Hitbox").getComponentReversed(),
					new ColorSettingGui(mcs.npcHitboxColor, "NPC Hitbox").getComponentReversed(),
					new ColorSettingGui(mcs.playerOmenOutlineColor, "Player Omen").getComponentReversed(),
					new ColorSettingGui(mcs.npcOmenOutlineColor, "NPC Omen").getComponentReversed(),
					new ColorSettingGui(mcs.fakeNpcOmenOutlineColor, "Fake Omen").getComponentReversed(),
					new ColorSettingGui(mcs.tetherColor, "Tethers").getComponentReversed()
			);
			tabs.addTabEager("Colors", colorSettings);
		}

		return tabs;
	}

	private static final String helpText = """
			What these numbers mean:

			The replay system will capture up to your chosen number of snapshots. The minimum interval determines how often snapshots can be taken.

			For example, if you set the max to 1000, and the interval to 500ms, it would keep up to 1000 snapshots, no closer than 500ms. Thus, you would have a minimum of 500 seconds of replay.

			Do note that setting too high of a maximum or too low of an interval can cause absurd memory usage.
			""";
}
