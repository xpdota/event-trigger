package gg.xp.xivsupport.persistence;

import org.codehaus.groovy.runtime.callsite.PerInstancePojoMetaClassSite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;

public final class EarlyPropsProvider {
	private EarlyPropsProvider() {
	}

	private static final Logger log = LoggerFactory.getLogger(EarlyPropsProvider.class);

	private static volatile PersistenceProvider pers;
	private static final Object lock = new Object();

	public static PersistenceProvider getProvider() {
		if (pers == null) {
			synchronized (lock) {
				if (pers == null) {
					return pers = create();
				}
			}
		}
		return pers;
	}

	private static PersistenceProvider create() {
		try {
			return new PropertiesFilePersistenceProvider(Platform.getInstallDir().toPath().resolve("init.properties").toFile());
		}
		catch (Throwable t) {
			log.error("Error creating early properties provider", t);
			return new InMemoryMapPersistenceProvider();
		}
	}
}
