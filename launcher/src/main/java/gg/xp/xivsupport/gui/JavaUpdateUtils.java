package gg.xp.xivsupport.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JavaUpdateUtils {

	private final Consumer<String> logging;

	public static void main(String[] args) throws IOException {
		// simple test
		new JavaUpdateUtils(System.out::println)
				.installVersion(21, new File("launcher/target/jdk21"));
	}

	public JavaUpdateUtils(Consumer<String> logging) {
		this.logging = logging;
	}

	private void log(String text) {
		logging.accept(text);
	}

	public void installVersion(int version, File realDestDir) throws IOException {
		File destDir = realDestDir.toPath().getParent().resolve(realDestDir.getName() + "_tmp").toFile();
		log("Downloading to " + destDir);
		// adapted from https://www.baeldung.com/java-compress-and-uncompress
		String os = Update.isWindows() ? "windows" : "linux";
		// I don't think the game runs on ARM...
		String arch = "x64";
		String url = "https://download.oracle.com/java/%s/latest/jdk-%s_%s-%s_bin.zip".formatted(version, version, os, arch);
		log("Downloading " + url);
		try (InputStream stream = new URL(url).openStream()) {

			ZipInputStream zis = new ZipInputStream(stream);

			byte[] buffer = new byte[1024];
			ZipEntry rootEntry = zis.getNextEntry();
			ZipEntry zipEntry;
			while ((zipEntry = zis.getNextEntry()) != null) {
				File newFile = newFile(destDir, zipEntry);
				if ("src.zip".equalsIgnoreCase(newFile.getName())) {
					// We don't need to install java source
					continue;
				}
				if (zipEntry.isDirectory()) {
					log("Directory: " + newFile);
					if (!newFile.isDirectory() && !newFile.mkdirs()) {
						throw new IOException("Failed to create directory " + newFile);
					}
				}
				else {
					log("File: " + newFile);
					// fix for Windows-created archives
					File parent = newFile.getParentFile();
					if (!parent.isDirectory() && !parent.mkdirs()) {
						throw new IOException("Failed to create directory " + parent);
					}

					// write file content
					FileOutputStream fos = new FileOutputStream(newFile);
					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
					fos.close();
				}
			}

			zis.closeEntry();
			zis.close();
		}
		log("Moving into place");
		destDir.renameTo(realDestDir);
		log("JDK " + version + " successfully downloaded and extracted");
	}

	private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
		// Kind of messy but should be fine for what it needs to do
		// The zip comes with a root directory, but we don't want that
		File destFile = new File(destinationDir, zipEntry.getName().split("/", 2)[1]);
		if (destFile.toPath().startsWith(destinationDir.toPath())) {
			return destFile;
		}
		else {
			throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
		}
	}
}
