package gg.xp.xivsupport.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JavaUpdateUtils {

	public static void installVersion(int version, File destDir) throws IOException {
		// adapted from https://www.baeldung.com/java-compress-and-uncompress
		String os = Update.isWindows() ? "windows" : "linux";
		// I don't think the game runs on ARM...
		String arch = "x64";
		String url = "https://download.oracle.com/java/%s/latest/jdk-%s_%s-%s_bin.zip".formatted(version, version, os, arch);
		InputStream stream = new URL(url).openStream();
		ZipInputStream zis = new ZipInputStream(stream);

		byte[] buffer = new byte[1024];
		ZipEntry zipEntry = zis.getNextEntry();
		while (zipEntry != null) {
			File newFile = newFile(destDir, zipEntry);
			if (zipEntry.isDirectory()) {
				if (!newFile.isDirectory() && !newFile.mkdirs()) {
					throw new IOException("Failed to create directory " + newFile);
				}
			}
			else {
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
			zipEntry = zis.getNextEntry();
		}

		zis.closeEntry();
		zis.close();
	}

	private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
		File destFile = new File(destinationDir, zipEntry.getName());

		if (destFile.toPath().startsWith(destinationDir.toPath())) {
			return destFile;
		}
		else {
			throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
		}
	}
}
