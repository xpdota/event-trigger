package gg.xp.xivsupport.events.triggers.seq;

public class SequentialTriggerTimeoutException extends RuntimeException {
	public SequentialTriggerTimeoutException() {
	}

	public SequentialTriggerTimeoutException(String message) {
		super(message);
	}

	public SequentialTriggerTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}

	public SequentialTriggerTimeoutException(Throwable cause) {
		super(cause);
	}

	public SequentialTriggerTimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
