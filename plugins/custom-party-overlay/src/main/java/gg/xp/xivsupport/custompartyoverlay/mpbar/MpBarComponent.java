package gg.xp.xivsupport.custompartyoverlay.mpbar;

import gg.xp.xivsupport.custompartyoverlay.BasePartyListComponent;
import gg.xp.xivsupport.gui.tables.renderers.MpBar;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class MpBarComponent extends BasePartyListComponent {
	private final MpBar bar = new MpBar();
	private final MpBarComponentConfig config;

	public MpBarComponent(MpBarComponentConfig config) {
		this.config = config;
		config.addAndRunListener(this::applySettings);
	}
	private void applySettings() {
		bar.setBgTransparency(config.getBgTransparency().get());
		bar.setFgTransparency(config.getFgTransparency().get());
		bar.setBackground(config.getBackgroundColor().get());
		bar.setMpColor(config.getMpColor().get());
		bar.setTextColor(config.getTextColor().get());
		bar.setTextMode(config.getFractionDisplayMode().get());
		bar.revalidate();
	}

	@Override
	protected Component makeComponent() {
		return bar;
	}

	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		bar.setData(xpc);
//		SwingUtilities.invokeLater(bar::revalidate);
	}
}
