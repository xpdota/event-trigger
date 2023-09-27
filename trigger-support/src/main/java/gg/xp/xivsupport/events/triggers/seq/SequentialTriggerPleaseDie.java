package gg.xp.xivsupport.events.triggers.seq;

public class SequentialTriggerPleaseDie extends RuntimeException {
	public SequentialTriggerPleaseDie() {
	}

	public SequentialTriggerPleaseDie(String message) {
		super(message);
	}

	public SequentialTriggerPleaseDie(String message, Throwable cause) {
		super(message, cause);
	}

	public SequentialTriggerPleaseDie(Throwable cause) {
		super(cause);
	}

	public SequentialTriggerPleaseDie(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
