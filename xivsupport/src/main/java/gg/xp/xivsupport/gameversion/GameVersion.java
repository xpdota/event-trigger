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
			return Objects.equals(this.individualParts, that.individualParts);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(individualParts);
	}

	private static int compareVersions(List<String> thisItems, List<String> thatItems) {
		if (thisItems.isEmpty()) {
			if (thatItems.isEmpty()) {
				return 0;
			}
			return -1;
		}
		else {
			if (thatItems.isEmpty()) {
				return 1;
			}
			return compareVersions(thisItems.subList(1, thisItems.size()), thatItems.subList(1, thatItems.size()));
		}
	}
}
