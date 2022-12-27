package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class FontSetting {
	private final ValueSetting<String> fontName;
	private final BooleanSetting bold;
	private final BooleanSetting italic;
	private final IntSetting size;

	private Font cached;
	private boolean hasCachedValue;

	public FontSetting(PersistenceProvider persistence, String settingKeyBase, String defaultFontName, int defaultSize) {
		this.fontName = new ValueSetting<String>(persistence, settingKeyBase + ".font-name", defaultFontName);
		this.fontName.addListener(() -> SwingUtilities.invokeLater(this::resetCache));
		this.bold = new BooleanSetting(persistence, settingKeyBase + ".bold", false);
		this.bold.addListener(() -> SwingUtilities.invokeLater(this::resetCache));
		this.italic = new BooleanSetting(persistence, settingKeyBase + ".italic", false);
		this.italic.addListener(() -> SwingUtilities.invokeLater(this::resetCache));
		this.size = new IntSetting(persistence, settingKeyBase + ".size", defaultSize);
		this.size.addListener(() -> SwingUtilities.invokeLater(this::resetCache));
	}

	public ValueSetting<String> getFontName() {
		return fontName;
	}

	public BooleanSetting getBold() {
		return bold;
	}

	public BooleanSetting getItalic() {
		return italic;
	}

	public IntSetting getSize() {
		return size;
	}

	private void resetCache() {
		hasCachedValue = false;
	}

	public Font get() {
		if (!hasCachedValue) {
			int style = Font.PLAIN;
			if (bold.get()) {
				style |= Font.BOLD;
			}
			if (italic.get()) {
				style |= Font.ITALIC;
			}
			cached = new Font(fontName.get(), style, size.get());
			hasCachedValue = true;
		}
		return cached;
	}
}
