package gg.xp.xivsupport.custompartyoverlay.mpbar;

import gg.xp.xivsupport.custompartyoverlay.DataWatchingCustomPartyComponent;
import gg.xp.xivsupport.gui.tables.renderers.MpBar;
import gg.xp.xivsupport.models.ManaPoints;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class MpBarComponent extends DataWatchingCustomPartyComponent<ManaPoints> {
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
		forceApplyLastData();
	}

	@Override
	protected Component makeComponent() {
		return bar;
	}

	@Override
	protected ManaPoints extractData(@NotNull XivPlayerCharacter xpc) {
		return xpc.getMp();
	}

	@Override
	protected void applyData(ManaPoints data) {
		bar.setData(data);
		bar.repaint();
	}
}
