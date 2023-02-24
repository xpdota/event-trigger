package gg.xp.xivsupport.groovy;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.events.triggers.marks.adv.SpecificAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.adv.SpecificAutoMarkSlotRequest;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import groovy.lang.Closure;
import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyInterceptable;
import groovy.lang.MetaClass;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.function.Consumer;

@ScanMe
public class AmHelper {

	private final EventMaster master;

	public AmHelper(EventMaster master) {
		this.master = master;
	}

	public class Builder {
		XivPlayerCharacter player;
		MarkerSign marker = MarkerSign.ATTACK_NEXT;
		Integer slot;
		Consumer<Event> consumer = master::pushEvent;

		public Builder player(XivPlayerCharacter player) {
			this.player = player;
			return this;
		}

		public Builder target(XivPlayerCharacter player) {
			this.player = player;
			return this;
		}

		public Builder slot(int slot) {
			if (slot < 1 || slot > 8) {
				throw new IllegalArgumentException("Slot should be between 1 and 8");
			}
			this.slot = slot;
			return this;
		}

		public Builder with(MarkerSign marker) {
			this.marker = marker;
			return this;
		}

		public Builder with(String marker) {
			this.marker = MarkerSign.of(marker);
			return this;
		}

		public Builder consumer(Closure<?> consumer) {
			this.consumer = consumer::call;
			return this;
		}

		public Event build() {
			if (player == null) {
				if (slot == null) {
					throw new IllegalArgumentException("Either 'player' or 'slot' must be specified.");
				}
				else {
					return new SpecificAutoMarkSlotRequest(slot, marker);
				}
			}
			else {
				return new SpecificAutoMarkRequest(player, marker);
			}
		}

		public void buildAndFire() {
			consumer.accept(build());
		}
	}

	public void mark(Closure<?> closure) {
		Builder builder = new Builder();
		closure.setDelegate(builder);
		closure.run();
		builder.buildAndFire();
	}

}
