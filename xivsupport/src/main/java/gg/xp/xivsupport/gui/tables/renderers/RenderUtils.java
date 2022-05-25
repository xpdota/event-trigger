package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.reevent.events.Event;
import gg.xp.xivdata.data.ActionIcon;
import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivdata.data.HasIconURL;
import gg.xp.xivdata.data.HasOptionalIconURL;
import gg.xp.xivdata.data.StatusEffectLibrary;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public final class RenderUtils {
	private RenderUtils() {
	}

	public static void setTooltip(Component component, String tooltip) {
		if (component instanceof JComponent jc) {
			jc.setToolTipText(tooltip);
		}
	}

	public static Color withAlpha(Color base, int alpha) {
		return new Color(base.getRGB() & 0xffffff + (alpha << 24), true);
	}

	public static @Nullable HasIconURL guessIconFor(Object object) {
		if (object == null) {
			return null;
		}
		@Nullable HasIconURL iconSource;
		if (object instanceof HasIconURL hasIcon) {
			iconSource = hasIcon;
		}
		else if (object instanceof HasOptionalIconURL maybeHasIcon) {
			iconSource = maybeHasIcon.getIconUrl();
		}
		else if (object instanceof HasAbility ability) {
			ActionIcon ai = ActionLibrary.iconForId(ability.getAbility().getId());
			iconSource = ai == null ? null : ai.isDefaultIcon() ? null : ai;
		}
		else if (object instanceof HasStatusEffect status) {
			iconSource = StatusEffectLibrary.iconForId(status.getBuff().getId(), status.getStacks());
		}
		else {
			return null;
		}
		return iconSource;
	}
}
