package gg.xp.xivsupport.events.triggers.easytriggers;

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.bean.BeanDeserializer;

public class FailCaptureDeserializerModifier<T, F extends T> extends ValueDeserializerModifier {

	private final Class<T> parentType;
	private final Class<F> failType;
	private final FailProducer<F> failProducer;

	public FailCaptureDeserializerModifier(Class<T> parentType, Class<F> failType, FailProducer<F> failProducer) {
		this.parentType = parentType;
		this.failType = failType;
		this.failProducer = failProducer;
	}

	@Override
	public ValueDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription.Supplier beanDescRef, ValueDeserializer<?> deserializer) {
		Class<?> bc = beanDescRef.getBeanClass();
		// Ignore wholly unrelated types.
		if (!parentType.isAssignableFrom(bc)) {
			// TODO replace with 'return deserializer'
			return super.modifyDeserializer(config, beanDescRef, deserializer);
		}
		// This gets called twice when deserializing to the ParentType - once with ParentType as the bean desc, once with the concrete resolved type.
		if (deserializer instanceof BeanDeserializer bd) {
			return new FailCaptureDeserializer<T, F>(bd, beanDescRef, parentType, failType, failProducer);
		}
		return super.modifyDeserializer(config, beanDescRef, deserializer);
	}
}
