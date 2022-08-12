package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivsupport.events.actionresolution.SequenceIdTracker;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.gui.tables.renderers.HpBar;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

public class HpBarComponent extends BasePartyListComponent{
	private final HpBar bar = new HpBar();
	private final BooleanSetting showPredictedHp;
	private final SequenceIdTracker sqidTracker;

	public HpBarComponent(BooleanSetting showPredictedHp, SequenceIdTracker sqidTracker) {
		this.showPredictedHp = showPredictedHp;
		this.sqidTracker = sqidTracker;
		bar.setBgTransparency(128);
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
		bar.revalidate();
	}
}
