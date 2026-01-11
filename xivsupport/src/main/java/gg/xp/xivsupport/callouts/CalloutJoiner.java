package gg.xp.xivsupport.callouts;

import gg.xp.reevent.scan.Alias;
import gg.xp.reevent.scan.ScanMe;
import org.apache.commons.lang3.stream.Streams;

import java.util.stream.Collectors;

@ScanMe
@Alias("join")
public class CalloutJoiner {

	private final SingleValueReplacement svr;

	public CalloutJoiner(SingleValueReplacement svr) {
		this.svr = svr;
	}

	/**
	 * Exposes the 'join' helper function to callouts. This is different from Groovy's normal List.join in that it
	 * uses SingleValueReplacement, so callout styling will work.
	 *
	 * @param sep   Separator to use.
	 * @param items The items to join.
	 * @return The joined string
	 */
	public String call(String sep, Iterable<Object> items) {
		return Streams.of(items).map(svr::singleReplacement).collect(Collectors.joining(sep));
	}

}
