package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"ReplaceNullCheck", "AssignmentOrReturnOfFieldWithMutableType"})
public class EnumListSetting<X extends Enum<X>> {

	private static final Logger log = LoggerFactory.getLogger(EnumListSetting.class);

	private final Class<X> enumCls;
	private final PersistenceProvider persistence;
	private final String propertyKey;
	private final BadKeyBehavior bkb;
	private final List<X> dflt;

	/**
	 * Behavior for if a property key does not map to a valid enum member
	 */
	public enum BadKeyBehavior {
		/**
		 * Throw an IllegalArgumentException
		 */
		THROW,
		/**
		 * Omit the value but read the rest in order
		 */
		OMIT,
		/**
		 * Return the default value, i.e. pretend the property was never saved
		 */
		RETURN_DEFAULT
	}

	private boolean hasCachedValue;
	private List<X> cached;

	public EnumListSetting(Class<X> enumCls, PersistenceProvider persistence, String propertyKey, BadKeyBehavior bkb, List<X> dflt) {
		this.enumCls = enumCls;
		this.persistence = persistence;
		this.propertyKey = propertyKey;
		this.bkb = bkb;
		this.dflt = dflt;
	}

	public List<X> get() {
		if (hasCachedValue) {
			return cached;
		}
		else {
			List<X> computed = computeValue();
			hasCachedValue = true;
			if (computed == null) {
				return cached = null;
			}
			return cached = Collections.unmodifiableList(computed);
		}

	}

	private List<X> computeValue() {
		String valueFromPersistence = persistence.get(propertyKey, String.class, null);
		if (valueFromPersistence == null) {
			return dflt;
		}
		else {
			String[] stringItems = valueFromPersistence.split(",");
			List<X> out = new ArrayList<>(stringItems.length);
			for (String stringItem : stringItems) {
				try {
					X item = Enum.valueOf(enumCls, stringItem);
					out.add(item);
				}
				catch (IllegalArgumentException e) {
					log.error("Invalid key ({}) for property ({}) - no member of ({}) for that value", stringItem, propertyKey, enumCls.getSimpleName());
					switch (bkb) {
						case OMIT:
							continue;
						case THROW:
							throw e;
						case RETURN_DEFAULT:
							return dflt;
					}
				}
			}
			return out;
		}
	}

	public void delete() {
		cached = dflt;
		hasCachedValue = true;
		persistence.delete(propertyKey);

	}

	public void set(List<X> newValue) {
		String stringified = newValue.stream()
				.map(Enum::name)
				.collect(Collectors.joining(","));
		cached = List.copyOf(newValue);
		hasCachedValue = true;
		persistence.save(propertyKey, stringified);
	}

}
