package gg.xp.reevent.context;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BasicStateStore implements StateStore {

	private final Map<Class<?>, Object> map = new ConcurrentHashMap<>();

	@Override
	@SuppressWarnings("unchecked")
	public <X> X get(Class<X> clazz) {
		return (X) map.computeIfAbsent(clazz, (cls) -> {
			try {
				return cls.getConstructor().newInstance();
			}
			catch (NoSuchMethodException nsme) {
				throw new RuntimeException("Error instantiating state class because it does not have the correct constructor. A custom instance may need to be installed.", nsme);

			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException("Error instantiating state class " + cls, e);
			}
		});
	}

	@Override
	public <X> void putCustom(Class<X> clazz, X instance) {
		map.put(clazz, instance);
	}
}
