package gg.xp.xivsupport.custompartyoverlay.hpbar;

import gg.xp.xivsupport.custompartyoverlay.BasePartyListComponent;
import gg.xp.xivsupport.events.actionresolution.SequenceIdTracker;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.gui.tables.renderers.HpBar;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

public class HpBarComponent extends BasePartyListComponent {
	private final HpBar bar = new HpBar();
	private final BooleanSetting showPredictedHp;
	private final SequenceIdTracker sqidTracker;
	private final HpBarComponentConfig config;

	public HpBarComponent(StandardColumns cols, SequenceIdTracker sqidTracker, HpBarComponentConfig config) {
		// TODO: this should really move
		this.showPredictedHp = cols.getShowPredictedHp();
		this.sqidTracker = sqidTracker;
		this.config = config;
		config.addAndRunListener(this::applySettings);
	}

	private void applySettings() {
		bar.setBgTransparency(config.getBgTransparency().get());
		bar.setFgTransparency(config.getFgTransparency().get());
		bar.setBackground(config.getBackgroundColor().get());
		bar.setEmptyGradientColor(config.getHpGradientEmpty().get());
		bar.setFullGradientColor(config.getHpGradientFull().get());
		bar.setFullHpColor(config.getFullHpColor().get());
		bar.setShieldColor(config.getShieldColor().get());
		bar.setTextColor(config.getTextColor().get());
		bar.setTextMode(config.getFractionDisplayMode().get());
	}

	@Override
	protected Component makeComponent() {
		return bar;
	}

	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		long pending;
		if (showPredictedHp.get()) {
			List<AbilityUsedEvent> events = sqidTracker.getEventsTargetedOnEntity(xpc);
			long dmg = 0;
			for (AbilityUsedEvent event : events) {
				dmg += event.getDamage();
			}
			pending = dmg;
		}
		else {
			pending = 0;
		}
		bar.setData(xpc, pending * -1);
//		bar.revalidate();
	}
}
