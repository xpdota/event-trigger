package gg.xp.reevent.scan;

import gg.xp.reevent.context.StateStore;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventHandler;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.events.TypedEventHandler;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AutoChildEventHandlerTest {

	private static class FooEvent extends BaseEvent {

	}

	private static class UnrelatedEvent extends BaseEvent {

	}

	private interface FooEventHandler extends TypedEventHandler<FooEvent> {
		@Override
		default Class<FooEvent> getType() {
			return FooEvent.class;
		};
	}

	private static class FooEventHandlerImpl implements FooEventHandler {
		private int handled;
		@Override
		public void handle(EventContext context, FooEvent event) {
			handled++;
		}
	}

	private static class ExtendedFooEventHandler implements FooEventHandler {
		private int handled;
		@Override
		public void handle(EventContext context, FooEvent event) {
			handled++;
		}
	}

	private interface EventHandlerIntf extends FooEventHandler {
	}

	private static class EventHandlerIntfImpl implements EventHandlerIntf {
		private int handled;
		@Override
		public void handle(EventContext context, FooEvent event) {
			handled++;
		}
	}

	private static class MyHandlerPack extends AutoChildEventHandler {
		private int beHandled;
		@AutoFeed
		private final EventHandler<BaseEvent> beHandler = new TypedEventHandler<>() {
			@Override
			public Class<BaseEvent> getType() {
				return BaseEvent.class;
			}

			@Override
			public void handle(EventContext context, BaseEvent event) {
				beHandled++;
			}
		};

		private int feHandled;
		@AutoFeed
		private final EventHandler<FooEvent> feHandler = new TypedEventHandler<>() {
			@Override
			public Class<FooEvent> getType() {
				return FooEvent.class;
			}

			@Override
			public void handle(EventContext context, FooEvent event) {
				feHandled++;
			}
		};

		private int unrelatedHandled;
		@AutoFeed
		private final EventHandler<UnrelatedEvent> unrelatedEventEventHandler = new TypedEventHandler<>() {
			@Override
			public Class<UnrelatedEvent> getType() {
				return UnrelatedEvent.class;
			}

			@Override
			public void handle(EventContext context, UnrelatedEvent event) {
				unrelatedHandled++;
			}
		};

		@AutoFeed
		private final FooEventHandlerImpl feh = new FooEventHandlerImpl();

		@AutoFeed
		private final ExtendedFooEventHandler efeh = new ExtendedFooEventHandler();

		private int intfHandled;
		@AutoFeed
		private final EventHandlerIntf intf = (ctx, e) -> intfHandled++;

		@AutoFeed
		private final EventHandlerIntfImpl impl = new EventHandlerIntfImpl();
	}

	@Test
	void negativeTest() {
		MyHandlerPack mhp = new MyHandlerPack();
		mhp.initAutoChildHandler(ctx, new InitEvent());
		mhp.callChildEventHandlers(ctx, new UnrelatedEvent());

		Assert.assertEquals(mhp.beHandled, 1);

		Assert.assertEquals(mhp.unrelatedHandled, 1);

		Assert.assertEquals(mhp.feHandled, 0);
		Assert.assertEquals(mhp.feh.handled, 0);
		Assert.assertEquals(mhp.efeh.handled, 0);
		Assert.assertEquals(mhp.intfHandled, 0);
		Assert.assertEquals(mhp.impl.handled, 0);
	}

	@Test
	void positiveTest() {
		MyHandlerPack mhp = new MyHandlerPack();
		mhp.initAutoChildHandler(ctx, new InitEvent());
		mhp.callChildEventHandlers(ctx, new FooEvent());

		Assert.assertEquals(mhp.beHandled, 1);

		Assert.assertEquals(mhp.unrelatedHandled, 0);

		Assert.assertEquals(mhp.feHandled, 1);
		Assert.assertEquals(mhp.feh.handled, 1);
		Assert.assertEquals(mhp.efeh.handled, 1);
		Assert.assertEquals(mhp.intfHandled, 1);
		Assert.assertEquals(mhp.impl.handled, 1);
	}

	private static final DummyEventContext ctx = new DummyEventContext();
	private static final class DummyEventContext implements EventContext {

		@Override
		public void accept(Event event) {

		}

		@Override
		public void enqueue(Event event) {

		}

		@Override
		public StateStore getStateInfo() {
			return null;
		}
	}

}