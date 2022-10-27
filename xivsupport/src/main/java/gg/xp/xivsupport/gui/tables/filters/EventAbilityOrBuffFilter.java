package gg.xp.xivsupport.gui.tables.filters;

import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import org.jetbrains.annotations.NotNull;
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
		if (item instanceof HasAbility ha) {
			return ha.getAbility().getName();
		}
		if (item instanceof HasStatusEffect hse) {
			return hse.getBuff().getName();
		}
		throw new IllegalStateException("Item not correct class - should have been pre-filtered");
	}

	private static long getIdForItem(Object item) {
		if (item instanceof HasAbility hasAbility) {
			return hasAbility.getAbility().getId();
		}
		if (item instanceof HasStatusEffect hse) {
			return hse.getBuff().getId();
		}
		throw new IllegalStateException("Item not correct class - should have been pre-filtered");
	}

	// TODO: support ranges, commas, including mixed numeric/string, etc
	@Override
	protected @Nullable Predicate<Object> getFilterForInput(@NotNull String input) {
		if (input.startsWith("0x")) {
			validationError = false;
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
		// TODO: Also account for effect results
		long wantedId;
		Predicate<Object> stringFilter = super.getFilterForInput(input);
		if (stringFilter == null) {
			// null means NO FILTER i.e. thing -> true
			return null;
		}
		try {
			wantedId = Long.parseLong(input.trim(), 10);
		}
		catch (NumberFormatException nfe) {
			return stringFilter;
		}
		return stringFilter.or(item -> getIdForItem(item) == wantedId);

	}

	@Override
	public boolean preFilter(Object item) {
		// Pre-filter
		return item instanceof HasAbility || item instanceof HasStatusEffect;
	}

}
