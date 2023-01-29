package gg.xp.telestosupport;

import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;

public class TelestoConnectionError extends BaseTelestoResponse implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = 5675470435257672749L;
	private final Throwable error;

	public TelestoConnectionError(Throwable error) {
		this.error = error;
	}

	public Throwable getError() {
		return error;
	}

	@Override
	public String getPrimaryValue() {
		return error.toString();
	}

}
