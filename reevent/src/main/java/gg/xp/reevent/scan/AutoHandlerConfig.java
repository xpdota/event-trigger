package gg.xp.reevent.scan;

import java.net.URL;
import java.util.Collections;
import java.util.List;

public class AutoHandlerConfig {
	private boolean strict;
	private boolean isNotLive;
	private volatile boolean scanned;
	private List<URL> addonJars = Collections.emptyList();

	public boolean isNotLive() {
		return isNotLive;
	}

	public void setNotLive(boolean notLive) {
		assertNotScanned();
		isNotLive = notLive;
	}

	public List<URL> getAddonJars() {
		return addonJars;
	}

	public void setAddonJars(List<URL> addonJars) {
		assertNotScanned();
		this.addonJars = addonJars;
	}

	public void setStrict(boolean strict) {
		assertNotScanned();
		this.strict = strict;
	}

	void assertNotScanned() {
		if (scanned) {
			throw new IllegalStateException("Already scanned - changing configuration now is useless");
		}

	}

	public boolean isStrict() {
		return strict;
	}

	void setScanned() {
		this.scanned = true;
	}
}
