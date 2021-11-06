package logread;

import gg.xp.logread.DirTailer;
import gg.xp.logread.LogTailer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DirTailerTest {
	@Test
	void testLogTailDir() throws IOException, InterruptedException {
		List<String> lines = new ArrayList<>();
		File testFolder = new File("target/ignore_me_log_dir2");
		File testFile = new File(testFolder, "foo.log");
		testFolder.mkdirs();
		if (testFile.exists()) {
			if (!testFile.delete()) {
				throw new AssertionError("Couldn't delete test file: " + testFile);
			}
		}
		BufferedWriter writer = new BufferedWriter(new FileWriter(testFile, true));
		DirTailer dirTailer = new DirTailer(testFolder, lines::add);
		dirTailer.start();
		writer.write("This gets ignored since we only start watching the file after seeing a modification\n");
		writer.close();
		Thread.sleep(1000);
//		writer.write(String.format("Random number: %s\n", ThreadLocalRandom.current().nextInt()));
		writer = new BufferedWriter(new FileWriter(testFile, true));
		writer.write("Foo");
		writer.flush();
		writer.write("Bar\n");
		writer.write("Baz\n");
		writer.flush();
		writer.write("Last Line");
		writer.flush();
		// TODO
		Thread.sleep(1000);

		// This test doesn't work well for some reason, but I confirmed manually that it works
		Assert.assertEquals(lines, List.of("FooBar", "Baz", "Last Line"));


	}
}
