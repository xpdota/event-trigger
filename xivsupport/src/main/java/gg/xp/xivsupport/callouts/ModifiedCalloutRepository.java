package gg.xp.xivsupport.callouts;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.gui.overlay.FlyingTextOverlay;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ModifiedCalloutRepository {

	private static final Logger log = LoggerFactory.getLogger(ModifiedCalloutRepository.class);

	private final PicoContainer container;
	private final PersistenceProvider persistence;
	private final BooleanSetting enableTts;
	private final CalloutDefaultsRepository globalDefaults;
	private final BooleanSetting enableOverlay;

	public ModifiedCalloutRepository(PicoContainer container, PersistenceProvider persistence, FlyingTextOverlay overlay, CalloutDefaultsRepository defaults) {
		this.container = container;
		this.persistence = persistence;
		enableOverlay = overlay.getEnabled();
		enableTts = new BooleanSetting(persistence, "enable-tts-callouts", true);
		this.globalDefaults = defaults;
	}

	private final List<CalloutGroup> allCallouts = new ArrayList<>();

	@HandleEvents
	public void handleEvent(EventContext context, InitEvent init) {
		List<Object> objects = container.getComponents(Object.class)
				.stream()
				.filter(o -> o.getClass().isAnnotationPresent(CalloutRepo.class)).sorted(Comparator.comparing(o -> {
					String canonicalName = o.getClass().getCanonicalName();
					if (canonicalName == null) {
						return "";
					}
					return canonicalName;
				})).toList();

		objects.forEach(o -> {
			Class<?> clazz = o.getClass();
			String description = clazz.getAnnotation(CalloutRepo.class).name();
			List<ModifiedCalloutHandle> callouts = new ArrayList<>();
			List<Field> calloutFields = Arrays.stream(clazz.getDeclaredFields()).filter(f -> ModifiableCallout.class.isAssignableFrom(f.getType())).toList();
			String classPropStub = "callouts." + clazz.getCanonicalName();
			String topLevelPropStub = "callouts.group." + clazz.getCanonicalName();
			calloutFields.forEach(f -> {
				String fieldName = f.getName();
				String fullPropStub = classPropStub + '.' + fieldName;
				f.setAccessible(true);
				ModifiableCallout<?> original;
				try {
					original = (ModifiableCallout<?>) f.get(o);
				}
				catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
				ModifiedCalloutHandle modified = ModifiedCalloutHandle.installHandle(f, original, persistence, fullPropStub, enableTts, enableOverlay, globalDefaults.getGlobalDefaults());
				callouts.add(modified);
			});
			List<Field> varFields = Arrays.stream(clazz.getDeclaredFields()).filter(f -> CalloutVar.class.isAssignableFrom(f.getType())).toList();
			String varsPropStub = "callout-vars." + clazz.getCanonicalName();
			List<CalloutVarHandle> vars = new ArrayList<>();
			varFields.forEach(f -> {
				String fieldName = f.getName();
				String fullpropStub = varsPropStub + '.' + fieldName;
				f.setAccessible(true);
				CalloutVar original;
				try {
					original = (CalloutVar) f.get(o);
				}
				catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
				CalloutVarHandle modified = CalloutVarHandle.installHandle(f, original, persistence, fullpropStub);
				vars.add(modified);
			});

			CalloutGroup cg;
			if (o instanceof OverridesCalloutGroupEnabledSetting override) {
				cg = new CalloutGroup(clazz, description, override.getCalloutGroupEnabledSetting(), callouts, vars);
			}
			else {
				cg = new CalloutGroup(clazz, description, topLevelPropStub, persistence, callouts, vars);
			}
			allCallouts.add(cg);
		});
		log.info("Found {} callout repo classes", allCallouts.size());
	}

	public List<CalloutGroup> getAllCallouts() {
		return Collections.unmodifiableList(allCallouts);
	}

	public BooleanSetting getEnableOverlay() {
		return enableOverlay;
	}

	public BooleanSetting getEnableTts() {
		return enableTts;
	}
}

