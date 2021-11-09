package gg.xp.events.state;

import gg.xp.context.StateStore;
import gg.xp.context.SubState;
import org.picocontainer.MutablePicoContainer;

public class PicoStateStore implements StateStore {

	private final MutablePicoContainer pico;

	public PicoStateStore(MutablePicoContainer pico) {
		this.pico = pico;
	}

	@Override
	public <X extends SubState> X get(Class<X> clazz) {
		return pico.getComponent(clazz);
	}

	@Override
	public <X> void putCustom(Class<X> clazz, X instance) {
		pico.addComponent(instance);
	}
}
