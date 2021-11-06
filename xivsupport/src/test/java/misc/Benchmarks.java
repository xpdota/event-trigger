package misc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Ignore
public class Benchmarks {

	private static final Logger log = LoggerFactory.getLogger(Benchmarks.class);

	private static final String logline = "38|2021-05-10T22:31:52.3330000-07:00|10669D22|Wynn Dohz|0050501C|122634|122634|10000|10000|27|0|80.35929|4.77615|17.09997|0.869517|3A00|AA|0|0A016D|41F00000|E0000000|0A0168|41F00000|E0000000||c243843d37798b12a00ec8cef4fd4d43";

	private static void timeIt(String label, Runnable run) {
		long before = System.currentTimeMillis();
		run.run();
		long after = System.currentTimeMillis();
		long delta = after - before;
		log.info("Timing for {}: {}", label, delta);
	}

	@Test
	void testBasicMatch() {
		int count = 10_000_000;
		// Just in case a field access is slower than a local var, copy it here
		String line = logline;
		Pattern m38 = Pattern.compile("^38\\|.*");
		Pattern m21 = Pattern.compile("^21\\|.*");
		Pattern f38 = Pattern.compile("^38\\|");
		Pattern f21 = Pattern.compile("^21\\|");
		String s38 = "38|";
		String s21 = "21|";

		// Verify patterns
		Assert.assertTrue(m38.matcher(line).matches());
		Assert.assertFalse(m21.matcher(line).matches());
		Assert.assertTrue(f38.matcher(line).find());
		Assert.assertFalse(f21.matcher(line).find());
		Assert.assertTrue(line.startsWith(s38));
		Assert.assertFalse(line.startsWith(s21));

		// using matches()
		timeIt("matching pattern using matches", () -> {
			for (int i = 0; i < count; i++) {
				m38.matcher(line).matches();
			}
		});
		timeIt("non-matching pattern using matches", () -> {
			for (int i = 0; i < count; i++) {
				m21.matcher(line).matches();
			}
		});

		// using find()
		timeIt("matching pattern using find", () -> {
			for (int i = 0; i < count; i++) {
				f38.matcher(line).find();
			}
		});
		timeIt("non-matching pattern using find", () -> {
			for (int i = 0; i < count; i++) {
				f21.matcher(line).find();
			}
		});

		// using startsWith()
		// using find()
		timeIt("matching pattern using startsWith", () -> {
			for (int i = 0; i < count; i++) {
				line.startsWith(s38);
			}
		});
		timeIt("non-matching pattern using startsWith", () -> {
			for (int i = 0; i < count; i++) {
				line.startsWith(s21);
			}
		});
	}

	@Test
	void testCapturing() {
		int count = 1_000_000;
		// Just in case a field access is slower than a local var, copy it here
		String line = logline;
		Pattern m38 = Pattern.compile("^38\\|[^|]*\\|([^|]*)\\|.*");
		Pattern m21 = Pattern.compile("^21\\|[^|]*\\|([^|]*)\\|.*");
		Pattern f38 = Pattern.compile("^38\\|[^|]*\\|([^|]*)");
		Pattern f21 = Pattern.compile("^21\\|[^|]*\\|([^|]*)");
		String s38 = "38|";
		String s21 = "21|";

		// Verify patterns
		Assert.assertTrue(m38.matcher(line).matches());
		Assert.assertFalse(m21.matcher(line).matches());
		Assert.assertTrue(f38.matcher(line).find());
		Assert.assertFalse(f21.matcher(line).find());
		Assert.assertTrue(line.startsWith(s38));
		Assert.assertFalse(line.startsWith(s21));

		// using matches()
		timeIt("matching pattern using matches", () -> {
			for (int i = 0; i < count; i++) {
				Matcher matcher = m38.matcher(line);
				matcher.matches();
				matcher.group(1);
			}
		});
		timeIt("non-matching pattern using matches", () -> {
			for (int i = 0; i < count; i++) {
				Matcher matcher = m21.matcher(line);
				matcher.matches();
			}
		});

		// using find()
		timeIt("matching pattern using find", () -> {
			for (int i = 0; i < count; i++) {
				Matcher matcher = f38.matcher(line);
				matcher.find();
				matcher.group(1);
			}
		});
		timeIt("non-matching pattern using find", () -> {
			for (int i = 0; i < count; i++) {
				Matcher matcher = f21.matcher(line);
				matcher.find();
			}
		});

		// using startsWith()
		timeIt("matching pattern using startsWith", () -> {
			for (int i = 0; i < count; i++) {
				line.startsWith(s38);
				line.split("\\|", 4);
			}
		});
		timeIt("matching pattern using startsWith (increased limit)", () -> {
			for (int i = 0; i < count; i++) {
				line.startsWith(s38);
				line.split("\\|", 12);
			}
		});
		timeIt("matching pattern using startsWith (no limit)", () -> {
			for (int i = 0; i < count; i++) {
				line.startsWith(s38);
				line.split("\\|");
			}
		});
		timeIt("non-matching pattern using startsWith", () -> {
			for (int i = 0; i < count; i++) {
				line.startsWith(s21);
			}
		});

	}

	@Test
	void testConditions() {
		// TODO
		int count = 1_000_000;
		// Just in case a field access is slower than a local var, copy it here
		String line = logline;
		Pattern m38 = Pattern.compile("^38\\|[^|]*\\|([^|]*)\\|.*");
		Pattern m21 = Pattern.compile("^21\\|[^|]*\\|([^|]*)\\|.*");
		Pattern f38 = Pattern.compile("^38\\|[^|]*\\|([^|]*)");
		Pattern f21 = Pattern.compile("^21\\|[^|]*\\|([^|]*)");
		String s38 = "38|";
		String s21 = "21|";

		// Verify patterns
		Assert.assertTrue(m38.matcher(line).matches());
		Assert.assertFalse(m21.matcher(line).matches());
		Assert.assertTrue(f38.matcher(line).find());
		Assert.assertFalse(f21.matcher(line).find());
		Assert.assertTrue(line.startsWith(s38));
		Assert.assertFalse(line.startsWith(s21));

		// using matches()
		timeIt("matching pattern using matches", () -> {
			for (int i = 0; i < count; i++) {
				Matcher matcher = m38.matcher(line);
				matcher.matches();
				matcher.group(1);
			}
		});
		timeIt("non-matching pattern using matches", () -> {
			for (int i = 0; i < count; i++) {
				Matcher matcher = m21.matcher(line);
				matcher.matches();
			}
		});

		// using find()
		timeIt("matching pattern using find", () -> {
			for (int i = 0; i < count; i++) {
				Matcher matcher = f38.matcher(line);
				matcher.find();
				matcher.group(1);
			}
		});
		timeIt("non-matching pattern using find", () -> {
			for (int i = 0; i < count; i++) {
				Matcher matcher = f21.matcher(line);
				matcher.find();
			}
		});

		// using startsWith()
		timeIt("matching pattern using startsWith", () -> {
			for (int i = 0; i < count; i++) {
				line.startsWith(s38);
				line.split("\\|", 4);
			}
		});
		timeIt("matching pattern using startsWith (increased limit)", () -> {
			for (int i = 0; i < count; i++) {
				line.startsWith(s38);
				line.split("\\|", 12);
			}
		});
		timeIt("matching pattern using startsWith (no limit)", () -> {
			for (int i = 0; i < count; i++) {
				line.startsWith(s38);
				line.split("\\|");
			}
		});
		timeIt("non-matching pattern using startsWith", () -> {
			for (int i = 0; i < count; i++) {
				line.startsWith(s21);
			}
		});

	}

}
