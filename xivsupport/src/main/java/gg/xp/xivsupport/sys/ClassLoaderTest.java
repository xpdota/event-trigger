package gg.xp.xivsupport.sys;

import gg.xp.reevent.scan.ForceReloadClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClassLoaderTest {

	private static final Logger log = LoggerFactory.getLogger(ClassLoaderTest.class);

	private ClassLoaderTest() {
	}

	public static void main(String[] args) {
		ForceReloadClassLoader cl1 = new ForceReloadClassLoader();
		ForceReloadClassLoader cl2 = new ForceReloadClassLoader();

		Class<?> c1 = cl1.findClass("gg.xp.xivsupport.events.actlines.events.ZeroLogLineEvent");
		Class<?> c2 = cl2.findClass("gg.xp.xivsupport.events.actlines.events.ZeroLogLineEvent");

		log.info("Stuff");
	}
}
