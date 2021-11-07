package gg.xp.sys;

import gg.xp.scan.ForceReloadClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassLoaderTest {

	private static final Logger log = LoggerFactory.getLogger(ClassLoaderTest.class);

	public static void main(String[] args) {
		ForceReloadClassLoader cl1 = new ForceReloadClassLoader();
		ForceReloadClassLoader cl2 = new ForceReloadClassLoader();

		Class<?> c1 = cl1.findClass("gg.xp.events.actlines.ZeroLogLineEvent");
		Class<?> c2 = cl2.findClass("gg.xp.events.actlines.ZeroLogLineEvent");

		log.info("Stuff");
	}
}
