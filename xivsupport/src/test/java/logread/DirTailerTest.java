package logread;

import gg.xp.logread.DirTailer;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DirTailerTest {
	// TODO: this test is flaky. I think there's some strange file flushing behavior making it unreliable,
	// but this doesn't seem to be a problem in actual usage.
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
		writer.write("This gets ignored since we only start watching the file after seeing a modification\n");
		writer.close();
		dirTailer.start();
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
		MatcherAssert.assertThat(lines, Matchers.contains("FooBar", "Baz", "Last Line"));


	}
}
