package gg.xp.xivsupport.groovy;

import gg.xp.compmonitor.CompMonitor;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.scan.Alias;
import gg.xp.reevent.scan.AutoHandlerConfig;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.ManaPoints;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivWorld;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.transform.CompileStatic;
import groovy.transform.TypeChecked;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.GroovySandbox;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.NoOpGroovySandbox;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.RejectASTTransformsCustomizer;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SandboxScope;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.StandardGroovySandbox;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.groovy.sandbox.SandboxTransformer;
import org.picocontainer.PicoContainer;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import static org.reflections.scanners.Scanners.SubTypes;

@ScanMe
public class GroovyManager {

	private static final Logger log = LoggerFactory.getLogger(GroovyManager.class);
	private static final Logger scriptLogger = LoggerFactory.getLogger("gg.xp.xivsupport.groovy.Scripts");
	private final PicoContainer container;
	private final GroovySandbox sandbox;
	private final BooleanSetting sandboxSetting;
	private final AutoHandlerConfig ahc;
	private final boolean useSandbox;
	private Binding binding;

	public GroovyManager(PicoContainer container, Whitelist whitelist, PersistenceProvider pers, AutoHandlerConfig ahc) {
		this.container = container;
		sandboxSetting = new BooleanSetting(pers, "groovy.enable-sandbox", true);
		this.ahc = ahc;
		// TODO: need a secure way to make sure scripts can't change this
		// This seems reasonable enough for now - allow it if the user is specifically running in an IDE, or if
		// it is an integration test using non-persistent settings.
		if (Platform.isInIDE() || pers instanceof InMemoryMapPersistenceProvider) {
			useSandbox = sandboxSetting.get();
			if (!useSandbox) {
				log.warn("SANDBOX IS DISABLED! SCRIPTS CAN DO ANYTHING!");
			}
		}
		else {
			useSandbox = true;
		}
		if (useSandbox) {
			sandbox = new StandardGroovySandbox().withWhitelist(whitelist);
		}
		else {
			sandbox = new NoOpGroovySandbox();
		}
	}

	private final Object ccLock = new Object();
	private volatile @Nullable CompilerConfiguration compilerConfig;

	public CompilerConfiguration getCompilerConfig() {
		if (compilerConfig == null) {
			synchronized (ccLock) {
				if (compilerConfig == null) {
					return compilerConfig = makeCompilerConfig();
				}
			}
		}
		return compilerConfig;
	}

	private final Object clLock = new Object();
	private volatile @Nullable ClassLoader cl;

	public ClassLoader getClassLoader() {
		if (cl == null) {
			synchronized (clLock) {
				if (cl == null) {
					return cl = makeClassLoader();
				}
			}
		}
		return cl;
	}

	private ClassLoader makeClassLoader() {
		return new URLClassLoader(ahc.getAddonJars().toArray(URL[]::new), Thread.currentThread().getContextClassLoader());
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
				Event.class.getCanonicalName(),
				CompileStatic.class.getCanonicalName(),
				TypeChecked.class.getCanonicalName());
		// TODO: add ability effects and other common events here
		importCustomizer.addStarImports(
				"gg.xp.xivsupport.events.actlines.events",
				"java.util",
				"java.util.function",
				"java.time",
				"javax.swing",
				"java.awt",
				"gg.xp.xivdata.data",
				"gg.xp.xivsupport.gui",
				"gg.xp.xivsupport.events.actlines.events.abilityeffect",
				"gg.xp.xivsupport.events.actlines.events.actorcontrol",
				"gg.xp.xivsupport.models",
				"gg.xp.xivsupport.gui.tables",
				"gg.xp.xivsupport.events.triggers.marks",
				"gg.xp.xivsupport.events.triggers.marks.adv"
		);
		Reflections reflections = new Reflections(
				new ConfigurationBuilder()
						.setUrls(ClasspathHelper.forJavaClassPath())
						.setParallel(true)
						.setScanners(Scanners.SubTypes));
		reflections.get(SubTypes.of(Event.class))
				.stream()
//				.map(Class::getCanonicalName)
				.filter(s -> s != null && !s.isBlank())
				.forEach(importCustomizer::addImports);
		// TODO revisit later
//		for (Class<?> s : reflections.get(SubTypes.of(Object.class).asClass(Thread.currentThread().getContextClassLoader()))) {
//			InvokerHelper.getMetaClass(s);
//			try {
//				Introspector.getBeanInfo(s);
//			}
//			catch (IntrospectionException e) {
//				log.error("Bean fail", e);
//			}
//		}
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

