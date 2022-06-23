package gg.xp.xivsupport.events.actlines.events.vfx;

import java.io.Serial;
import java.io.Serializable;

public final class StatusLoopVfx implements Serializable {
	@Serial
	private static final long serialVersionUID = 8794691687567445402L;
	private final long id;

	private StatusLoopVfx(long id) {
		this.id = id;
	}

	public static StatusLoopVfx of(long id) {
		return new StatusLoopVfx(id);
	}

	public long getId() {
		return id;
	}

	@Override
	public String toString() {
		return String.format("%s (0x%X)", id, id);
	}
}
