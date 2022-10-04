package gg.xp.xivsupport.gui.overlay;

import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.callouts.CalloutProcessor;
import gg.xp.xivsupport.callouts.conversions.GlobalArenaSectorConverter;
import gg.xp.xivsupport.callouts.conversions.DefaultArenaSectorConversion;
import gg.xp.xivsupport.callouts.conversions.GlobalCallReplacer;
import gg.xp.xivsupport.callouts.gui.GlobalReplacementGui;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.RefreshingHpBar;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.ManaPoints;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.EnumSettingGui;
import gg.xp.xivsupport.persistence.gui.FontSettingGui;
import gg.xp.xivsupport.persistence.gui.StringSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.EnumSetting;
import gg.xp.xivsupport.speech.BasicCalloutEvent;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ScanMe
public class CalloutStyleConfigGui implements PluginTab {

	private final FlyingTextOverlay overlay;
	private final CalloutProcessor calloutProcessor;
	private final EventMaster master;
	private final GlobalArenaSectorConverter asc;
	private final GlobalCallReplacer gcr;

	public CalloutStyleConfigGui(FlyingTextOverlay overlay, CalloutProcessor calloutProcessor, EventMaster master, GlobalArenaSectorConverter asc, GlobalCallReplacer gcr) {
		this.overlay = overlay;
		this.calloutProcessor = calloutProcessor;
		this.master = master;
		this.asc = asc;
		this.gcr = gcr;
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
			tpane.add("Player Names", panel);
		}
		{
			JPanel panel = new TitleBorderFullsizePanel("Arena Sector Conversions");
			List<Component> components = new ArrayList<>();
			EnumSetting<DefaultArenaSectorConversion> mainSetting = asc.getMainSetting();
			mainSetting.addListener(panel::repaint);
			components.add(new EnumSettingGui<>(mainSetting, "Default Style", () -> true).getComponent());

			components.add(new JLabel("Custom Values (select 'Custom' in the box above):"));
			for (var entry : asc.getPerSectorSettings().entrySet()) {
				ArenaSector sector = entry.getKey();
				components.add(
						new StringSettingGui(entry.getValue(),
								sector == ArenaSector.UNKNOWN ? "Unknown/Error" : sector.getFriendlyName(),
								() -> mainSetting.get() == DefaultArenaSectorConversion.CUSTOM).getComponent());
			}

			GuiUtil.simpleTopDownLayout(panel, 400, components.toArray(Component[]::new));
			tpane.add("Arena Directions", panel);
		}
		{
			GlobalReplacementGui grg = new GlobalReplacementGui(gcr);
			tpane.add("Text Replacements", grg);
		}

		return tpane;
	}

	@Override
	public int getSortOrder() {
		return 2;
	}
}
