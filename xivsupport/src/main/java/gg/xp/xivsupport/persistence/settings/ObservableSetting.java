package gg.xp.xivsupport.persistence.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ObservableSetting {
	private final Logger log = LoggerFactory.getLogger(ObservableSetting.class);

	private final List<Runnable> callbacks = new ArrayList<>();

	public void addListener(Runnable listener) {
		callbacks.add(listener);
	}

	public void removeListener(Runnable listener) {
		callbacks.remove(listener);
	}

	protected void notifyListeners() {
		callbacks.forEach(runnable -> {
			try {
				runnable.run();
			}
			catch (Throwable t) {
				log.error("Error notifying listener", t);
			}
		});
	}

}
