package gg.xp.xivsupport.events.actlines.events.jobguage;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.RawJobGaugeEvent;

public class JobGaugeHandlers {


	@HandleEvents
	public void handleEvents(EventContext context, RawJobGaugeEvent event) {
		Event out;
		switch (event.getJob()) {
			case SGE -> {
				out = doSgeGauge(event);
			}
			default -> {
				return;
			}
		}
		context.accept(out);
	}

	private Event doSgeGauge(RawJobGaugeEvent event) {
		byte[] data = event.getRawData();
		// Out of 20000 ms
		long addersGallProgess = bytesToInt(data[2], data[1]);
		int fullStacks = data[3] & 0xff;
		double addersGallOverall = fullStacks + addersGallProgess / (double) JobGaugeConstants.SGE_GAUGE_RECHARGE_TIME;
		int adderSting = data[4];
		boolean eukrasiaActive = data[5] > 0;

		return new SgeGaugeEvent(addersGallOverall, adderSting, eukrasiaActive);
	}

	private static long bytesToLong(byte... bytes) {
		long out = 0;
		for (int i = 0; i < bytes.length; i++) {
			out <<= 8;
			out += bytes[i] & 0xff;
		}
		return out;
	}

	private static long bytesToInt(byte... bytes) {
		int out = 0;
		for (int i = 0; i < bytes.length; i++) {
			out <<= 8;
			out += bytes[i] & 0xff;
		}
		return out;
	}
}
