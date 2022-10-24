package gg.xp.reevent.scan;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class FeedHandlerChildInfo<X extends Annotation, Z> {
	private final Class<?> parentCls;
	private final Object parentInst;
	private final Field handlerField;
	private final Z handlerFieldValue;
	private final X annotation;

	public FeedHandlerChildInfo(Class<?> parentCls, Object parentInst, Field handlerField, Z handlerFieldValue, X annotation) {
		this.parentCls = parentCls;
		this.parentInst = parentInst;
		this.handlerField = handlerField;
		this.handlerFieldValue = handlerFieldValue;
		this.annotation = annotation;
	}

	public Class<?> getParentCls() {
		return parentCls;
	}

	public Object getParentInst() {
		return parentInst;
	}

	public Field getHandlerField() {
		return handlerField;
	}

	public Z getHandlerFieldValue() {
		return handlerFieldValue;
	}

	public X getAnnotation() {
		return annotation;
	}
}
