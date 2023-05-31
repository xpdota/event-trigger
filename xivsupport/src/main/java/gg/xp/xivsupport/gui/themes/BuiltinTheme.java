package gg.xp.xivsupport.gui.themes;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import gg.xp.xivsupport.gui.FlatDarkestLaf;
import gg.xp.xivsupport.gui.util.HasFriendlyName;

import javax.swing.*;
import java.util.function.Supplier;

public enum BuiltinTheme implements HasFriendlyName {

	DARK("Dark", FlatDarkLaf::new),
	LIGHT("Light", FlatLightLaf::new),
	DARK_HIGH_CONTRAST("Darker", FlatDarkestLaf::new);

	private final String friendlyName;
	private final Supplier<LookAndFeel> lafSupplier;

	BuiltinTheme(String friendlyName, Supplier<LookAndFeel> lafSupplier) {
		this.friendlyName = friendlyName;
		this.lafSupplier = lafSupplier;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}

	public LookAndFeel getLaf() {
		return lafSupplier.get();
	}
}
