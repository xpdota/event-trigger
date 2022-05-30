package gg.xp.reevent.scan;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventHandler;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.events.TypedEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public abstract class AutoChildEventHandler {

	private static final Logger log = LoggerFactory.getLogger(AutoChildEventHandler.class);

	private final List<ChildEventHandler<?>> handlers = new ArrayList<>();
	private boolean isInitDone;

	private record ChildEventHandler<X extends Event>(EventHandler<X> handler, Class<X> type, String name) {

		private boolean isValidFor(Object obj) {
			return type.isInstance(obj);
		}

		private void run(EventContext ctx, Object event) {
			if (isValidFor(event)) {
				try {
					//noinspection unchecked
					handler.handle(ctx, (X) event);
				}
				catch (Throwable t) {
					log.error("Error invoking child event handler '{}'", name, t);
				}
			}
		}
	}

	protected AutoChildEventHandler() {
	}

	@HandleEvents
	public void initAutoChildHandler(EventContext ctx, InitEvent event) {
		checkInit();
	}

	private void checkInit() {
		if (isInitDone) {
			return;
		}
		try {
			List<Field> fields = getAllAssignableClasses(getClass()).stream()
					.flatMap(cls -> Arrays.stream(cls.getDeclaredFields()))
					.toList();

			for (Field field : fields) {
				if (!field.isAnnotationPresent(AutoFeed.class)) {
					continue;
				}
				TypedEventHandler<?> handler;
				try {
					field.setAccessible(true);
					Object fieldValue = field.get(this);
					if (fieldValue instanceof TypedEventHandler<?> teh) {
						handler = teh;
					}
					else {
						continue;

					}
				}
				catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
				//noinspection unchecked,rawtypes
				handlers.add(new ChildEventHandler(handler, handler.getType(), field.getName()));
//					Class<?> realType;
//					// Two possibilities:
//					// 1. The item directly implements EventHandler<X>, in which case we get the generic type directly from
//					// the field definition.
//					// 2. The item extends another class that implements EventHandler<X>, in which case we need to get the
//					// type from the superclass.
//					// 3. The item implements an interface that extends EventHandler<X>, in which case we need to figure out
//					// *which*
//
//					// Option 1
//					if (field.getType().equals(EventHandler.class)) {
//						realType = getGenericTypeParamFrom(field.getGenericType());
//					}
//					else {
//						Class<?> fieldType = field.getType();
//						Type ehType = walkSuperAndInterfaces(fieldType, EventHandler.class);
//						if (ehType != null) {
//							realType = getGenericTypeParamFrom(ehType);
//						}
//						else {
//							continue;
//						}
//					}
//					EventHandler<?> handler;
//					try {
//						field.setAccessible(true);
//						handler = (EventHandler<?>) field.get(this);
//					}
//					catch (IllegalAccessException e) {
//						throw new RuntimeException(e);
//					}
//					handlers.add(new ChildEventHandler(handler, realType, field.getName()));
			}
			log.info("Class {}: found {} child event handlers", getClass().getSimpleName(), handlers.size());
		}
		finally {
			isInitDone = true;
		}
	}

	//	private static @Nullable Type walkSuperAndInterfaces(Class<?> clazz, Class<?> target) {
//		while (clazz != null && !clazz.equals(Object.class)) {
//			if (target.equals(clazz.getSuperclass())) {
//				return getGenericTypeParamFrom(clazz.getGenericSuperclass());
//			}
//			else {
//				Optional<Type> intf = Arrays.stream(clazz.getGenericInterfaces()).filter(i -> target.isAssignableFrom(typeToClass(i))).findAny();
//				if (intf.isPresent()) {
//					Type type = intf.get();
//					Class<?> foundCls = typeToClass(type);
//					if (target.equals(foundCls)) {
//						return type;
//					}
//					return walkSuperAndInterfaces(foundCls, target);
//				}
//				clazz = clazz.getSuperclass();
//			}
//		}
//		return null;
//	}
//
//	private static Class<?> getGenericTypeParamFrom(Type genType) {
//		if (genType instanceof ParameterizedType pt) {
//			Type typeParam = pt.getActualTypeArguments()[0];
//			return typeToClass(typeParam);
//		}
//		else {
//			return Event.class;
//		}
//	}
//
	private static <X> List<Class<? super X>> getAllAssignableClasses(Class<X> initialCls) {
		List<Class<? super X>> out = new ArrayList<>();
		Class<? super X> clazz = initialCls;
		while (clazz != null && !clazz.equals(Object.class)) {
			out.add(clazz);
			clazz = clazz.getSuperclass();
		}
		return out;
	}
//
//	private static Class<?> typeToClass(Type type) {
//		if (type instanceof Class clazz) {
//			return clazz;
//		}
//		else if (type instanceof ParameterizedType pt) {
//			return typeToClass(pt.getRawType());
//		}
//		else {
//			throw new IllegalStateException("I don't know how to handle Type object " + type);
//		}
//	}

	@HandleEvents
	public void callChildEventHandlers(EventContext ctx, Event event) {
		checkInit();
		handlers.forEach(handler -> handler.run(ctx, event));
	}

}
