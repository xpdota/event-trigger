package gg.xp.xivsupport.events.actlines.events.jobguage;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.JobGaugeUpdate;
import gg.xp.xivsupport.events.actlines.events.RawJobGaugeEvent;
import org.jetbrains.annotations.Nullable;

public final class JobGaugeHandlers {

	private JobGaugeHandlers() {
	}

	@HandleEvents
	public static void handleEvents(EventContext context, RawJobGaugeEvent event) {
		@Nullable JobGaugeUpdate out = parse(event);
		if (out != null) {
			context.accept(out);
		}
	}

	public static @Nullable JobGaugeUpdate parse(RawJobGaugeEvent event) {
		byte[] data = event.getRawData();
		return switch (event.getJob()) {
			case WHM -> WhmGaugeEvent.fromRaw(data);
			case SCH -> SchGaugeEvent.fromRaw(data);
			case AST -> AstGaugeEvent.fromRaw(data);
			case SGE -> SgeGaugeEvent.fromRaw(data);
			case PLD -> PldGaugeEvent.fromRaw(data);
			case WAR -> WarGaugeEvent.fromRaw(data);
			case DRK -> DrkGaugeEvent.fromRaw(data);
			case GNB -> GnbGaugeEvent.fromRaw(data);
			case RPR -> RprGaugeEvent.fromRaw(data);
			case MCH -> MchGaugeEvent.fromRaw(data);
			default -> null;
		};
	}

	/**
	 * Convert bytes into a single long value. The rightmost input is considered to be
	 * the least significant part (big endian). Behavior is undefined if passing more than
	 * 8 bytes.
	 *
	 * @param bytes The byte.
	 * @return The long value.
	 */
	public static long bytesToLong(byte... bytes) {
		int out = 0;
		for (byte b : bytes) {
			out <<= 8;
			out += b & 0xff;
		}
		return out;
	}
}
