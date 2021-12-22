package gg.xp.reevent.time;

import java.time.Instant;

@FunctionalInterface
public interface CurrentTimeSource {

	Instant now();

}
