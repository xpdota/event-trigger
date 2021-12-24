package gg.xp.xivsupport.gui;

import gg.xp.xivsupport.gui.util.CatchFatalError;
import gg.xp.xivsupport.gui.util.CatchFatalErrorInUpdater;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// This one will NOT be launched with the full classpath - it NEEDS to be self-sufficient
// ...which is also why the code is complete shit, no external libraries.
public class Update {

	private static final String updaterUrlTemplate = "https://xpdota.github.io/event-trigger/%s/%s";
	private static final String defaultBranch = "stable";
	private String branch;
	private static final String manifestFile = "manifest";
	private static final String propsOverrideFileName = "update.properties";
	private final File installDir;
	private final File depsDir;
	private final File propsOverride;
	private final JTextArea textArea;

	private URI makeUrl(String filename) {
		try {
			return new URI(updaterUrlTemplate.formatted(getBranch(), filename));
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private String getBranch() {
		if (this.branch != null) {
			return branch;
		}
		Properties props = new Properties();
		String branch;
		if (propsOverride.exists()) {
			appendText("Properties file exists, loading it...");
			try {
				props.load(new FileInputStream(propsOverride));
			}
			catch (IOException e) {
				appendText("ERROR: Could not read properties!");
				appendText(e.toString());
				appendText(getStackTrace(e));
			}
			branch = props.getProperty("branch");
			if (branch == null) {
				appendText("Branch not specified in properties file, assuming default of " + branch);
				branch = defaultBranch;
			}
		}
		else {
			appendText("Properties file does not exist, creating one with defaults");
			props.setProperty("branch", defaultBranch);
			branch = defaultBranch;
			try {
				props.store(new FileOutputStream(propsOverride), "Created by updater");
			}
			catch (IOException e) {
				appendText("ERROR: Could not save properties!");
				appendText(e.toString());
				appendText(getStackTrace(e));
			}
		}
		this.branch = branch;
		appendText("Using branch: " + branch);
		return branch;
	}

	private Path getLocalFile(String name) {
		return Paths.get(depsDir.toString(), name);
	}

	private final JFrame frame;
	private final JPanel content;
	private final JButton button;
	private final StringBuilder logText = new StringBuilder();
	private final HttpClient client = HttpClient.newHttpClient();

	private Update() {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Throwable e) {
			// Ignore
		}
		frame = new JFrame("Triggevent Updater");
		frame.setSize(new Dimension(800, 500));
		frame.setLocationRelativeTo(null);
		content = new JPanel();
		content.setBorder(new EmptyBorder(10, 10, 10, 10));
		content.setLayout(new BorderLayout());
		frame.add(content);
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setCaretPosition(0);
		textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		JScrollPane scroll = new JScrollPane(textArea);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		content.add(scroll, BorderLayout.CENTER);
		button = new JButton("Wait");
		button.setPreferredSize(new Dimension(80, button.getPreferredSize().height));
		button.addActionListener(l -> System.exit(0));
		JPanel buttonHolder = new JPanel();
		buttonHolder.add(button);
		content.add(buttonHolder, BorderLayout.PAGE_END);
		try {
			File jarLocation = new File(Update.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			if (jarLocation.isFile()) {
				jarLocation = jarLocation.getParentFile();
			}
			this.installDir = jarLocation;
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		depsDir = Paths.get(installDir.toString(), "deps").toFile();
		propsOverride = Paths.get(installDir.toString(), propsOverrideFileName).toFile();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		button.setEnabled(false);
		appendText("Install dir: " + installDir);
		appendText("Starting update check...");
	}

	private synchronized void appendText(String text) {
		logText.append(text).append('\n');
		textArea.setText(logText.toString());
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	private void doUpdateCheck() {
		try {
			HttpResponse<String> manifestResponse = client.send(HttpRequest.newBuilder().GET().uri(makeUrl(manifestFile)).build(), HttpResponse.BodyHandlers.ofString());
			if (manifestResponse.statusCode() != 200) {
				throw new RuntimeException("Bad response: %s: %s".formatted(manifestResponse.statusCode(), manifestResponse));
			}
			String body = manifestResponse.body();
			Map<String, String> expectedFiles = body.lines().map(line -> line.split("\s+")).collect(Collectors.toMap(s -> s[1], s -> s[0]));
			File[] depsFiles = depsDir.listFiles();
			Map<String, String> actualFiles;
			appendText("Hashing Local Files...");
			if (depsFiles == null) {
				actualFiles = Collections.emptyMap();
			}
			else {
				actualFiles = Arrays.stream(depsFiles)
						.parallel()
						.filter(File::isFile)
						.collect(Collectors.toMap(File::getName, Update::md5sum));
			}
			List<String> allKeys = new ArrayList<>();
			allKeys.addAll(actualFiles.keySet());
			allKeys.addAll(expectedFiles.keySet());
			allKeys.sort(String::compareTo);
			Set<String> allKeysSet = new LinkedHashSet<>(allKeys);
			allKeysSet.forEach(key -> {
				appendText("%32s -> %32s %s".formatted(actualFiles.get(key), expectedFiles.get(key), key));
			});
			appendText("Calculating update...");
			List<String> localFilesToDelete = new ArrayList<>();
			List<String> filesToDownload = new ArrayList<>();
			actualFiles.forEach((name, md5) -> {
				String expected = expectedFiles.get(name);
				if (!md5.equals(expected)) {
					localFilesToDelete.add(name);
				}
			});
			expectedFiles.forEach((name, md5) -> {
				String actual = actualFiles.get(name);
				if (!md5.equals(actual)) {
					filesToDownload.add(name);
				}
			});
			appendText(String.format("Updating %s files...", filesToDownload.size()));
			localFilesToDelete.forEach(name -> {
				boolean deleted;
				do {
					deleted = Paths.get(depsDir.toString(), name).toFile().delete();
					if (deleted) {
						return;
					}
					appendText("Could not delete file %s. Make sure the app is not running.");
					try {
						Thread.sleep(5000);
					}
					catch (InterruptedException e) {

					}
				} while (true);
			});

			depsDir.mkdirs();
			depsDir.mkdir();
			AtomicInteger downloaded = new AtomicInteger();
			filesToDownload.parallelStream().forEach((name) -> {
				HttpResponse.BodyHandler<Path> handler = HttpResponse.BodyHandlers.ofFile(getLocalFile(name));
				try {
					client.send(HttpRequest.newBuilder().GET().uri(makeUrl(name)).build(), handler);
				}
				catch (IOException | InterruptedException e) {
					throw new RuntimeException(e);
				}
				appendText(String.format("Downloaded %s / %s files", downloaded.incrementAndGet(), filesToDownload.size()));
			});
			appendText("Update finished! %s files needed to be updated.".formatted(filesToDownload.size()));
			button.setText("Close");
			button.setEnabled(true);
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					Runtime.getRuntime().exec(Paths.get(installDir.toString(), "triggevent.exe").toString());
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}));
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		CatchFatalErrorInUpdater.run(() -> {
			Update update = new Update();
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {

			}
			update.doUpdateCheck();
		});
	}

	private static String md5sum(File file) {
		try (FileInputStream fis = new FileInputStream(file)) {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			try (DigestInputStream dis = new DigestInputStream(fis, md5)) {
				dis.readAllBytes();
			}
			byte[] md5sum = md5.digest();
			StringBuilder md5String = new StringBuilder();
			for (byte b : md5sum) {
				md5String.append(String.format("%02x", b & 0xff));
			}
			return md5String.toString();
		}
		catch (IOException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private static String getStackTrace(final Throwable throwable) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
	}

}
