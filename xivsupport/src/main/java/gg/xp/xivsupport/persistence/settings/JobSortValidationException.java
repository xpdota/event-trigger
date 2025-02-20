package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivdata.data.*;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class JobSortValidationException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 6391762663338571558L;
	private final boolean silent;
	private final Set<Job> expected;
	private final List<Job> actual;

	JobSortValidationException(final String message, final boolean silent, Set<Job> expected, List<Job> actual) {
		super(message);
		this.silent = silent;
		this.expected = expected.isEmpty() ? Collections.emptySet() : EnumSet.copyOf(expected);
		this.actual = new ArrayList<>(actual);
	}

	public boolean isSilent() {
		return silent;
	}

	public Set<Job> getExpected() {
		return Collections.unmodifiableSet(expected);
	}

	public List<Job> getActual() {
		return Collections.unmodifiableList(actual);
	}
}
