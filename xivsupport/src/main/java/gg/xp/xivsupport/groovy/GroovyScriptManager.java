package gg.xp.xivsupport.groovy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.groovy.GroovyScriptHolder;
import gg.xp.xivsupport.gui.groovy.ScriptNameAndFileStub;
import gg.xp.xivsupport.gui.tables.filters.ValidationError;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.settings.ExternalObservable;
import groovy.transform.builder.InitializerStrategy;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ScanMe
public class GroovyScriptManager {
	private static final Logger log = LoggerFactory.getLogger(GroovyScriptManager.class);
	private final List<GroovyScriptHolder> scripts = new ArrayList<>();
	private final ObjectMapper mapper = new ObjectMapper();
	private final ExternalObservable obs = new ExternalObservable();
	private final GroovyManager mgr;

	public GroovyScriptManager(GroovyManager mgr) {
		this.mgr = mgr;
		// TODO: move this
		if (System.getProperty("skip-groovy-script-load", "false").equalsIgnoreCase("true")) {
			log.info("Skipping script load");
		}
		else {
			loadScripts();
		}
	}

	public List<GroovyScriptHolder> getScripts() {
		return Collections.unmodifiableList(scripts);
	}

	private void loadScripts() {
		scripts.clear();
		{
			GroovyScriptHolder defaultScript = new GroovyScriptHolder();
			defaultScript.setScriptName("Scratch");
			defaultScript.setScriptContent(defaultScriptContent);
			addScript(defaultScript);
		}
		{
			GroovyScriptHolder defaultScript = new GroovyScriptHolder();
			defaultScript.setScriptName("Example");
			defaultScript.setScriptContent("propertiesFilePersistenceProvider.@properties");
			addScript(defaultScript);
		}
		File groovyDir = Platform.getGroovyDir().toFile();
		if (groovyDir.exists() && groovyDir.isDirectory()) {

			File[] scriptFiles = groovyDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith("groovy"));
			if (scriptFiles == null) {
				log.warn("scriptFiles == null");
				return;
			}
			Arrays.stream(scriptFiles).parallel().map(scriptFile -> {
				try {
					GroovyScriptHolder script = readFile(scriptFile);
					log.info("Read script file '{}' into script '{}'", scriptFile, script.getScriptName());
					return script;
				}
				catch (Throwable t) {
					log.error("Error reading script file '{}'", scriptFile, t);
					return null;
				}
			}).filter(Objects::nonNull).forEachOrdered(this::addScript);
		}
		obs.notifyListeners();
	}


	@HandleEvents
	public void runStartupScripts(EventContext context, InitEvent init) {
		scripts.forEach(script -> {
			if (script.isStartup()) {
				script.run();
			}
		});
	}

	public void reloadAll() {
		loadScripts();
	}

	private void addScript(GroovyScriptHolder holder) {
		configureScriptHolder(holder);
		scripts.add(holder);
		obs.notifyListeners();
	}

	private void removeScript(GroovyScriptHolder holder) {
		scripts.remove(holder);
		obs.notifyListeners();
	}

	public void addListener(Runnable listener) {
		obs.addListener(listener);
	}

	public void removeListener(Runnable listener) {
		obs.removeListener(listener);
	}

	private static final Pattern validFilenameStubPattern = Pattern.compile("[A-Za-z0-9-._]+");

	public void validateNewScriptName(String scriptName) {
		if (scriptName == null || scriptName.isBlank()) {
			throw new ValidationError("Script name must not be null/blank");
		}
		if (scripts.stream().anyMatch(script -> script.getScriptName().equalsIgnoreCase(scriptName))) {
			throw new ValidationError("Script name '%s' already in use".formatted(scriptName));
		}
	}

	public void validateNewScriptFile(String filenameStub) {
		if (filenameStub == null || filenameStub.isBlank()) {
			throw new ValidationError("Filename stub must not be null/blank");
		}
		if (!validFilenameStubPattern.matcher(filenameStub).matches()) {
			throw new ValidationError("Filename '%s' contains illegal characters".formatted(filenameStub));
		}
		File fileForStub = getFileForStub(filenameStub);
		if (fileForStub.exists()) {
			throw new ValidationError("File already exists");
		}
	}

	public GroovyScriptHolder createAndAddNew(ScriptNameAndFileStub newNameAndFile) {
		String scriptName = newNameAndFile.name();
		String filenameStub = newNameAndFile.fileStub();
		validateNewScriptName(scriptName);
		validateNewScriptFile(filenameStub);
		GroovyScriptHolder script = new GroovyScriptHolder();
		script.setScriptName(scriptName);
		script.setFile(getFileForStub(filenameStub));
		log.info("Creating new script '{}' at '{}'", scriptName, script.getFile());
		addScript(script);
		return script;
	}

	public GroovyScriptHolder cloneAs(GroovyScriptHolder existing, ScriptNameAndFileStub newNameAndFile) {
		String scriptName = newNameAndFile.name();
		String filenameStub = newNameAndFile.fileStub();
		validateNewScriptName(scriptName);
		validateNewScriptFile(filenameStub);
		GroovyScriptHolder script = existing.copyAs(scriptName, getFileForStub(filenameStub));
		log.info("Creating new script '{}' at '{}'", scriptName, script.getFile());
		addScript(script);
		return script;
	}

	public void renameScript(GroovyScriptHolder holder, ScriptNameAndFileStub newNameAndFile) {
		String scriptName = newNameAndFile.name();
		String filenameStub = newNameAndFile.fileStub();
		validateNewScriptName(scriptName);
		validateNewScriptFile(filenameStub);
		holder.setScriptName(scriptName);
		holder.setFile(getFileForStub(filenameStub));
		obs.notifyListeners();
	}

	public void delete(GroovyScriptHolder scriptToDelete) {
		File file = scriptToDelete.getFile();
		if (file == null) {
			throw new IllegalArgumentException("Cannot delete a script that doesn't have a file");
		}
		if (file.exists()) {
			boolean deleted = file.delete();
			if (!deleted) {
				throw new IllegalArgumentException("Failed to delete file '" + file + '\'');
			}
		}
		removeScript(scriptToDelete);
	}

	private File getFileForStub(String stub) {
		return Paths.get(Platform.getGroovyDir().toString(), stub + ".groovy").toFile();
	}

	public void saveAll() {
		for (GroovyScriptHolder script : scripts) {
			if (script.isSaveable()) {
				script.save();
			}
		}
	}

	private static final String propFormat = "// PROP: %s=%s";
	private static final Pattern propPattern = Pattern.compile("// PROP: ([^=]+)=(.*)");

	private GroovyScriptHolder readFile(File file) {
		String content;
		try {
			content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			// TODO: error handling
			throw new RuntimeException(e);
		}
		// Scan for embedded props
		Map<String, String> rawProps = new HashMap<>();
		List<String> lines = new ArrayList<>();
		Iterator<String> iter = content.lines().iterator();
		while (iter.hasNext()) {
			String line = iter.next();
			Matcher matcher = propPattern.matcher(line);
			if (matcher.matches()) {
				rawProps.put(matcher.group(1), matcher.group(2));
			}
			// Trim off the empty lines
			else if (!line.trim().isEmpty()) {
				lines.add(line);
				iter.forEachRemaining(lines::add);
			}
		}
		GroovyScriptHolder script = mapper.convertValue(rawProps, GroovyScriptHolder.class);
		script.setScriptContent(String.join("\n", lines));
		if (script.getScriptName() == null) {
			script.setScriptName(file.getName());
		}
		script.setFile(file);
		return script;
	}

	public void saveScript(GroovyScriptHolder script) {
		File outFile = script.getFile();
		if (outFile == null) {
			throw new IllegalArgumentException("Cannot save a script with no associated file");
		}
		log.info("Saving script '{}' into file '{}'", script.getScriptName(), outFile);
		StringBuilder fileOut = new StringBuilder();
		Map<String, String> props = mapper.convertValue(script, new TypeReference<>() {
		});
		props.forEach((k, v) -> fileOut.append(propFormat.formatted(k, v)).append('\n'));
		// Insert a newline so that properties can't be pasted in
		fileOut.append('\n');
		fileOut.append(script.getScriptContent());
		try {
			outFile.getParentFile().mkdirs();
			FileUtils.writeStringToFile(outFile, fileOut.toString(), StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new RuntimeException("Error writing script to disk");
		}
	}

	public void reloadScript(GroovyScriptHolder script) {
		GroovyScriptHolder newScript = readFile(script.getFile());
		script.setScriptContent(newScript.getScriptContent());
	}

	private void configureScriptHolder(GroovyScriptHolder holder) {
		if (!holder.isMgrSet()) {
			holder.setScriptMgr(this);
		}
	}

	public GroovyManager getGroovyManager() {
		return mgr;
	}

	private static final String defaultScriptContent = """
			\"""Hi There!

			This is the Groovy Console. You can run scripts here, written in Groovy (https://groovy-lang.org/).
			For the most part, Java code will also be valid Groovy code, so you can also use this to prototype mainline code.

			By default, everything in the DI container is injected as a variable, with the first letter of the class name lowercased.

			For example, I can see that there are currently ${rawEventStorage.events.size()} events on record. The current player name is ${xivState.player?.name}.
						
			You could also run propertiesFilePersistenceProvider.@properties to dump all settings into a key/value display.

			Your return type can be a String, a List, Map, or Swing Component. The value will be rendered differently according to its type. In this case, it is a String.
						
			Variables defined here will be scoped locally. If you want it to be persistent across multiple executions, then use binding.setVariable("name", value).
			You can also store a global variable with globals.varName = value. \"""
			""";

}
