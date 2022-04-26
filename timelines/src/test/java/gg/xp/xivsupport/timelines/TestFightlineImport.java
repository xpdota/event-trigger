package gg.xp.xivsupport.timelines;

import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class TestFightlineImport {

	@Test
	void testImport() {
		String fileContents;
		try {
			String resourcePath = "/fightline_import_1.json";
			InputStream resource = TestFightlineImport.class.getResourceAsStream(resourcePath);
			if (resource == null) {
				throw new IllegalArgumentException("The resource '%s' does not exist".formatted(resourcePath));
			}
			fileContents = IOUtils.toString(resource, StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}


	}


}