		return new GroovyShell(makeClassLoader(), binding, getCompilerConfig());
	}

	private final Object bindLock = new Object();
	private volatile @Nullable Binding globalBinding;

	Binding getGlobalBinding() {
		if (globalBinding == null) {
			synchronized (bindLock) {
				if (globalBinding == null) {
					return globalBinding = makeGlobalBinding();
				}
			}
		}
		return globalBinding;
	}

	public Binding makeBinding() {
		return new SubBinding(getGlobalBinding());
	}

	private Binding makeGlobalBinding() {
		Binding binding = new Binding();
		binding.setVariable("globals", binding);
		// TODO: external classes can't have annotations added here at the moment, so do them manually
		binding.setVariable("pico", container);
		binding.setVariable("container", container);
		binding.setVariable("picoContainer", container);
		binding.setVariable("log", scriptLogger);

		container.getComponent(CompMonitor.class).addAndRunListener(item -> {
			Object instance = item.instance();
			String mainName = StringUtils.uncapitalize(instance.getClass().getSimpleName());
			binding.setVariable(mainName, instance);
			Class<?> itemCls = item.cls();
			List<Class<?>> ifaces = new ArrayList<>(ClassUtils.getAllInterfaces(itemCls));
			ifaces.addAll(ClassUtils.getAllSuperclasses(itemCls));
			ifaces.add(instance.getClass());
			for (Class<?> iface : ifaces) {
				Alias[] aliases = iface.getAnnotationsByType(Alias.class);
				for (Alias alias : aliases) {
					String aliasName = alias.value();
					binding.setVariable(aliasName, instance);
				}
			}
		});

		return binding;
	}

	@HandleEvents(order = -10_000_000)
	public void finishInit(EventContext context, InitEvent init) {
		// Precache some common calls
		getGlobalBinding();
		new Thread("GroovyStartupHelper") {
			@Override
			public void run() {
				precacheCommonStuff();
			}
		}.start();
	}

	public void precacheCommonStuff() {
		// TODO revisit later - is there a way to force invokedynamic lookups early?
//		List<? extends Class<? extends BaseEvent>> classes = List.of(AbilityUsedEvent.class, AbilityCastStart.class);
		// This didn't help much
//		for (Class<? extends BaseEvent> cls : classes) {
//			Method[] methods = cls.getMethods();
//			MethodHandles.Lookup l = MethodHandles.lookup();
//			try {
//				for (Method method : methods) {
//					MethodHandle handle = l.unreflect(method);
//					log.info("Handle: {}", handle);
////					MethodHandle handle = l.findVirtual(cls, method.getName(), MethodType.methodType(method.getReturnType()));
//				}
//			}
////			catch (NoSuchMethodException | IllegalAccessException e) {
//			catch (IllegalAccessException e) {
//				throw new RuntimeException(e);
//			}
//		}
		// This didn't help much
//		ArrayUtil.createArray(1, 2, 3);
		// This helped maybe a tiny bit
//		getGlobalBinding().getVariables().values().forEach(object -> {
//			InvokerHelper.getMetaClass(object).getMethods();
//			try {
//				Introspector.getBeanInfo(object.getClass());
//			}
//			catch (IntrospectionException e) {
//				throw new RuntimeException(e);
//			}
//
//		});
		XivAbility ability = new XivAbility(123, "Foo Ability");
		XivPlayerCharacter player = new XivPlayerCharacter(0x10000001, "Me, The Player", Job.GNB, XivWorld.of(), true, 1, new HitPoints(123, 123), ManaPoints.of(123, 123), new Position(0, 0, 0, 0), 0, 0, 1, 80, 0, 0);
		XivPlayerCharacter otherCharInParty = new XivPlayerCharacter(0x10000002, "Someone Else In My Party", Job.GNB, XivWorld.of(), false, 1, new HitPoints(123, 123), ManaPoints.of(123, 123), new Position(0, 0, 0, 0), 0, 0, 1, 80, 0, 0);
		AbilityCastStart acs = new AbilityCastStart(ability, player, otherCharInParty, 6.0);
		GroovyShell shell = makeShell();
		shell.parse("\"Dummy script\"").run();
		// Run some dummy scripts to force Groovy to cache stuff *before* it's performance-critical
		Script script = shell.parse("""
				"${acs.ability}; ${acs.abilityIdMatches(0x5EF8)}; ${acs.source.name}; ${acs.target.name}"
				new SpecificAutoMarkRequest(acs.target, MarkerSign.CROSS)
								""");
		script.getBinding().setVariable("acs", acs);
		// Uncomment and drop breakpoint on the log.info line to get a stack trace halfway through the script
//		new Thread(() -> {
//			try {
//				Thread.sleep(40);
//			}
//			catch (InterruptedException e) {
//				throw new RuntimeException(e);
//			}
//			log.info("Slept");
//		}).start();
		try (SandboxScope ignored = getSandbox().enter()) {
			script.run();
		}
	}

	public GroovySandbox getSandbox() {
		return sandbox;
	}
}
