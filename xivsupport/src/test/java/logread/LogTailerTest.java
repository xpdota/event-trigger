package logread;

import gg.xp.logread.LogTailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LogTailerTest {

	private static final Logger log = LoggerFactory.getLogger(LogTailerTest.class);

	@Test
	void testLogTailSimple() throws IOException, InterruptedException {
		List<String> lines = new ArrayList<>();
		File testFolder = new File("target/ignore_me_log_dir");
		File testFile = new File(testFolder, "foo.log");
		testFolder.mkdirs();
		testFile.delete();
		BufferedWriter writer = new BufferedWriter(new FileWriter(testFile));

		writer.write("This line should not appear in the output because it is written prior to watching the file\n");
		writer.flush();
		LogTailer logTailer = new LogTailer(testFile, lines::add);
		logTailer.startCurrentPos();
		// TODO
		Thread.sleep(1000);
		writer.flush();
		writer.write("Foo");
		writer.flush();
		writer.write("Bar\n");
		writer.write("Baz\n");
		writer.flush();
		writer.write("Last Line");
		writer.flush();
		// TODO
		Thread.sleep(2000);

		log.info("Lines: {}", lines);
		Assert.assertEquals(lines, List.of("FooBar", "Baz", "Last Line"));


	}
}
