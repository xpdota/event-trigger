package gg.xp.sys;

import gg.xp.scan.AutoHandlerInstanceProvider;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PicoBasedInstanceProvider implements AutoHandlerInstanceProvider {
	private static final Logger log = LoggerFactory.getLogger(PicoBasedInstanceProvider.class);
	private final Object lock = new Object();
	private final MutablePicoContainer pico;

	public PicoBasedInstanceProvider(MutablePicoContainer pico) {
		this.pico = pico;
	}

	@Override
	public <X> X getInstance(Class<X> clazz) {
		X instance = pico.getComponent(clazz);
		if (instance == null) {
			synchronized (lock) {
				instance = pico.getComponent(clazz);
				if (instance == null) {
					log.debug("Adding component {}", clazz);
					pico.addComponent(clazz);
					instance = pico.getComponent(clazz);
				}
			}
		}
		return instance;
	}
}
