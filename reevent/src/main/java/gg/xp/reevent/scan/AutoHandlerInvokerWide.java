package gg.xp.reevent.scan;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.BiConsumer;

public class AutoHandlerInvokerWide implements AutoHandlerInvoker {
	private final BiConsumer consumer;

	public AutoHandlerInvokerWide(Method method, Object clazzInstance) {
		try {
			MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(method.getDeclaringClass(), MethodHandles.lookup());
			MethodHandle unreflected = lookup.unreflect(method);
//			consumer = createLambdaFactory(EventConsumer.class, method).apply(clazzInstance);
			MethodType ftype = MethodType.methodType(void.class, EventContext.class, method.getParameterTypes()[1]);
			if ((method.getModifiers() & Modifier.STATIC) > 0) {
				consumer = (BiConsumer) LambdaMetafactory.metafactory(
						lookup,
						"accept",
						MethodType.methodType(BiConsumer.class),
						ftype.erase(),
						unreflected,
						ftype
				).getTarget().invoke();
			}
			else {
				consumer = (BiConsumer) LambdaMetafactory.metafactory(
						lookup,
						"accept",
						MethodType.methodType(BiConsumer.class, method.getDeclaringClass()),
						ftype.erase(),
						unreflected,
						ftype
				).getTarget().invoke(clazzInstance);
			}
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean requiresContext() {
		return true;
	}

	@Override
	public void handle(EventContext context, Event event) {
		consumer.accept(context, event);
	}
}
