package gg.xp.xivsupport.gui.groovy;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.transform.CompileStatic;
import groovy.transform.TypeChecked;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.GroovySandbox;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.NoOpGroovySandbox;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.StandardGroovySandbox;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.RejectASTTransformsCustomizer;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.groovy.sandbox.SandboxTransformer;
import org.picocontainer.PicoContainer;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Predicate;

import static org.reflections.scanners.Scanners.SubTypes;

@ScanMe
public class GroovyManager {

	private static final Logger log = LoggerFactory.getLogger(GroovyManager.class);
	private final PicoContainer container;
	private final GroovySandbox sandbox;
	private final BooleanSetting sandboxSetting;
	private boolean useSandbox;

	public GroovyManager(PicoContainer container, Whitelist whitelist, PersistenceProvider pers) {
		this.container = container;
		sandboxSetting = new BooleanSetting(pers, "groovy.enable-sandbox", true);
		useSandbox = sandboxSetting.get();
		if (useSandbox) {
			sandbox = new StandardGroovySandbox().withWhitelist(whitelist);
		}
		else {
			sandbox = new NoOpGroovySandbox();
		}
	}

	private final Object cclock = new Object();
	private volatile @Nullable CompilerConfiguration compilerConfig;

	public CompilerConfiguration getCompilerConfig() {
		if (compilerConfig == null) {
			synchronized (cclock) {
				if (compilerConfig == null) {
					return compilerConfig = makeCompilerConfig();
				}
			}
		}
		return compilerConfig;
	}

	private static final Object staticLock = new Object();
	private static volatile ImportCustomizer importCustomizer;

	public static ImportCustomizer getImportCustomizer() {
		if (importCustomizer == null) {
			synchronized (staticLock) {
				if (importCustomizer == null) {
					return importCustomizer = makeImportCustomizer();
				}
			}
		}
		return importCustomizer;
	}

	private static ImportCustomizer makeImportCustomizer() {
		ImportCustomizer importCustomizer = new ImportCustomizer();
		importCustomizer.addImports(
				Predicate.class.getCanonicalName(),
				Event.class.getCanonicalName(),
				CompileStatic.class.getCanonicalName(),
				TypeChecked.class.getCanonicalName());
		// TODO: add ability effects and other common events here
		importCustomizer.addStarImports(
				"gg.xp.xivsupport.events.actlines.events",
				"javax.util",
				"javax.util.function",
				"javax.swing",
				"gg.xp.xivdata.data",
				"gg.xp.xivsupport.gui",
				"gg.xp.xivsupport.gui.tables"
		);
		Reflections reflections = new Reflections(
				new ConfigurationBuilder()
						.setUrls(ClasspathHelper.forJavaClassPath())
						.setParallel(true)
						.setScanners(Scanners.SubTypes));
		reflections.get(SubTypes.of(Event.class).asClass())
				.stream()
				.map(Class::getCanonicalName)
				.filter(Objects::nonNull)
				.forEach(importCustomizer::addImports);
		return importCustomizer;
	}

	private CompilerConfiguration makeCompilerConfig() {
		log.info("Setting up Groovy CompilerConfiguration");
		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
		ImportCustomizer importCustomizer = getImportCustomizer();
		compilerConfiguration.addCompilationCustomizers(importCustomizer);

		if (useSandbox) {
			SandboxTransformer sbt = new SandboxTransformer();
			compilerConfiguration.addCompilationCustomizers(sbt);

			RejectASTTransformsCustomizer rejectAst = new RejectASTTransformsCustomizer();
			compilerConfiguration.addCompilationCustomizers(rejectAst);
		}


		log.info("Done with CompilerConfiguration");
		return compilerConfiguration;
	}

	public GroovyShell makeShell() {
		Binding binding = makeBinding();

		return new GroovyShell(binding, getCompilerConfig());
	}

	public Binding makeBinding() {
		Binding binding = new Binding();
		container.getComponents().forEach(item -> {
			String simpleName = item.getClass().getSimpleName();
			simpleName = StringUtils.uncapitalize(simpleName);
			binding.setProperty(simpleName, item);
		});
		// TODO: find a way to systematically do these
		// TODO: expose user scripts here
		binding.setProperty("xivState", container.getComponent(XivState.class));
		return binding;
	}


	public GroovySandbox getSandbox() {
		return sandbox;
	}
}
