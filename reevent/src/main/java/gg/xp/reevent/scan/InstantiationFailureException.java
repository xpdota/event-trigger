package gg.xp.reevent.scan;

import java.io.Serial;

public final class InstantiationFailureException extends InitException {
	@Serial
	private static final long serialVersionUID = 2373115199586956408L;
	private final Class<?> failedClass;

	public InstantiationFailureException(Class<?> failedClass, Throwable cause) {
		super("Class %s failed to initialize: %s".formatted(failedClass.getName(), cause.getMessage()), cause);
		this.failedClass = failedClass;
	}

	public Class<?> getFailedClass() {
		return failedClass;
	}

	@Override
	public String describeFailedComponent() {
		return failedClass.getSimpleName();
	}
}
