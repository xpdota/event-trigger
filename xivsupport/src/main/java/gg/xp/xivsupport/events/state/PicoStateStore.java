package gg.xp.xivsupport.events.state;

import gg.xp.reevent.context.StateStore;
import gg.xp.reevent.context.SubState;
import org.picocontainer.MutablePicoContainer;

public class PicoStateStore implements StateStore {

	private final MutablePicoContainer pico;

	public PicoStateStore(MutablePicoContainer pico) {
		this.pico = pico;
	}

	@Override
	public <X> X get(Class<X> clazz) {
		return pico.getComponent(clazz);
	}

	@Override
	public <X> void putCustom(Class<X> clazz, X instance) {
		pico.addComponent(instance);
	}
}
