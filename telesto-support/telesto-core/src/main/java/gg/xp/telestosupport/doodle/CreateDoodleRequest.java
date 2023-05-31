package gg.xp.telestosupport.doodle;

import gg.xp.reevent.events.BaseEvent;

import java.io.Serial;

public class CreateDoodleRequest extends BaseEvent {
	@Serial
	private static final long serialVersionUID = -3233486410139001298L;
	private final DoodleSpec spec;

	public CreateDoodleRequest(DoodleSpec spec) {
		this.spec = spec;
	}

	public DoodleSpec getSpec() {
		return spec;
	}

}
