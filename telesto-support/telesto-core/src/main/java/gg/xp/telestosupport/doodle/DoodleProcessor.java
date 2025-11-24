package gg.xp.telestosupport.doodle;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.telestosupport.TelestoMain;
import gg.xp.xivsupport.callouts.SingleValueReplacement;
import gg.xp.xivsupport.callouts.conversions.GlobalCallReplacer;
import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.groovy.GroovyScriptProcessor;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import groovy.lang.Binding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DoodleProcessor implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(DoodleProcessor.class);
	private final ObjectMapper mapper = new ObjectMapper();
	private final TelestoMain telesto;
	private final BooleanSetting doodleSetting;
	private final EventMaster master;
	private final Object dynamicDoodlesLock = new Object();
	private final List<DynamicDoodle> dynamicDoodles = new ArrayList<>();

	private final GroovyManager groovyMgr;
	private final SingleValueReplacement svr;
	private final GlobalCallReplacer gr;
	private final GroovyScriptProcessor gsp;
	private final RefreshLoop<DoodleProcessor> dynamicDoodleRefreshLoop;


	public DoodleProcessor(TelestoMain telesto,
	                       PersistenceProvider pers,
	                       EventMaster master,
	                       GroovyManager mgr,
	                       SingleValueReplacement svr,
	                       GlobalCallReplacer gr,
	                       GroovyScriptProcessor gsp
	                       ) {
		this.telesto = telesto;
		doodleSetting = new BooleanSetting(pers, "telesto-support.doodle-support.enable", false);
		this.master = master;
		groovyMgr = mgr;
		this.svr = svr;
		this.gr = gr;
		this.gsp = gsp;
		// TODO: make refresh time a setting
		this.dynamicDoodleRefreshLoop = new RefreshLoop<>("DynamicDoodleRefresh", this, doodleProcessor -> {
			try {
				doodleProcessor.refreshDynamics();
			} catch (Throwable t) {
				log.error("Error refreshing doodles", t);
				try {
					Thread.sleep(5_000);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}, ignored -> 50L);
	}

	public BooleanSetting enableDoodles() {
		return doodleSetting;
	}

	private final short sessionId = (short) (Math.random() * 16384);
	private final AtomicInteger counter = new AtomicInteger();

	private String nextName() {
		return String.format("anon-doodle-%d-%d", sessionId, counter.getAndIncrement());
	}

	@HandleEvents(order = -1_000)
	public void drawDoodle(EventContext context, CreateDoodleRequest request) {
		DoodleSpec spec = request.getSpec();
		if (spec.name == null) {
			spec.name = nextName();
		}
		if (spec instanceof DynamicDoodle dyn) {
			synchronized (dynamicDoodlesLock) {
				dynamicDoodles.removeIf(that -> that.getName().equals(dyn.getName()));
				Binding binding = groovyMgr.makeBinding();
				// TODO: this might need to be revised when multi-event easy triggers are a thing.
				// This will probably need to move to a dedicated field.
				binding.setVariable("event", request.getParent());
				dyn.setProcessor(new DynamicValueProcessor() {
					@Override
					public <X> X process(String input, Class<X> outputType) {
						return gsp.runScript(input, binding, outputType);
					}

					@Override
					public String processString(DynamicText input) {
						return gsp.replaceInString(input.text(), binding, input.singleReplacements(), input.globalReplacements());
					}
				});
				dynamicDoodles.add(dyn);
				dynamicDoodleRefreshLoop.startIfNotStarted();
				dynamicDoodleRefreshLoop.refreshNow();
			}
		}
		else {
			JsonNode json = mapper.valueToTree(spec);
			context.accept(telesto.makeMessage(2_000_001, "EnableDoodle", json, false));
		}
	}

	@Override
	public boolean enabled(EventContext context) {
		return doodleSetting.get();
	}


	private void refreshDynamics() {
		synchronized (dynamicDoodlesLock) {
			List<DynamicDoodle> toRemove = new ArrayList<>();
			for (DynamicDoodle dyn : dynamicDoodles) {
				if (dyn.isExpired()) {
					toRemove.add(dyn);
					telesto.sendMessageDirectly(telesto.makeMessage(2_000_003, "DisableDoodle", Map.of("name", dyn.getName()), false));
				}
				boolean dirty = dyn.reprocess();
				if (dirty) {
					telesto.sendMessageDirectly(telesto.makeMessage(2_000_002, "EnableDoodle", mapper.valueToTree(dyn), false));
				}
			}
			dynamicDoodles.removeAll(toRemove);
		}
	}
}
