package gg.xp.xivsupport.gui;

import com.formdev.flatlaf.FlatDarkLaf;

public class FlatDarkestLaf extends FlatDarkLaf {
	public static final String NAME = "FlatLaf Darcula";

	public FlatDarkestLaf() {
	}

	public static boolean setup() {
		return setup(new FlatDarkestLaf());
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public static boolean install() {
		return setup();
	}

	public static void installLafInfo() {
		installLafInfo("FlatLaf Darkest", FlatDarkestLaf.class);
	}

	public String getName() {
		return "FlatLaf Darkest";
	}

	public String getDescription() {
		return "FlatLaf Darkest Look and Feel";
	}

}
