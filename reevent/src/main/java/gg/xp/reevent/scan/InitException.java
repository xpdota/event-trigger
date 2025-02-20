package gg.xp.reevent.scan;

public abstract sealed class InitException extends Throwable permits InstantiationFailureException, JarLoadException{
	protected InitException() {
	}

	protected InitException(String message) {
		super(message);
	}

	protected InitException(String message, Throwable cause) {
		super(message, cause);
	}

	protected InitException(Throwable cause) {
		super(cause);
	}

	protected InitException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public abstract String describeFailedComponent();
}
