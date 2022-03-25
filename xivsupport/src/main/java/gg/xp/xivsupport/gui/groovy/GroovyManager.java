package gg.xp.xivsupport.gui.groovy;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.state.XivState;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.transform.CompileStatic;
import groovy.transform.TypeChecked;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.picocontainer.PicoContainer;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static org.reflections.scanners.Scanners.SubTypes;

@ScanMe
public class GroovyManager {

	private final PicoContainer container;
	private final List<GroovyScriptHolder> scripts = new ArrayList<>();

	public GroovyManager(PicoContainer container) {
		this.container = container;
		{
			GroovyScriptHolder defaultScript = new GroovyScriptHolder();
			defaultScript.scriptName = "Scratch";
			defaultScript.scriptContent = defaultScriptContent;
			defaultScript.shouldSave = false;
			addScript(defaultScript);
		}
		{
			GroovyScriptHolder defaultScript = new GroovyScriptHolder();
			defaultScript.scriptName = "Example";
			defaultScript.scriptContent = "propertiesFilePersistenceProvider.@properties";
			defaultScript.shouldSave = false;
			addScript(defaultScript);
		}

	}

	public List<GroovyScriptHolder> getScripts() {
		return Collections.unmodifiableList(scripts);
	}

	private void addScript(GroovyScriptHolder holder) {
		configureScriptHolder(holder);
		scripts.add(holder);
	}

	private void configureScriptHolder(GroovyScriptHolder holder) {
		if (holder.shellProvider == null) {
			holder.shellProvider = this::makeShell;
		}
	}

	private GroovyShell makeShell() {
		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
		ImportCustomizer importCustomizer = new ImportCustomizer();
		importCustomizer.addImports(
				Predicate.class.getCanonicalName(),
				CompileStatic.class.getCanonicalName(),
				TypeChecked.class.getCanonicalName());
		importCustomizer.addStarImports(
				"gg.xp.xivsupport.events.actlines.events",
				"javax.swing",
				"gg.xp.xivsupport.gui",
				"gg.xp.xivsupport.gui.tables"
		);
		Reflections reflections = new Reflections(
				new ConfigurationBuilder()
						.setUrls(ClasspathHelper.forJavaClassPath())
						.setScanners(Scanners.SubTypes));
		reflections.get(SubTypes.of(Event.class).asClass())
				.stream()
				.map(Class::getCanonicalName)
				.filter(Objects::nonNull)
				.forEach(importCustomizer::addImports);

		compilerConfiguration.addCompilationCustomizers(importCustomizer);
		Binding binding = new Binding();
		GroovyShell shell = new GroovyShell(binding, compilerConfiguration);
		container.getComponents().forEach(item -> {
			String simpleName = item.getClass().getSimpleName();
			simpleName = StringUtils.uncapitalize(simpleName);
			binding.setProperty(simpleName, item);
		});
		// TODO: find a way to systematically do these
		binding.setProperty("xivState", container.getComponent(XivState.class));

		return shell;

	}

	private static final String defaultScriptContent = """
			\"""Hi There!

			This is the Groovy Console. You can run scripts here, written in Groovy (https://groovy-lang.org/).
			For the most part, Java code will also be valid Groovy code, so you can also use this to prototype mainline code.

			By default, everything in the DI container is injected as a variable, with the first letter of the class name lowercased.

			For example, I can see that there are currently ${rawEventStorage.getEvents().size()} events on record. The current player name is ${xivState.getPlayer()?.getName()}.
			
			You could also run propertiesFilePersistenceProvider.@properties to dump all settings into a key/value display.

			Your return type can be a String, a List, Map, or Swing Component. The value will be rendered differently according to its type. In this case, it is a String.
			
			Variables defined here will be scoped locally. If you want it to be persistent across multiple executions, then use binding.setVariable("name", value).
			
			This does NOT have any sandboxing, so don't run random stuff you found on the internet. It can do anything to your system that compiled Java code would be able to do. \"""
			""";

}
