package gg.xp.xivsupport.gui.tables.filters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

public class IdFilter<X> extends TextBasedFilter<X> {

	private final Function<X, Long> idExtractor;

	public IdFilter(Runnable filterUpdatedCallback, String label, Function<X, Long> idExtractor) {
		super(filterUpdatedCallback, label, unused -> "");
		this.idExtractor = idExtractor;
	}

	@Override
	protected String boxToolTip() {
		return "Numeric IDs may be entered as base 10 (1234) or hex (0x15AB)";
	}

	@Override
	protected @Nullable Predicate<X> getFilterForInput(@NotNull String input) {
		validationError = false;
		if (input.isEmpty()) {
			return null;
		}
		String trimmed = input.trim();
		long wantedId;
		try {
			if (trimmed.startsWith("0x")) {
				wantedId = Long.parseLong(trimmed.substring(2).trim(), 16);
			}
			else {
				wantedId = Long.parseLong(trimmed);
			}
		}
		catch (NumberFormatException nfe) {
			validationError = true;
			return item -> false;
		}
		return item -> {
			Long id = idExtractor.apply(item);
			return id != null && id == wantedId;
		};
	}
}
