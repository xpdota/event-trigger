package gg.xp.gui.tables.filters;

import gg.xp.events.actlines.events.HasAbility;
import gg.xp.events.actlines.events.HasStatusEffect;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

public class EventAbilityOrBuffFilter extends TextBasedFilter<Object> {

	private static final Logger log = LoggerFactory.getLogger(EventAbilityOrBuffFilter.class);

	public EventAbilityOrBuffFilter(Runnable filterUpdatedCallback) {
		super(filterUpdatedCallback, "Ability/Buff", EventAbilityOrBuffFilter::getNameForItem);
	}

	private static String getNameForItem(Object item) {
		if (item instanceof HasAbility) {
			return (((HasAbility) item).getAbility().getName());
		}
		if (item instanceof HasStatusEffect) {
			return (((HasStatusEffect) item).getBuff().getName());
		}
		throw new IllegalStateException("Item not correct class - should have been pre-filtered");
	}

	private static long getIdForItem(Object item) {
		if (item instanceof HasAbility) {
			return (((HasAbility) item).getAbility().getId());
		}
		if (item instanceof HasStatusEffect) {
			return (((HasStatusEffect) item).getBuff().getId());
		}
		throw new IllegalStateException("Item not correct class - should have been pre-filtered");
	}

	@Override
	protected @Nullable Predicate<Object> getFilterForInput(String input) {
		if (input.startsWith("0x")) {
			validationError = false;
			// TODO: this is also inefficient because we should just be parsing the input text
			long wantedId;
			try {
				wantedId = Long.parseLong(input.substring(2).trim(), 16);
			}
			catch (NumberFormatException nfe) {
				validationError = true;
				return item -> false;
			}
			return item -> getIdForItem(item) == wantedId;
		}
		return super.getFilterForInput(input);
	}

	@Override
	public boolean preFilter(Object item) {
		// Pre-filter
		return (item instanceof HasAbility || item instanceof HasStatusEffect);
	}

}
