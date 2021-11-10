package gg.xp.util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Utils {
	private Utils() {
	}

	public static Map<Field, Object> dumpAllFields(Object object) {
		Map<Field, Object> fieldValues = new LinkedHashMap<>();
		Class<?> currentClass = object.getClass();
		while (currentClass != null) {
			Arrays.stream(currentClass.getDeclaredFields())
					.sorted(Comparator.comparing(Field::getName))
					.forEach(field -> {
						field.setAccessible(true);
						Object value;
						try {
							value = field.get(object);
						}
						catch (IllegalAccessException e) {
							throw new RuntimeException(String.format("Error dumping field %s on object %s", field, object), e);
						}
						fieldValues.put(field, value);
					});
			currentClass = currentClass.getSuperclass();
		}
		return fieldValues;
	}
}
