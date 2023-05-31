package gg.xp.xivsupport.util;

import gg.xp.xivdata.data.rsv.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class RsvLookupUtil {

	public static <X> String lookup(long id, @Nullable String givenName, Function<Long, @Nullable X> dataLookup, Function<@NotNull X, String> nameGetter) {

		// This needs to handle a lot of cases in a priority system.
		// Here is the rough priority:
		// 1. ACT-provided good name
		// 2. Datafile-provided good name
		// 3. ACT-provided RSV name, with valid RSV lookup
		// 4. Datafile-provided RSV name, with valid RSV lookup
		// 5. ACT-provided RSV name, RSV lookup failed - return _rsv name
		// 6. Datafile-provided RSV name, RSV lookup failed - return _rsv name
		// 7. Unknown_xyz name

		// Fast path for #1
		if (givenName != null && !givenName.startsWith("_rsv")) {
			return givenName;
		}
		@Nullable X info = dataLookup.apply(id);
		@Nullable String nameFromInfo = info == null ? null : nameGetter.apply(info);
		if (givenName == null) {
			if (nameFromInfo == null) {
				// case #7
				return String.format("Unknown_%x", id);
			}
			// case #2, #4, #6 - we can skip others since givenName is null
			return DefaultRsvLibrary.tryResolve(nameFromInfo);
		}
		else {
			if (nameFromInfo == null) {
				return DefaultRsvLibrary.tryResolve(givenName);
			}
			// At this point, we *have* both, we need to pick one
			if (!givenName.startsWith("_rsv")) {
				return givenName;
			}
			if (!nameFromInfo.startsWith("_rsv")) {
				return nameFromInfo;
			}
			givenName = DefaultRsvLibrary.tryResolve(givenName);
			if (!givenName.startsWith("_rsv")) {
				return givenName;
			}
			nameFromInfo = DefaultRsvLibrary.tryResolve(nameFromInfo);
			if (!nameFromInfo.startsWith("_rsv")) {
				return nameFromInfo;
			}
			return givenName;
		}
	}

}
