package gg.xp.xivsupport.gui.map;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.ColorSetting;
import gg.xp.xivsupport.persistence.settings.ObservableSetting;

import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;

@ScanMe
public class MapColorSettings extends ObservableSetting {

	final ColorSetting selectionColor;
	final ColorSetting enemyColor;
	final ColorSetting fakeEnemyColor;
	final ColorSetting otherColor;
	final ColorSetting otherPlayerColor;
	final ColorSetting partyMemberColor;
	final ColorSetting localPcColor;
	final ColorSetting playerHitboxColor;
	final ColorSetting npcHitboxColor;
	final ColorSetting selectedBackground;
	final ColorSetting playerOmenOutlineColor;
	final ColorSetting npcOmenOutlineColor;
	final ColorSetting fakeNpcOmenOutlineColor;
	final ColorSetting tetherColor;

	private static class Defaults {
		private static final Color selectionColor = Color.CYAN;
		private static final Color enemyColor = new Color(145, 0, 0);
		private static final Color fakeEnemyColor = new Color(170, 120, 0);
		private static final Color otherColor = new Color(128, 128, 128);
		private static final Color otherPlayerColor = new Color(82, 204, 82);
		private static final Color partyMemberColor = new Color(104, 120, 222);
		private static final Color localPcColor = new Color(150, 199, 255);
		private static final Color playerHitboxColor = new Color(150, 150, 255, 120);
		private static final Color npcHitboxColor = new Color(250, 30, 30, 90);
		private static final Color selectedBackground = new Color(192, 255, 255, 175);
		private static final Color playerOmenOutlineColor = new Color(80, 200, 255);
		private static final Color npcOmenOutlineColor = new Color(255, 98, 0);
		private static final Color fakeNpcOmenOutlineColor = new Color(255, 49, 135);
		private static final Color tetherColor = new Color(103, 48, 179);
	}

	public MapColorSettings(PersistenceProvider pers) {
		String settingsKeyBase = "map-color-settings.";
		selectionColor = new ColorSetting(pers, settingsKeyBase + "selectionColor", Defaults.selectionColor);
		enemyColor = new ColorSetting(pers, settingsKeyBase + "enemyColor", Defaults.enemyColor);
		fakeEnemyColor = new ColorSetting(pers, settingsKeyBase + "fakeEnemyColor", Defaults.fakeEnemyColor);
		otherColor = new ColorSetting(pers, settingsKeyBase + "otherColor", Defaults.otherColor);
		otherPlayerColor = new ColorSetting(pers, settingsKeyBase + "otherPlayerColor", Defaults.otherPlayerColor);
		partyMemberColor = new ColorSetting(pers, settingsKeyBase + "partyMemberColor", Defaults.partyMemberColor);
		localPcColor = new ColorSetting(pers, settingsKeyBase + "localPcColor", Defaults.localPcColor);
		playerHitboxColor = new ColorSetting(pers, settingsKeyBase + "playerHitboxColor", Defaults.playerHitboxColor);
		npcHitboxColor = new ColorSetting(pers, settingsKeyBase + "npcHitboxColor", Defaults.npcHitboxColor);
		selectedBackground = new ColorSetting(pers, settingsKeyBase + "selectedBackground", Defaults.selectedBackground);
		playerOmenOutlineColor = new ColorSetting(pers, settingsKeyBase + "playerOmenOutlineColor", Defaults.playerOmenOutlineColor);
		npcOmenOutlineColor = new ColorSetting(pers, settingsKeyBase + "npcOmenOutlineColor", Defaults.npcOmenOutlineColor);
		fakeNpcOmenOutlineColor = new ColorSetting(pers, settingsKeyBase + "fakeNpcOmenOutlineColor", Defaults.fakeNpcOmenOutlineColor);
		tetherColor = new ColorSetting(pers, settingsKeyBase + "tetherColor", Defaults.tetherColor);

		List.of(selectionColor, enemyColor, fakeEnemyColor, otherColor, otherPlayerColor,
						partyMemberColor, localPcColor, playerHitboxColor, npcHitboxColor,
						selectedBackground, playerOmenOutlineColor, npcOmenOutlineColor, fakeNpcOmenOutlineColor,
						tetherColor)
				.forEach(i -> i.addListener(this::notifyListeners));
	}

	public Border getSelectionBorder() {
		return new LineBorder(selectionColor.get(), 2);
	}
}
