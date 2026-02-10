package gg.xp.xivsupport.events.triggers.easytriggers;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.bean.BeanDeserializer;

public class FailCaptureDeserializer<T, F extends T> extends BeanDeserializer {

	private final BeanDeserializer src;
	private final BeanDescription.Supplier beanDescRef;
	private final Class<T> parentType;
	private final Class<F> failType;
	private final FailProducer<F> failProducer;
	// for debugging
	private BeanProperty prop;
	private boolean noFail;
	private boolean forceFail;

	public FailCaptureDeserializer(BeanDeserializer bd,
	                               BeanDescription.Supplier beanDescRef,
	                               Class<T> parentType,
	                               Class<F> failType,
	                               FailProducer<F> failProducer) {
		super(bd);
		this.src = bd;
		this.beanDescRef = beanDescRef;
		this.parentType = parentType;
		this.failType = failType;
		this.failProducer = failProducer;
	}

	private FailCaptureDeserializer<T, F> withDeser(BeanDeserializer bd) {
		return new FailCaptureDeserializer<>(bd, beanDescRef, parentType, failType, failProducer);
	}

	@Override
	public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
		var ctxed = super.createContextual(ctxt, property);
		var out = withDeser(ctxed instanceof BeanDeserializer bd ? bd : src);
		out.noFail = property != null
		             && !property.getType().getRawClass().isAssignableFrom(failType)
		             && property.getType().getRawClass().isAssignableFrom(parentType);
		out.forceFail = beanDescRef.getBeanClass() == failType;
		out.prop = property;
		return out;
	}

	@Override
	public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
		JsonNode root = p.objectReadContext().readTree(p);
		// In the case of an unknown type ID, this will be called with "Failed" as the bean class.
		if (forceFail) {
			return failProducer.makeFail(root, null);
		}
		try (JsonParser asTokens = ctxt.treeAsTokens(root)) {
			try {
				// Expects to be on the token
				asTokens.nextToken();
				return super.deserialize(asTokens, ctxt);
			}
			catch (JacksonException e) {
				if (noFail) {
					throw e;
				}
				return failProducer.makeFail(root, e);
			}
		}
	}


}
