package gg.xp.sys;

import gg.xp.scan.AutoHandlerInstanceProvider;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;

public class PicoBasedInstanceProvider implements AutoHandlerInstanceProvider {
	private final MutablePicoContainer pico;

	public PicoBasedInstanceProvider(MutablePicoContainer pico) {
		this.pico = pico;
	}

	@Override
	public <X> X getInstance(Class<X> clazz) {
		X instance = pico.getComponent(clazz);
		if (instance == null) {
			pico.addComponent(clazz);
			instance = pico.getComponent(clazz);
		}
		return instance;
	}
}
