package gg.xp.reevent.scan;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class ForceReloadClassLoader extends ClassLoader {

	private static final Logger log = LoggerFactory.getLogger(ForceReloadClassLoader.class);

	@Override
	public Class<?> loadClass(String s) {
		return findClass(s);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		log.warn("loadClass({}, {}) called - custom behavior may not apply", name, resolve);
		return super.loadClass(name, resolve);
	}

	@Override
	protected Class<?> findClass(String moduleName, String name) {
		log.warn("findClass({}, {}) called - custom behavior may not apply", moduleName, name);
		return super.findClass(moduleName, name);
	}

	@Override
	public Class<?> findClass(String s) {
		log.info("findClass({})", s);
		try {
			byte[] bytes = loadClassData(s);
			return defineClass(s, bytes, 0, bytes.length);
		}
		catch (Throwable ioe) {
			try {
				return super.loadClass(s);
			}
			catch (ClassNotFoundException ignore) {
			}
			log.error("Error in class loader", ioe);
			return null;
		}
	}

	private static byte[] loadClassData(String className) throws IOException {
		log.info("loadClassData({})", className);
		String actualFileName = "/" + className.replaceAll("\\.", "/") + ".class";
		URL resourceUrl = ForceReloadClassLoader.class.getResource(actualFileName);
		File f;
		try {
			f = new File(resourceUrl.toURI());
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		int size = (int) f.length();
		byte[] buff = new byte[size];
		FileInputStream fis = new FileInputStream(f);
		DataInputStream dis = new DataInputStream(fis);
		dis.readFully(buff);
		dis.close();
		return buff;
	}
}
