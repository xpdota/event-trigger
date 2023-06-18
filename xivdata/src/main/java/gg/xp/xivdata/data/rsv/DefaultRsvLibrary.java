package gg.xp.xivdata.data.rsv;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public final class DefaultRsvLibrary {

	private static RsvLibrary library = new NoopRsvLibrary();

	private DefaultRsvLibrary() {
	}

	public static RsvLibrary getLibrary() {
		return library;
	}

	public static void setLibrary(RsvLibrary library) {
		DefaultRsvLibrary.library = library;
	}

	public static @Nullable String get(String key) {
		String libValue = library.get(key);
		if (libValue == null) {
			return null;
		}
		return libValue.intern();
	}

	@Contract("null -> null")
	public static String tryResolve(String original) {
		if (original == null) {
			return null;
		}
		if (original.startsWith("_rsv")) {
			String decodedMaybe = get(original);
			if (decodedMaybe != null) {
				return decodedMaybe;
			}
		}
		return original;
	}
}
