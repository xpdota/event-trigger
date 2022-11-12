package gg.xp.reevent.scan;

import java.net.URL;
import java.util.Collections;
import java.util.List;

public class AutoHandlerConfig {
	private boolean isNotLive;
	private List<URL> addonJars = Collections.emptyList();

	public boolean isNotLive() {
		return isNotLive;
	}

	public void setNotLive(boolean notLive) {
		isNotLive = notLive;
	}

	public List<URL> getAddonJars() {
		return addonJars;
	}

	public void setAddonJars(List<URL> addonJars) {
		this.addonJars = addonJars;
	}
}
