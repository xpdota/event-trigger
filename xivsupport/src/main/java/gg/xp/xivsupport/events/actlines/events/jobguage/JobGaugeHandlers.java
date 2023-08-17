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
			case WHM -> {
				out = doWhmGauge(event);
			}
			case SCH -> {
				out = doSchGauge(event);
			}
			case SGE -> {
				out = doSgeGauge(event);
			}
			case PLD -> {
				out = doPldGauge(event);
			}
			case WAR -> {
				out = doWarGauge(event);
			}
			case DRK -> {
				out = doDrkGauge(event);
			}
			case GNB -> {
				out = doGnbGauge(event);
			}
			default -> {
				return;
			}
		}
		context.accept(out);
	}

	private Event doWhmGauge(RawJobGaugeEvent event) {
		byte[] data = event.getRawData();

		long lilyDuration = bytesToInt(data[4], data[3]);
		int lilyCount = data[5];
		int bloodLily = data[6];

		return new WhmGaugeEvent(lilyDuration, lilyCount, bloodLily);
	}

	private Event doSchGauge(RawJobGaugeEvent event) {
		byte[] data = event.getRawData();

		int aetherflow = data[1];
		int faerieGauge = data[2];
		long seraphDuration = bytesToInt(data[4], data[3]);
		int unknown5 = data[5];

		return new SchGaugeEvent(aetherflow, faerieGauge, seraphDuration, unknown5);
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

	private Event doPldGauge(RawJobGaugeEvent event) {
		byte[] data = event.getRawData();
		int oathGauge = data[1];

		return new PldGaugeEvent(oathGauge);
	}

	private Event doWarGauge(RawJobGaugeEvent event) {
		byte[] data = event.getRawData();
		int beastGauge = data[1];

		return new WarGaugeEvent(beastGauge);
	}

	private Event doDrkGauge(RawJobGaugeEvent event) {
		byte[] data = event.getRawData();
		int bloodGauge = data[1];
		long darkSideDuration = bytesToInt(data[4], data[3]);
		long esteemDuration = bytesToInt(data[8], data[7]);

		return new DrkGaugeEvent(bloodGauge, darkSideDuration, esteemDuration);
	}

	private Event doGnbGauge(RawJobGaugeEvent event) {
		byte[] data = event.getRawData();
		int powderGauge = data[1];

		return new GnbGaugeEvent(powderGauge);
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
