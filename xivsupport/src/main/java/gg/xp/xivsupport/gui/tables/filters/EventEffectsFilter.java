package gg.xp.xivsupport.gui.tables.filters;

import gg.xp.xivsupport.events.actlines.events.HasEffects;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

public class EventEffectsFilter extends TextBasedFilter<Object> {

	private static final Logger log = LoggerFactory.getLogger(EventEffectsFilter.class);

	public EventEffectsFilter(Runnable filterUpdatedCallback) {
		super(filterUpdatedCallback, "Effects", EventEffectsFilter::getNameForItem);
	}

	private static String getNameForItem(Object item) {
		if (item instanceof HasEffects he) {
			return he.getEffects().stream().map(AbilityEffect::getDescription).collect(Collectors.joining("; "));
		}
		throw new IllegalStateException("Item not correct class - should have been pre-filtered");
	}

	@Override
	public boolean preFilter(Object item) {
		// Pre-filter
		return item instanceof HasEffects;
	}

}
