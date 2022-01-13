package gg.xp.xivsupport.persistence;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SingleFlatFileProvider {
	private static final Logger log = LoggerFactory.getLogger(SingleFlatFileProvider.class);
	private final File file;

	public SingleFlatFileProvider(File file) {
		this.file = file;
	}

	public boolean exists() {
		return file.exists();
	}

	public String read() {
		try {
			return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void write(String content) {
		try {
			FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
