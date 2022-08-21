package gg.xp.xivsupport.gui.overlay;

import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.callouts.CalloutProcessor;
import gg.xp.xivsupport.callouts.gui.CalloutsConfigTab;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.RefreshingHpBar;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.ManaPoints;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.EnumSettingGui;
import gg.xp.xivsupport.persistence.gui.FontSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.speech.BasicCalloutEvent;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class CalloutStyleConfigGui implements PluginTab {

	private final FlyingTextOverlay overlay;
	private final CalloutProcessor calloutProcessor;
	private final EventMaster master;

	public CalloutStyleConfigGui(FlyingTextOverlay overlay, CalloutProcessor calloutProcessor, EventMaster master) {
		this.overlay = overlay;
		this.calloutProcessor = calloutProcessor;
		this.master = master;
	}


	@Override
	public String getTabName() {
		return "Callout Styling";
	}

	@Override
	public Component getTabContents() {
		JTabbedPane tpane = new JTabbedPane();
		{
			JPanel panel = new TitleBorderFullsizePanel("Visual Callouts");
//			panel.setLayout(new GridBagLayout());
			BooleanSetting enabled = overlay.getEnabled();
			enabled.addListener(panel::repaint);

			JCheckBox enableDisable = new BooleanSettingGui(enabled, "Flying Text Enabled").getComponent();
			Component alignment = new EnumSettingGui<>(overlay.getAlignmentSetting(), "Text Alignment", enabled::get).getComponent();
			Component color = new ColorSettingGui(overlay.getTextColorSetting(), "Text Color", enabled::get).getComponent();
			Component font = new FontSettingGui(overlay.getTextFontSetting(), "Text Font", GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()).getComponent();

			JButton testButton1 = new JButton("Display Simple Test Callout");
			testButton1.addActionListener(l -> {
				master.pushEvent(new BasicCalloutEvent(null, "Sample Text Goes Here"));
			});
			JButton testButton2 = new JButton("Display Icon Test Callout");
			testButton2.addActionListener(l -> {
				master.pushEvent(new BasicCalloutEvent(null, "Sample Text Goes Here") {
					@Override
					public @Nullable Component graphicalComponent() {
						return IconTextRenderer.getStretchyIcon(StatusEffectLibrary.iconForId(158, 0));
					}
				});
			});
			JButton testButton3 = new JButton("Display Component Test Callout");
			testButton3.addActionListener(l -> {
				master.pushEvent(new BasicCalloutEvent(null, "Sample Text Goes Here") {
					@Override
					public @Nullable Component graphicalComponent() {
						XivCombatant dummyCombatant = new XivCombatant(
								0x1001_1001,
								"Test Combatant",
								false,
								false,
								2,
								new HitPoints(12345, 34567),
								ManaPoints.of(5000, 10000),
								null,
								0,
								0,
								0,
								90,
								0,
								5000
						);
						RefreshingHpBar refreshingHpBar = new RefreshingHpBar(() -> dummyCombatant);
						refreshingHpBar.setPreferredSize(new Dimension(200, 100));
						return refreshingHpBar;
					}
				});
			});

			GuiUtil.simpleTopDownLayout(panel, 400, enableDisable, alignment, color, font, testButton1, testButton2, testButton3);
			tpane.add("Visual Callouts", panel);
		}
		{
			JPanel panel = new TitleBorderFullsizePanel("Name Conversions");
			JCheckBox replaceYou = new BooleanSettingGui(calloutProcessor.getReplaceYou(), "Replace your own name with 'YOU'").getComponent();
			Component playerNameStylePanel = new EnumSettingGui<>(calloutProcessor.getPcNameStyle(), "Player Name Style", () -> true).getComponent();
			GuiUtil.simpleTopDownLayout(panel, 400, replaceYou, playerNameStylePanel);
			tpane.add("Name Conversions", panel);
		}
		return tpane;
	}

	@Override
	public int getSortOrder() {
		return 2;
	}
}
