package gg.xp.xivsupport.sys;

import gg.xp.reevent.scan.AutoHandlerInstanceProvider;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoCompositionException;
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

	@Override
	public void preAdd(Class<?> clazz) {
		// TODO: find a better way of doing this
		try {
			pico.addComponent(clazz);
		} catch (PicoCompositionException e) {
			if (!e.getMessage().contains("Duplicate")) {
				throw e;
			}
		}
	}
}
