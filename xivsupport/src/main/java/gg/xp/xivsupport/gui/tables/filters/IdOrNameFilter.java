package gg.xp.xivsupport.gui.tables.filters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.function.Predicate;

public class IdOrNameFilter<X> extends TextBasedFilter<X> {

	private static final Logger log = LoggerFactory.getLogger(IdOrNameFilter.class);
	private final Function<X, Long> idExtractor;

	public IdOrNameFilter(String label, Function<X, Long> idExtractor, Function<X, String> nameExtractor, Runnable filterUpdatedCallback) {
		super(filterUpdatedCallback, label, nameExtractor);
		this.idExtractor = idExtractor;
	}

	@Override
	protected @Nullable Predicate<X> getFilterForInput(@NotNull String input) {
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
			return item -> idExtractor.apply(item) == wantedId;
		}
		// Since it's conceivable that a name could be numeric, make it an 'or'
		long wantedId;
		Predicate<X> sup = super.getFilterForInput(input);
		try {
			wantedId = Integer.parseInt(input);
		}
		catch (NumberFormatException e) {
			return sup;
		}
		Predicate<X> base10filter = item -> idExtractor.apply(item) == wantedId;
		if (sup == null) {
			return base10filter;
		}
		else {
			return sup.or(base10filter);
		}
	}
}
