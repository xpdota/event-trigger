package gg.xp.xivsupport.gameversion;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GameVersion implements Comparable<GameVersion>, Serializable {

	@Serial
	private static final long serialVersionUID = 6902676638616515964L;
	private final List<String> individualParts;

	private GameVersion(List<String> individualParts) {
		this.individualParts = individualParts;
	}

	public static GameVersion fromString(String value) {
		return new GameVersion(Arrays.stream(value.split("\\.")).toList());
	}

	@Override
	public String toString() {
		return String.join(".", individualParts);
	}

	public static final GameVersion UNKNOWN_LATEST = new GameVersion(Collections.singletonList("9999")) {
		@Override
		public String toString() {
			return "UNKNOWN_LATEST";
		}
	};

	@Override
	public int compareTo(@NotNull GameVersion that) {
		if (this.equals(that)) {
			return 0;
		}
		return compareVersions(this.individualParts, that.individualParts);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof GameVersion that) {
			if (this.individualParts.size() == that.individualParts.size()) {
				return Objects.equals(this.individualParts, that.individualParts);
			}
			else {
				return compareVersions(this.individualParts, that.individualParts) == 0;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(individualParts);
	}

	// This is a bit of a strange comparison, as we want to use non-number-aware string comparison
	// e.g. 6.15 < 6.2, despite 15 > 2.
	private static int compareVersions(List<String> thisItems, List<String> thatItems) {
		// Ran out of items in both lists
		if (thisItems.isEmpty() && thatItems.isEmpty()) {
			return 0;
		}
		String thisFirst;
		String thatFirst;
		// Flag to continue on. Don't bother continuing if we ran out of items.
		boolean cont = true;
		// If not present, assume zero
		if (thisItems.isEmpty()) {
			thisFirst = "0";
			cont = false;
		}
		else {
			thisFirst = thisItems.get(0);
		}
		// If not present, assume zero
		if (thatItems.isEmpty()) {
			thatFirst = "0";
			cont = false;
		}
		else {
			thatFirst = thatItems.get(0);
		}
		int immediateComparison;
		// Special case = treat any string of pure zeroes as being identical.
		if (Integer.parseInt(thisFirst) == 0 && Integer.parseInt(thatFirst) == 0) {
			immediateComparison = 0;
		}
		else {
			// If we have a difference, return that.
			immediateComparison = thisFirst.compareTo(thatFirst);
		}
		if (immediateComparison != 0) {
			return immediateComparison;
		}
		if (cont) {
			return compareVersions(thisItems.subList(1, thisItems.size()), thatItems.subList(1, thatItems.size()));
		}
		else {
			return 0;
		}
	}
}
