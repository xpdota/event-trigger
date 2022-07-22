package gg.xp.xivsupport.gui.overlay;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.StandardCopyOption;

// from https://www.adamh.cz/blog/2012/12/how-to-load-native-jni-library-from-jar/
public class LibUtil {
	private static final int MIN_PREFIX_LENGTH = 3;
	private static final String NATIVE_FOLDER_PATH_PREFIX = "nativeutils";
	private static File temporaryDir;

	public static void loadLibraryFromJar(String path) {

		try {
			if (null == path || !path.startsWith("/")) {
				throw new IllegalArgumentException("The path has to be absolute (start with '/').");
			}

			// Obtain filename from path
			String[] parts = path.split("/");
			String filename = (parts.length > 1) ? parts[parts.length - 1] : null;

			// Check if the filename is okay
			if (filename == null || filename.length() < MIN_PREFIX_LENGTH) {
				throw new IllegalArgumentException("The filename has to be at least 3 characters long.");
			}

			// Prepare temporary file
			if (temporaryDir == null) {
				temporaryDir = createTempDirectory(NATIVE_FOLDER_PATH_PREFIX);
				temporaryDir.deleteOnExit();
			}

			File temp = new File(temporaryDir, filename);

			try (InputStream is = ScalableJFrameLinuxRealImpl.class.getResourceAsStream(path)) {
				Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			catch (IOException e) {
				temp.delete();
				throw e;
			}
			catch (NullPointerException e) {
				temp.delete();
				throw new FileNotFoundException("File " + path + " was not found inside JAR.");
			}

			try {
				System.load(temp.getAbsolutePath());
			}
			finally {
				if (isPosixCompliant()) {
					// Assume POSIX compliant file system, can be deleted after loading
					temp.delete();
				}
				else {
					// Assume non-POSIX, and don't delete until last file descriptor closed
					temp.deleteOnExit();
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean isPosixCompliant() {
		try {
			return FileSystems.getDefault()
					.supportedFileAttributeViews()
					.contains("posix");
		}
		catch (FileSystemNotFoundException
				| ProviderNotFoundException
				| SecurityException e) {
			return false;
		}
	}

	private static File createTempDirectory(String prefix) throws IOException {
		String tempDir = System.getProperty("java.io.tmpdir");
		File generatedDir = new File(tempDir, prefix + System.nanoTime());

		if (!generatedDir.mkdir())
			throw new IOException("Failed to create temp directory " + generatedDir.getName());

		return generatedDir;
	}
}
