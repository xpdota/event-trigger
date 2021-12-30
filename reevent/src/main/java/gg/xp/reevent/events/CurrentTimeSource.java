package gg.xp.reevent.events;

import java.time.Instant;

public interface CurrentTimeSource {
	Instant now();
}
