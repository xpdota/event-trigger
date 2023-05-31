package gg.xp.xivsupport.groovy.helpers;

import groovy.lang.Closure;
import groovy.lang.GroovyRuntimeException;
import org.codehaus.groovy.runtime.GStringImpl;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.util.function.Function;

/**
 * GString that uses a user-specified toString implementation for objects.
 */
public abstract class CustomGString extends GStringImpl {

	protected CustomGString(Object[] values, String[] strings) {
		super(values, strings);
	}

	/**
	 * Create a new custom conversion GString
	 *
	 * @param values  the value parts
	 * @param strings the string parts
	 * @param converter the custom object-to-string conversion
	 */
	public static CustomGString of(Object[] values, String[] strings, Function<@Nullable Object, String> converter) {
		return new CustomGString(values, strings) {
			@Override
			public String convert(Object obj) {
				return converter.apply(obj);
			}
		};
	}

	protected abstract String convert(Object obj);

	@Override
	public Writer writeTo(Writer out) throws IOException {
		String[] s = getStrings();
		Object[] values = getValues();
		int numberOfValues = values.length;
		for (int i = 0, size = s.length; i < size; i++) {
			out.write(s[i]);
			if (i < numberOfValues) {
				final Object value = values[i];

				if (value instanceof Closure) {
					final Closure c = (Closure) value;
					int maximumNumberOfParameters = c.getMaximumNumberOfParameters();

					if (maximumNumberOfParameters == 0) {
						out.write(convert(c.call()));
					} else if (maximumNumberOfParameters == 1) {
						c.call(out);
					} else {
						throw new GroovyRuntimeException("Trying to evaluate a GString containing a Closure taking "
						                                 + maximumNumberOfParameters + " parameters");
					}
				} else {
					out.write(convert(value));
				}
			}
		}
		return out;
	}
}
