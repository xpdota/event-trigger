package gg.xp.xivsupport.gui.tabs;

public class AddonValidationException extends RuntimeException {
	public AddonValidationException() {
	}

	public AddonValidationException(String message) {
		super(message);
	}

	public AddonValidationException(String message, Throwable cause) {
		super(message, cause);
	}

	public AddonValidationException(Throwable cause) {
		super(cause);
	}

	public AddonValidationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
