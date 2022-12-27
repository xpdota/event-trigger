package gg.xp.xivsupport.custompartyoverlay.buffs;

import gg.xp.xivsupport.custompartyoverlay.BasePartyListComponent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

public abstract class BaseBuffsBarPartyComponent extends BasePartyListComponent {
	private final BuffsBar bar = new BuffsBar();
	private final BuffsBarConfig config;

	protected BaseBuffsBarPartyComponent(BuffsBarConfig config) {
		this.config = config;
		config.addAndRunListener(this::applySettings);
	}

	private void applySettings() {
		bar.setNormalBuffColor(config.getNormalTextColor().get());
		bar.setMyBuffColor(config.getMyBuffTextColor().get());
		bar.setRemovableBuffColor(config.getRemoveableBuffColor().get());
		bar.setXPadding(config.getxPadding().get());
		bar.setEnableShadows(config.getShadows().get());
		bar.setEnableTimers(config.getTimers().get());
		bar.setRtl(config.getRtl().get());
		bar.reformat();
	}

	@Override
	protected Component makeComponent() {
		return bar;
	}

	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		bar.setBuffs(getBuffsToDisplay(xpc));
	}

	protected abstract List<BuffApplied> getBuffsToDisplay(XivPlayerCharacter xpc);

}
