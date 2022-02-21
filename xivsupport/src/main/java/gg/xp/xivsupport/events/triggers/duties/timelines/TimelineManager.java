package gg.xp.xivsupport.events.triggers.duties.timelines;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.events.MapChangeEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.VictoryEvent;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TimelineManager {

	private static final Logger log = LoggerFactory.getLogger(TimelineManager.class);
	private static final Map<Long, String> zoneIdToTimelineFile = new HashMap<>();
	private final BooleanSetting debugMode;
	private final BooleanSetting prePullShow;
	private final BooleanSetting resetOnMapChange;
	private final PersistenceProvider pers;
	private final IntSetting rowsToDisplay;
	private final IntSetting secondsPast;
	private final IntSetting secondsFuture;

	static {
		zoneIdToTimelineFile.put(134L, "test.txt");
		zoneIdToTimelineFile.put(193L, "t10.txt");
		zoneIdToTimelineFile.put(194L, "t11.txt");
		zoneIdToTimelineFile.put(195L, "t12.txt");
		zoneIdToTimelineFile.put(196L, "t13.txt");
		zoneIdToTimelineFile.put(202L, "ifrit-nm.txt");
		zoneIdToTimelineFile.put(206L, "titan-nm.txt");
		zoneIdToTimelineFile.put(244L, "t4.txt");
		zoneIdToTimelineFile.put(245L, "t5.txt");
		zoneIdToTimelineFile.put(293L, "titan-hm.txt");
		zoneIdToTimelineFile.put(296L, "titan-ex.txt");
		zoneIdToTimelineFile.put(332L, "cape_westwind.txt");
		zoneIdToTimelineFile.put(355L, "t6.txt");
		zoneIdToTimelineFile.put(356L, "t7.txt");
		zoneIdToTimelineFile.put(357L, "t8.txt");
		zoneIdToTimelineFile.put(358L, "t9.txt");
		zoneIdToTimelineFile.put(359L, "levi-ex.txt");
		zoneIdToTimelineFile.put(377L, "shiva-hm.txt");
		zoneIdToTimelineFile.put(378L, "shiva-ex.txt");
		zoneIdToTimelineFile.put(421L, "the_vault.txt");
		zoneIdToTimelineFile.put(430L, "fractal_continuum.txt");
		zoneIdToTimelineFile.put(438L, "aetherochemical_research_facility.txt");
		zoneIdToTimelineFile.put(441L, "sohm_al.txt");
		zoneIdToTimelineFile.put(446L, "ravana-ex.txt");
		zoneIdToTimelineFile.put(449L, "a1s.txt");
		zoneIdToTimelineFile.put(450L, "a2s.txt");
		zoneIdToTimelineFile.put(451L, "a3s.txt");
		zoneIdToTimelineFile.put(452L, "a4s.txt");
		zoneIdToTimelineFile.put(521L, "a6n.txt");
		zoneIdToTimelineFile.put(523L, "a8n.txt");
		zoneIdToTimelineFile.put(524L, "sephirot-ex.txt");
		zoneIdToTimelineFile.put(529L, "a5s.txt");
		zoneIdToTimelineFile.put(530L, "a6s.txt");
		zoneIdToTimelineFile.put(531L, "a7s.txt");
		zoneIdToTimelineFile.put(532L, "a8s.txt");
		zoneIdToTimelineFile.put(556L, "weeping_city.txt");
		zoneIdToTimelineFile.put(572L, "xelphatol.txt");
		zoneIdToTimelineFile.put(577L, "sophia-ex.txt");
		zoneIdToTimelineFile.put(578L, "gubal_library_hard.txt");
		zoneIdToTimelineFile.put(583L, "a12n.txt");
		zoneIdToTimelineFile.put(584L, "a9s.txt");
		zoneIdToTimelineFile.put(585L, "a10s.txt");
		zoneIdToTimelineFile.put(586L, "a11s.txt");
		zoneIdToTimelineFile.put(587L, "a12s.txt");
		zoneIdToTimelineFile.put(615L, "baelsars_wall.txt");
		zoneIdToTimelineFile.put(616L, "shisui_of_the_violet_tides.txt");
		zoneIdToTimelineFile.put(617L, "sohm_al_hard.txt");
		zoneIdToTimelineFile.put(623L, "bardams_mettle.txt");
		zoneIdToTimelineFile.put(626L, "sirensong_sea.txt");
		zoneIdToTimelineFile.put(627L, "dun_scaith.txt");
		zoneIdToTimelineFile.put(660L, "doma_castle.txt");
		zoneIdToTimelineFile.put(661L, "castrum_abania.txt");
		zoneIdToTimelineFile.put(662L, "kugane_castle.txt");
		zoneIdToTimelineFile.put(663L, "temple_of_the_fist.txt");
		zoneIdToTimelineFile.put(674L, "susano.txt");
		zoneIdToTimelineFile.put(677L, "susano-ex.txt");
		zoneIdToTimelineFile.put(679L, "shinryu.txt");
		zoneIdToTimelineFile.put(689L, "ala_mhigo.txt");
		zoneIdToTimelineFile.put(691L, "o1n.txt");
		zoneIdToTimelineFile.put(692L, "o2n.txt");
		zoneIdToTimelineFile.put(693L, "o3n.txt");
		zoneIdToTimelineFile.put(694L, "o4n.txt");
		zoneIdToTimelineFile.put(695L, "o1s.txt");
		zoneIdToTimelineFile.put(696L, "o2s.txt");
		zoneIdToTimelineFile.put(697L, "o3s.txt");
		zoneIdToTimelineFile.put(698L, "o4s.txt");
		zoneIdToTimelineFile.put(719L, "lakshmi.txt");
		zoneIdToTimelineFile.put(720L, "lakshmi-ex.txt");
		zoneIdToTimelineFile.put(730L, "shinryu-ex.txt");
		zoneIdToTimelineFile.put(731L, "drowned_city_of_skalla.txt");
		zoneIdToTimelineFile.put(733L, "unending_coil_ultimate.txt");
		zoneIdToTimelineFile.put(734L, "royal_city_of_rabanastre.txt");
		zoneIdToTimelineFile.put(742L, "hells_lid.txt");
		zoneIdToTimelineFile.put(743L, "fractal_continuum_hard.txt");
		zoneIdToTimelineFile.put(746L, "byakko.txt");
		zoneIdToTimelineFile.put(748L, "o5n.txt");
		zoneIdToTimelineFile.put(749L, "o6n.txt");
		zoneIdToTimelineFile.put(750L, "o7n.txt");
		zoneIdToTimelineFile.put(751L, "o8n.txt");
		zoneIdToTimelineFile.put(752L, "o5s.txt");
		zoneIdToTimelineFile.put(753L, "o6s.txt");
		zoneIdToTimelineFile.put(754L, "o7s.txt");
		zoneIdToTimelineFile.put(755L, "o8s.txt");
		zoneIdToTimelineFile.put(758L, "byakko-ex.txt");
		zoneIdToTimelineFile.put(768L, "swallows_compass.txt");
		zoneIdToTimelineFile.put(776L, "ridorana_lighthouse.txt");
		zoneIdToTimelineFile.put(777L, "ultima_weapon_ultimate.txt");
		zoneIdToTimelineFile.put(778L, "tsukuyomi.txt");
		zoneIdToTimelineFile.put(779L, "tsukuyomi-ex.txt");
		zoneIdToTimelineFile.put(788L, "st_mocianne_hard.txt");
		zoneIdToTimelineFile.put(789L, "the_burn.txt");
		zoneIdToTimelineFile.put(793L, "ghimlyt_dark.txt");
		zoneIdToTimelineFile.put(798L, "o9n.txt");
		zoneIdToTimelineFile.put(799L, "o10n.txt");
		zoneIdToTimelineFile.put(800L, "o11n.txt");
		zoneIdToTimelineFile.put(801L, "o12n.txt");
		zoneIdToTimelineFile.put(802L, "o9s.txt");
		zoneIdToTimelineFile.put(803L, "o10s.txt");
		zoneIdToTimelineFile.put(804L, "o11s.txt");
		zoneIdToTimelineFile.put(805L, "o12s.txt");
		zoneIdToTimelineFile.put(806L, "yojimbo.txt");
		zoneIdToTimelineFile.put(810L, "suzaku.txt");
		zoneIdToTimelineFile.put(811L, "suzaku-ex.txt");
		zoneIdToTimelineFile.put(821L, "dohn_mheg.txt");
		zoneIdToTimelineFile.put(822L, "mt_gulg.txt");
		zoneIdToTimelineFile.put(823L, "qitana_ravel.txt");
		zoneIdToTimelineFile.put(824L, "seiryu.txt");
		zoneIdToTimelineFile.put(825L, "seiryu-ex.txt");
		zoneIdToTimelineFile.put(826L, "orbonne_monastery.txt");
		zoneIdToTimelineFile.put(827L, "eureka_hydatos.txt");
		zoneIdToTimelineFile.put(836L, "malikahs_well.txt");
		zoneIdToTimelineFile.put(837L, "holminster_switch.txt");
		zoneIdToTimelineFile.put(838L, "amaurot.txt");
		zoneIdToTimelineFile.put(840L, "twinning.txt");
		zoneIdToTimelineFile.put(841L, "akadaemia_anyder.txt");
		zoneIdToTimelineFile.put(845L, "titania.txt");
		zoneIdToTimelineFile.put(846L, "innocence.txt");
		zoneIdToTimelineFile.put(847L, "hades.txt");
		zoneIdToTimelineFile.put(848L, "innocence-ex.txt");
		zoneIdToTimelineFile.put(849L, "e1n.txt");
		zoneIdToTimelineFile.put(850L, "e2n.txt");
		zoneIdToTimelineFile.put(851L, "e3n.txt");
		zoneIdToTimelineFile.put(852L, "e4n.txt");
		zoneIdToTimelineFile.put(853L, "e1s.txt");
		zoneIdToTimelineFile.put(854L, "e2s.txt");
		zoneIdToTimelineFile.put(855L, "e3s.txt");
		zoneIdToTimelineFile.put(856L, "e4s.txt");
		zoneIdToTimelineFile.put(858L, "titania-ex.txt");
		zoneIdToTimelineFile.put(882L, "the_copied_factory.txt");
		zoneIdToTimelineFile.put(884L, "the_grand_cosmos.txt");
		zoneIdToTimelineFile.put(885L, "hades-ex.txt");
		zoneIdToTimelineFile.put(887L, "the_epic_of_alexander.txt");
		zoneIdToTimelineFile.put(897L, "ruby_weapon.txt");
		zoneIdToTimelineFile.put(898L, "anamnesis_anyder.txt");
		zoneIdToTimelineFile.put(902L, "e5n.txt");
		zoneIdToTimelineFile.put(903L, "e6n.txt");
		zoneIdToTimelineFile.put(904L, "e7n.txt");
		zoneIdToTimelineFile.put(905L, "e8n.txt");
		zoneIdToTimelineFile.put(906L, "e5s.txt");
		zoneIdToTimelineFile.put(907L, "e6s.txt");
		zoneIdToTimelineFile.put(908L, "e7s.txt");
		zoneIdToTimelineFile.put(909L, "e8s.txt");
		zoneIdToTimelineFile.put(912L, "ruby_weapon-ex.txt");
		zoneIdToTimelineFile.put(913L, "varis-ex.txt");
		zoneIdToTimelineFile.put(916L, "heroes_gauntlet.txt");
		zoneIdToTimelineFile.put(917L, "the_puppets_bunker.txt");
		zoneIdToTimelineFile.put(920L, "bozjan_southern_front.txt");
		zoneIdToTimelineFile.put(922L, "wol.txt");
		zoneIdToTimelineFile.put(923L, "wol-ex.txt");
		zoneIdToTimelineFile.put(930L, "shiva-un.txt");
		zoneIdToTimelineFile.put(933L, "matoyas_relict.txt");
		zoneIdToTimelineFile.put(934L, "emerald_weapon.txt");
		zoneIdToTimelineFile.put(935L, "emerald_weapon-ex.txt");
		zoneIdToTimelineFile.put(936L, "delubrum_reginae.txt");
		zoneIdToTimelineFile.put(937L, "delubrum_reginae_savage.txt");
		zoneIdToTimelineFile.put(938L, "paglthan.txt");
		zoneIdToTimelineFile.put(942L, "e9n.txt");
		zoneIdToTimelineFile.put(943L, "e10n.txt");
		zoneIdToTimelineFile.put(944L, "e11n.txt");
		zoneIdToTimelineFile.put(945L, "e12n.txt");
		zoneIdToTimelineFile.put(946L, "e9s.txt");
		zoneIdToTimelineFile.put(947L, "e10s.txt");
		zoneIdToTimelineFile.put(948L, "e11s.txt");
		zoneIdToTimelineFile.put(949L, "e12s.txt");
		zoneIdToTimelineFile.put(950L, "diamond_weapon.txt");
		zoneIdToTimelineFile.put(951L, "diamond_weapon-ex.txt");
		zoneIdToTimelineFile.put(952L, "the_tower_of_zot.txt");
		zoneIdToTimelineFile.put(953L, "titan-un.txt");
		zoneIdToTimelineFile.put(966L, "the_tower_at_paradigms_breach.txt");
		zoneIdToTimelineFile.put(969L, "the_tower_of_babil.txt");
		zoneIdToTimelineFile.put(970L, "vanaspati.txt");
		zoneIdToTimelineFile.put(972L, "levi-un.txt");
		zoneIdToTimelineFile.put(973L, "the_dead_ends.txt");
		zoneIdToTimelineFile.put(974L, "ktisis_hyperboreia.txt");
		zoneIdToTimelineFile.put(975L, "zadnor.txt");
		zoneIdToTimelineFile.put(976L, "smileton.txt");
		zoneIdToTimelineFile.put(978L, "the_aitiascope.txt");
		zoneIdToTimelineFile.put(986L, "stigma_dreamscape.txt");
		zoneIdToTimelineFile.put(992L, "zodiark.txt");
		zoneIdToTimelineFile.put(993L, "zodiark-ex.txt");
		zoneIdToTimelineFile.put(995L, "hydaelyn.txt");
		zoneIdToTimelineFile.put(996L, "hydaelyn-ex.txt");
		zoneIdToTimelineFile.put(997L, "endsinger.txt");
		zoneIdToTimelineFile.put(1002L, "p1n.txt");
		zoneIdToTimelineFile.put(1003L, "p1s.txt");
		zoneIdToTimelineFile.put(1004L, "p2n.txt");
		zoneIdToTimelineFile.put(1005L, "p2s.txt");
		zoneIdToTimelineFile.put(1006L, "p3n.txt");
		zoneIdToTimelineFile.put(1007L, "p3s.txt");
		zoneIdToTimelineFile.put(1008L, "p4n.txt");
		zoneIdToTimelineFile.put(1009L, "p4s.txt");
	}

	private TimelineProcessor currentTimeline;
	private XivZone zone;

	public TimelineManager(PersistenceProvider pers) {
		rowsToDisplay = new IntSetting(pers, "timeline-overlay.max-displayed", 6, 1, 32);
		secondsPast = new IntSetting(pers, "timeline-overlay.seconds-past", 0, 0, null);
		secondsFuture = new IntSetting(pers, "timeline-overlay.seconds-future", 60, 0, null);
		debugMode = new BooleanSetting(pers, "timeline-overlay.debug-mode", false);
		prePullShow = new BooleanSetting(pers, "timeline-overlay.show-pre-pull", false);
		resetOnMapChange = new BooleanSetting(pers, "timeline-overlay.reset-on-map-change", false);
		this.pers = pers;
	}

	public @Nullable TimelineProcessor getTimeline(long zoneId) {
		String filename = zoneIdToTimelineFile.get(zoneId);
		if (filename == null) {
			log.info("No timeline found for zone {}", zoneId);
			return null;
		}
		InputStream resource = TimelineManager.class.getResourceAsStream("/timeline/" + filename);
		if (resource == null) {
			log.info("Timeline file '{}' for zone '{}' is missing", filename, zoneId);
			return null;
		}
		try {
			return TimelineProcessor.of(this, resource, getCustomEntries(zoneId));
		}
		catch (Throwable e) {
			log.error("Error loading timeline", e);
			return null;
		}

	}

	public List<? extends TimelineEntry> getCustomEntries(long zoneId) {
		return getCustomSettings(zoneId).getEntries();
	}

	private static String propStubForZoneId(long zoneId) {
		return String.format("timeline.custom.zone-%s.custom-entries", zoneId);
	}

	private final Map<Long, TimelineCustomizations> customizations = new ConcurrentHashMap<>();

	public TimelineCustomizations getCustomSettings(long zoneId) {
		return customizations.computeIfAbsent(zoneId, (k) -> pers.get(propStubForZoneId(k), TimelineCustomizations.class, new TimelineCustomizations()));
	}

	public void commitCustomSettings(long zoneId)  {
		TimelineCustomizations cust = customizations.get(zoneId);
		if (cust == null) {
			log.warn("Customization was not found for zone {}", zoneId);
		}
		else {
			pers.save(propStubForZoneId(zoneId), cust);
		}
		if (this.zone != null && this.zone.getId() == zoneId && currentTimeline != null) {
			// TODO: save/restore the timestamp so it is easier to live-edit
			TimelineProcessor.TimelineSync lastSync = currentTimeline.getLastSync();
			loadTimelineForCurrentZone();
			currentTimeline.setLastSync(lastSync);
		}
	}

	@HandleEvents
	public void changeZone(EventContext context, ZoneChangeEvent zoneChangeEvent) {
		XivZone zone = zoneChangeEvent.getZone();
		doZoneChange(zone);
	}

	@HandleEvents(order = 40_000)
	public void actLine(EventContext context, ACTLogLineEvent event) {
		TimelineProcessor currentTimeline = this.currentTimeline;
		if (currentTimeline != null) {
			currentTimeline.processActLine(event);
		}
	}

	@HandleEvents(order = 40_000)
	public void mapChange(EventContext context, MapChangeEvent event) {
		if (resetOnMapChange.get()) {
			resetCurrent();
		}
	}

	@HandleEvents(order = 40_000)
	public void newPull(EventContext context, PullStartedEvent event) {
		resetCurrent();
	}

	// TODO: "Pull Ended" event?
	@HandleEvents(order = 40_000)
	public void pullEnded(EventContext context, VictoryEvent event) {
		resetCurrent();
	}

	private void resetCurrent() {
		TimelineProcessor currentTimeline = this.currentTimeline;
		if (currentTimeline != null) {
			currentTimeline.reset();
		}
	}

	private void doZoneChange(XivZone zoneId) {
		this.zone = zoneId;
		loadTimelineForCurrentZone();
	}

	private void loadTimelineForCurrentZone() {
		if (zone == null) {
			log.info("Zone is null, doing nothing");
			return;
		}
		long zoneId = zone.getId();
		currentTimeline = getTimeline(zoneId);
		if (currentTimeline == null) {
			log.info("No timeline for zone '{}'", zoneId);
		}
		else {
			log.info("Loaded timeline for zone '{}', {} timeline entries", zoneId, currentTimeline.getEntries().size());
		}
	}

	public static Map<Long, String> getTimelines() {
		return Collections.unmodifiableMap(zoneIdToTimelineFile);
	}

	public List<VisualTimelineEntry> getCurrentDisplayEntries() {
		TimelineProcessor currentTimeline = this.currentTimeline;
		if (currentTimeline == null) {
			return Collections.emptyList();
		}
		return currentTimeline.getCurrentTimelineEntries();
	}

	public IntSetting getRowsToDisplay() {
		return rowsToDisplay;
	}

	public BooleanSetting getDebugMode() {
		return debugMode;
	}

	public IntSetting getSecondsFuture() {
		return secondsFuture;
	}

	public IntSetting getSecondsPast() {
		return secondsPast;
	}

	public BooleanSetting getPrePullSetting() {
		return prePullShow;
	}

	public BooleanSetting getResetOnMapChangeSetting() {
		return resetOnMapChange;
	}
}
