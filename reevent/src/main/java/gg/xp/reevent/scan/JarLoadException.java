package gg.xp.reevent.scan;

import java.io.Serial;
import java.net.URL;
import java.util.Locale;

import static gg.xp.reevent.scan.AutoScan.getJarName;

public final class JarLoadException extends InitException {
	@Serial
	private static final long serialVersionUID = 4795475304003191429L;
	private final URL jarUrl;

	public JarLoadException(URL jarUrl, Throwable cause) {
		super("JAR '%s' failed to load: %s".formatted(jarUrl, cause.getMessage()));
		this.jarUrl = jarUrl;
	}

	@Override
	public String describeFailedComponent() {
		String jarName = getJarName(jarUrl.toString());
		if (jarName != null) {
			return jarName;
		}
		String file = jarUrl.getFile();
		if (file.toLowerCase(Locale.ROOT).endsWith(".jar")) {
			return file;
		}
		return jarUrl.toString();
	}
}
