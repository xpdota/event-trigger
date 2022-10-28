package gg.xp.xivsupport.events.triggers.easytriggers.gui;

public class TriggerCreationCancelledException extends RuntimeException {
	public TriggerCreationCancelledException() {
	}

	public TriggerCreationCancelledException(String message) {
		super(message);
	}

	public TriggerCreationCancelledException(String message, Throwable cause) {
		super(message, cause);
	}

	public TriggerCreationCancelledException(Throwable cause) {
		super(cause);
	}

	public TriggerCreationCancelledException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
