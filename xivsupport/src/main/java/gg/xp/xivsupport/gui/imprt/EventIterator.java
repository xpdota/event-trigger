package gg.xp.xivsupport.gui.imprt;

import gg.xp.reevent.events.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a feed of events.
 * <p>
 * Thread safety is important here. You should expect that getNext() will only be called from a single thread, BUT
 * that hasMore() may be called from any thread. Thus, you should employ a strategy such as simply using a boolean
 * field which you update after polling na event.
 *
 * @param <X> The event type.
 */
public interface EventIterator<X extends Event> {

	boolean hasMore();

	/**
	 * @return Next event, or null if no further events exist
	 */
	@Nullable X getNext();

	default @Nullable Integer totalEvents() {
		return null;
	}

	;

}
