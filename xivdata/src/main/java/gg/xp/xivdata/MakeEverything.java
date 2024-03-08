package gg.xp.xivdata;

import gg.xp.xivdata.data.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MakeEverything {

	private static final List<Process> processes = new ArrayList<>();


	private final File scDir;
	private final File xivDir;
	private final String version;

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
		if (!xivDir.isDirectory()) {
			printAndFail("The specified XIV dir is not a directory: " + scDir);
		}
		File gameVerFile = xivDir.toPath().resolve("game").resolve("ffxivgame.ver").toFile();
		try {
			version = FileUtils.readLines(gameVerFile, StandardCharsets.UTF_8).get(0).strip();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
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
					.redirectInput(ProcessBuilder.Redirect.PIPE)
//					.redirectError(ProcessBuilder.Redirect.PIPE)
//					.redirectOutput(ProcessBuilder.Redirect.PIPE)
					.directory(scDir);
			Process proc = process.start();
			synchronized (processes) {
				processes.add(proc);
			}
			long pid = proc.pid();
			OutputStream stdin = proc.getOutputStream();
			stdin.write("n\r\n".getBytes(StandardCharsets.UTF_8));
			stdin.flush();
			System.out.println("Started pid " + pid);
//			proc.getInputStream()
			return proc;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// TODO: the folder is actually the defs ver, not game ver, so this doesn't always work
	private String getVer() {
		return version;
//		try {
//			return Files.readString(Paths.get(scDir.toString(), "Definitions", "game.ver")).trim();
//		}
//		catch (IOException e) {
//			throw new RuntimeException(e);
//		}
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

	private void copyFileToDir(List<String> extractedFilePath, List<String> targetDirPath) {
		getTargetFile(targetDirPath.toArray(String[]::new)).mkdirs();
		copyFile(extractedFilePath, targetDirPath);
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
			printAndFail("""
					This requires exactly two arguments. The first is the path to your Saint Coinach directory, the second is the path to the XIV install dir.
					Example:
						MakeEverything "C:\\Users\\FooBar\\Downloads\\Saint Coinach" "C:\\Program Files (x86)\\Steam\\steamapps\\common\\FINAL FANTASY XIV Online"
					""");
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
			Process command = maker.runScCmd("rawexd Action Map Status NpcYell");
			waitForCommand(command);
			maker.copyFileToDir(List.of("rawexd", "Action.csv"), List.of("xiv", "actions"));
			maker.copyFileToDir(List.of("rawexd", "Map.csv"), List.of("xiv", "maps"));
			maker.copyFileToDir(List.of("rawexd", "Status.csv"), List.of("xiv", "statuseffect"));
			maker.copyFileToDir(List.of("rawexd", "NpcYell.csv"), List.of("xiv", "npcyell"));
		}
		{
			// We don't want the raw for this one
			Process command = maker.runScCmd("exd TerritoryType");
			waitForCommand(command);
			maker.copyFileToDir(List.of("exd", "TerritoryType.csv"), List.of("xiv", "territory"));
		}
		List<String> iconDir = List.of("xiv", "icon");
		// General
		{
			// Damage types
			maker.extractAndCopyIconRange(60011, 60013, iconDir);
			// Floor Markers
			maker.extractAndCopyIconRange(61241, 61248, iconDir);
			// Head markers
			maker.extractAndCopyIconRange(60701, 60714, iconDir);
		}

		// STATUS EFFECTS
		{
			StatusEffectLibraryImpl statusLibrary = StatusEffectLibrary.readAltCsv(maker.getTargetFile("xiv", "statuseffect", "Status.csv"));
			Map<Integer, StatusEffectInfo> statusCsvMap = statusLibrary.getAll();
			List<Long> statusIcons;
			// TODO: it looks like the way status effects work is that there is one icon for each stack value.
			// Maximum stack amounts are defined in Status.csv. Maybe it's time to improve the CSV reading?
			// There's also fields for whether it can be dispelled or not.
			{
				statusIcons = statusCsvMap.values().stream().flatMap((data) -> data.getAllIconIds().stream().filter(id -> id > 0)).distinct().toList();
				System.out.println("Number of status effect icons: " + statusIcons.size());
				maker.extractIconRange(statusIcons);
				System.out.println("Copying Icons");
				statusIcons.stream().parallel().forEach(iconNumber -> maker.copyIconIfExists(iconNumber, iconDir));
			}
		}
		// ACTIONS/ABILITIES
		{
			ActionLibraryImpl actionLibrary = ActionLibrary.readAltCsv(maker.getTargetFile("xiv", "actions", "Action.csv"));
			Map<Integer, ActionInfo> actionCsvMap = actionLibrary.getAll();
			List<Long> actionIcons;
			{
				actionIcons = actionCsvMap.values().stream().mapToLong(ActionInfo::iconId).filter(id -> id > 0).distinct().boxed().toList();
				System.out.println("Number of action icons: " + actionIcons.size());
				maker.extractIconRange(actionIcons);
				System.out.println("Copying Icons");
				actionIcons.stream().parallel().forEach(iconNumber -> maker.copyIconIfExists(iconNumber, iconDir));
			}
		}
	}

	private void extractAndCopyIconRange(int startClosed, int endClosed, List<String> dest) {
		extractIconRange(startClosed, endClosed);
		IntStream.rangeClosed(startClosed, endClosed).forEach(icon -> copyIconIfExists(icon, dest));
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
