package gg.xp.xivdata;

import gg.xp.xivdata.jobs.ActionIcon;
import gg.xp.xivdata.jobs.StatusEffectInfo;
import gg.xp.xivdata.jobs.StatusEffectIcon;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;

public class MakeEverything {

	private static final List<Process> processes = new ArrayList<>();


	private final File scDir;
	private final File xivDir;

	public MakeEverything(File scDir, File xivDir) {
		this.scDir = scDir;
		this.xivDir = xivDir;
		if (!scDir.exists()) {
			printAndFail("The specified SC dir does not exist: " + scDir);
		}
		if (!scDir.isDirectory()) {
			printAndFail("The specified SC dir is not a directory: " + scDir);
		}

		if (!xivDir.exists()) {
			printAndFail("The specified XIV dir does not exist: " + scDir);
		}
		if (!scDir.isDirectory()) {
			printAndFail("The specified XIV dir is not a directory: " + scDir);
		}
	}

	private Process runScCmd(String scArg) {
		return runScCmd(Collections.singletonList(scArg));
	}

	private Process runScCmd(List<String> scArgs) {
		List<String> allArgs = new ArrayList<>();
		allArgs.add(Paths.get(scDir.toString(), "SaintCoinach.Cmd.exe").toString());
		allArgs.add(xivDir.toString());
		allArgs.addAll(scArgs);
		try {
			ProcessBuilder process = new ProcessBuilder(allArgs)
					.redirectError(ProcessBuilder.Redirect.INHERIT)
					.redirectOutput(ProcessBuilder.Redirect.INHERIT)
//					.redirectError(ProcessBuilder.Redirect.PIPE)
//					.redirectOutput(ProcessBuilder.Redirect.PIPE)
					.directory(scDir);
			Process proc = process.start();
			synchronized (processes) {
				processes.add(proc);
			}
			long pid = proc.pid();
			System.out.println("Started pid " + pid);
//			proc.getInputStream()
			return proc;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String getVer() {
		try {
			return Files.readString(Paths.get(scDir.toString(), "Definitions", "game.ver")).trim();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Path getOutputDir() {
		return Paths.get(scDir.toString(), getVer());
	}

	private File getExtractedFile(String... filePathParts) {
		return Paths.get(getOutputDir().toString(), filePathParts).toFile();
	}

	private File getTargetFile(String... filePathParts) {
		return Paths.get(Paths.get(System.getProperty("user.dir"), "src", "main", "resources").toString(), filePathParts).toFile();
	}

	private void copyFile(List<String> extractedFilePath, List<String> targetFilePath) {
		File targetFile = getTargetFile(targetFilePath.toArray(String[]::new));
		File extractedFile = getExtractedFile(extractedFilePath.toArray(String[]::new));
		String name = extractedFilePath.get(extractedFilePath.size() - 1);
		if (targetFile.isDirectory()) {
			targetFile = Paths.get(targetFile.toString(), name).toFile();
		}
		try {
			if (name.endsWith(".csv")) {
				if (targetFile.exists()) {
					if (!targetFile.delete()) {
						throw new RuntimeException("Could not delete target file: " + extractedFile);
					}
				}

				// Convert line endings
				try (LineIterator iter = FileUtils.lineIterator(extractedFile);
				     FileOutputStream fos = new FileOutputStream(targetFile)) {
					iter.forEachRemaining(line -> {
						try {
							fos.write(line.getBytes(StandardCharsets.UTF_8));
							fos.write('\n');
						}
						catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
				}
			}
			else {
				FileUtils.copyFile(extractedFile,
						targetFile,
						StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void copyIconIfExists(long iconId, List<String> dest) {
		String dirName = String.format("%06d", Math.floorDiv(iconId, 1000) * 1000);
		String iconName = String.format("%06d_hr1.png", iconId);
		try {
			copyFile(List.of("ui", "icon", dirName, iconName), dest);
		}
		catch (Throwable t) {
			if (t.getMessage().contains("FileNotFound")) {
				//
			}
			else {
				throw t;
			}
		}
	}

	private void extractIconRange(Collection<Long> icons) {
		LongSummaryStatistics statusStats = icons.stream().mapToLong(Long::longValue).summaryStatistics();
		long min = statusStats.getMin();
		long max = statusStats.getMax();
		extractIconRange(min, max);
	}

	private void extractIconRange(long min, long max) {
		System.out.println("Extracting Icons");
		long delta = max - min;
		int partitions = 4;
		List<Process> commands = new ArrayList<>(partitions);
		for (long i = 0; i < partitions; i++) {
			long thisMin = min + delta * i / partitions;
			if (i > 0) {
				// avoid fenceposting
				thisMin++;
			}
			long thisMax = min + delta * (i + 1) / partitions;
			Process iconsCmd = runScCmd("uihd " + thisMin + " " + thisMax);
			commands.add(iconsCmd);
		}
		// TODO: optimize this
		commands.forEach(MakeEverything::waitForCommand);
		System.out.println("Done extracting icons");
	}

	public static void main(String[] args) {
		if (args.length != 2) {
			printAndFail("This requires exactly two arguments. The first is the path to your Saint Coinach directory, the second is the path to the XIV install dir.");
		}
		if (!System.getProperty("user.dir").endsWith("xivdata")) {
			printAndFail("This should be run from the xivdata directory");
		}
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			synchronized (processes) {
				processes.forEach(Process::destroyForcibly);
			}
		}));

		File scDir = new File(args[0]);
		File xivDir = new File(args[1]);

		MakeEverything maker = new MakeEverything(scDir, xivDir);

		System.out.println("Game Ver: " + maker.getVer());


		// CSV FILES
		{
			Process command = maker.runScCmd("rawexd Action Map Status");
			waitForCommand(command);
			maker.copyFile(List.of("rawexd", "Action.csv"), List.of("xiv", "actions"));
			maker.copyFile(List.of("rawexd", "Map.csv"), List.of("xiv", "maps"));
			maker.copyFile(List.of("rawexd", "Status.csv"), List.of("xiv", "statuseffect"));
		}

		// STATUS EFFECTS
		{
			StatusEffectIcon.readAltCsv(maker.getTargetFile("xiv", "statuseffect", "Status.csv"));
			Map<Long, StatusEffectInfo> statusCsvMap = StatusEffectIcon.getCsvValues();
			List<Long> statusIcons;
			// TODO: it looks like the way status effects work is that there is one icon for each stack value.
			// Maximum stack amounts are defined in Status.csv. Maybe it's time to improve the CSV reading?
			// There's also fields for whether it can be dispelled or not.
			{
				statusIcons = statusCsvMap.values().stream().flatMap((data) -> data.getAllIconIds().stream()).distinct().toList();
				System.out.println("Number of status effect icons: " + statusIcons.size());
				maker.extractIconRange(statusIcons);
				System.out.println("Copying Icons");
				List<String> statusIconDir = List.of("xiv", "statuseffect", "icons");
				statusIcons.stream().parallel().forEach(iconNumber -> maker.copyIconIfExists(iconNumber, statusIconDir));
			}
		}
		// ACTIONS/ABILITIES
		{
			ActionIcon.readAltCsv(maker.getTargetFile("xiv", "actions", "Action.csv"));
			Map<Long, Long> actionCsvMap = ActionIcon.getCsvValues();
			List<Long> actionIcons;
			{
				actionIcons = actionCsvMap.values().stream().distinct().toList();
				System.out.println("Number of action icons: " + actionIcons.size());
				maker.extractIconRange(actionCsvMap.values());
				System.out.println("Copying Icons");
				List<String> actionIconDir = List.of("xiv", "actions", "icons");
				actionIcons.stream().parallel().forEach(iconNumber -> maker.copyIconIfExists(iconNumber, actionIconDir));
			}
		}
	}

	private static int waitForCommand(Process command) {
		int i;
		try {
			i = command.waitFor();
			System.out.println("Return value: " + i);
			return i;
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static void printAndFail(String error) {
		System.out.println(error);
		System.exit(1);
	}
}
