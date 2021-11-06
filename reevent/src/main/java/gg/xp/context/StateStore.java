package gg.xp.context;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StateStore {

	private final Map<Class<?>, Object> map = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	public <X> X get(Class<X> clazz) {
		return (X) map.computeIfAbsent(clazz, (cls) -> {
			try {
				return cls.getConstructor().newInstance();
			}
			catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
				throw new RuntimeException("Error instantiation state class " + cls, e);
			}
		});
	}
}
