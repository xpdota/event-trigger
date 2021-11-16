package gg.xp.xivsupport.events.actlines.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public enum Job {


	ADV(0, false, JobType.UNKNOWN, "Adventurer"),
	GLA(1, true, JobType.TANK, "Gladiator"),
	PGL(2, true, JobType.TANK, "Pugilist"),
	MRD(3, true, JobType.TANK, "Marauder"),
	LNC(4, true, JobType.MELEE_DPS, "Lancer"),
	ARC(5, true, JobType.PRANGED, "Archer"),
	CNJ(6, true, JobType.HEALER, "Conjurer"),
	THM(7, true, JobType.CASTER, "Thaumaturge"),
	CRP(8, false, JobType.DOH, "Carpenter"),
	BSM(9, false, JobType.DOH, "Blacksmith"),
	ARM(10, false, JobType.DOH, "Armorer"),
	GSM(11, false, JobType.DOH, "Goldsmith"),
	LTW(12, false, JobType.DOH, "Leatherworker"),
	WVR(13, false, JobType.DOH, "Weaver"),
	ALC(14, false, JobType.DOH, "Alchemist"),
	CUL(15, false, JobType.DOH, "Culinarian"),
	MIN(16, false, JobType.DOL, "Miner"),
	BTN(17, false, JobType.DOL, "Botanist"),
	FSH(18, false, JobType.DOL, "Fisher"),
	PLD(19, false, JobType.TANK, "Paladin"),
	MNK(20, false, JobType.MELEE_DPS, "Monk"),
	WAR(21, false, JobType.TANK, "Warrior"),
	DRG(22, false, JobType.MELEE_DPS, "Dragoon"),
	BRD(23, false, JobType.PRANGED, "Bard"),
	WHM(24, false, JobType.HEALER, "White Mage"),
	BLM(25, false, JobType.CASTER, "Black Mage"),
	ACN(26, true, JobType.CASTER, "Arcanist"),
	SMN(27, false, JobType.CASTER, "Summoner"),
	SCH(28, false, JobType.HEALER, "Scholar"),
	ROG(29, true, JobType.MELEE_DPS, "Rogue"),
	NIN(30, false, JobType.MELEE_DPS, "Ninja"),
	MCH(31, false, JobType.PRANGED, "Machinist"),
	DRK(32, false, JobType.TANK, "Dark Knight"),
	AST(33, false, JobType.HEALER, "Astrologian"),
	SAM(34, false, JobType.MELEE_DPS, "Samurai"),
	RDM(35, false, JobType.CASTER, "Red Mage"),
	BLU(36, false, JobType.CASTER, "Blue Mage"),
	GNB(37, false, JobType.TANK, "Gunbreaker"),
	DNC(38, false, JobType.PRANGED, "Dancer");

	private static final Logger log = LoggerFactory.getLogger(Job.class);

	private final int id;
	private final boolean isClassRatherThanJob;
	private final JobType category;
	private final String friendlyName;

	Job(int id, boolean isClassRatherThanJob, JobType category, String friendlyName) {
		this.id = id;
		this.isClassRatherThanJob = isClassRatherThanJob;
		this.category = category;
		this.friendlyName = friendlyName;
	}

	// TODO: only supports default party list sort - revisit later to support custom ordering

	private static final EnumMap<Job, Integer> sortOrder = new EnumMap<>(Job.class);
	private static final Job[] sortOrderArray = {
			PLD, GLA, WAR, MRD, DRK, GNB,
			WHM, CNJ, SCH, AST,
			MNK, PGL, DRG, LNC, NIN, ROG, SAM,
			BRD, ARC, MCH, DNC,
			BLM, THM, SMN, ACN, RDM, BLU
	};

	static {
		int count = 0;
		for (Job job : sortOrderArray) {
			if (!job.isCombatJob()) {
				log.error("Invalid party sort order: non-combat job {} appears in party list sort order", job);
			}
			sortOrder.put(job, count++);
		}
		List<Job> allJobs = Arrays.asList(values());
		Set<Job> allCombatJobs = allJobs.stream().filter(Job::isCombatJob).collect(Collectors.toSet());
		allCombatJobs.removeAll(sortOrder.keySet());
		if (!allCombatJobs.isEmpty()) {
			log.error("Invalid party sort order: combat job(s) [{}] do not appear in party list sort order", allCombatJobs);
		}

	}

	public int partySortOrder() {
		// Sort non-combat jobs to the end of the list, I guess?
		return sortOrder.getOrDefault(this, Integer.MAX_VALUE);
	}


	public static Job getById(long id) {
		// This will work as long as they stay in order and contiguous
		if (id > values().length) {
			log.error("There is no job with ID {}, using ADV instead", id);
			return ADV;
		}
		return values()[(int) id];
	}

	public int getId() {
		return id;
	}

	public String getFriendlyName() {
		return friendlyName;
	}

	public boolean isClassRatherThanJob() {
		return isClassRatherThanJob;
	}

	public boolean isCombatJob() {
		return category.isCombatJob();
	}

	public boolean isCrafter() {
		return category.isCrafter();
	}

	public boolean isGatherer() {
		return category.isGatherer();
	}

	public boolean isTank() {
		return category.isTank();
	}

	public boolean isHealer() {
		return category.isHealer();
	}

	public boolean isDps() {
		return category.isDps();
	}

	public boolean isCaster() {
		return category.isCaster();
	}

	public boolean isPranged() {
		return category.isPranged();
	}

	public boolean isMeleeDps() {
		return category.isMeleeDps();
	}

}
