package gg.xp.reevent.scan;

import gg.xp.reevent.context.StateStore;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.InitEvent;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

public class LambdaMetafactoryTest {

	private int counter;
	private void markInvocation() {
		counter++;
	}
	private void checkInvocation() {
		Assert.assertEquals(counter, 1);
	}
	public void handleBasic(Event event) {
		markInvocation();
	}

	public void handleInstCompact(InitEvent event) {
		markInvocation();
	}

	public void handleInstWide(EventContext context, InitEvent event) {
		markInvocation();
	}

	public static void handleStaticCompact(InitEvent event) {
//		markInvocation();
	}

	public static void handleStaticWide(EventContext context, InitEvent event) {
//		markInvocation();
	}

	private static final EventContext dummyContext = new EventContext() {
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
	};

	private static final InitEvent dummyEvent = new InitEvent();

	@Test
	void testBasic() throws NoSuchMethodException {
		Method method = LambdaMetafactoryTest.class.getDeclaredMethod("handleBasic", Event.class);
		AutoHandlerInvokerCompact invoker = new AutoHandlerInvokerCompact(method, this);
		invoker.handle(dummyContext, dummyEvent);
	}

	@Test
	void testInstCompact() throws NoSuchMethodException {
		Method method = LambdaMetafactoryTest.class.getDeclaredMethod("handleInstCompact", InitEvent.class);
		AutoHandlerInvokerCompact invoker = new AutoHandlerInvokerCompact(method, this);
		invoker.handle(dummyContext, dummyEvent);
	}

	@Test
	void testStaticCompact() throws NoSuchMethodException {
		Method method = LambdaMetafactoryTest.class.getDeclaredMethod("handleStaticCompact", InitEvent.class);
		AutoHandlerInvokerCompact invoker = new AutoHandlerInvokerCompact(method, null);
		invoker.handle(dummyContext, dummyEvent);
	}

}
