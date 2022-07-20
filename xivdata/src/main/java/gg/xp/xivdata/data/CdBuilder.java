package gg.xp.xivdata.data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static gg.xp.xivdata.data.Job.AST;
import static gg.xp.xivdata.data.Job.BRD;
import static gg.xp.xivdata.data.Job.DNC;
import static gg.xp.xivdata.data.Job.DRG;
import static gg.xp.xivdata.data.Job.DRK;
import static gg.xp.xivdata.data.Job.GNB;
import static gg.xp.xivdata.data.Job.MCH;
import static gg.xp.xivdata.data.Job.MNK;
import static gg.xp.xivdata.data.Job.NIN;
import static gg.xp.xivdata.data.Job.PLD;
import static gg.xp.xivdata.data.Job.RDM;
import static gg.xp.xivdata.data.Job.RPR;
import static gg.xp.xivdata.data.Job.SAM;
import static gg.xp.xivdata.data.Job.SCH;
import static gg.xp.xivdata.data.Job.SGE;
import static gg.xp.xivdata.data.Job.SMN;
import static gg.xp.xivdata.data.Job.WAR;
import static gg.xp.xivdata.data.Job.WHM;
import static gg.xp.xivdata.data.JobType.CASTER;
import static gg.xp.xivdata.data.JobType.MELEE_DPS;
import static gg.xp.xivdata.data.JobType.TANK;

public class CdBuilder {
	// Required
	public JobType jobType;
	public Job job;
	public final CooldownType type;
	public final long[] abilityIds;
	public final boolean defaultPersOverlay;

	// Optional
	public long[] buffIds = {};
	public Integer maxCharges;
	public Double cooldown;
	public Double durationOverride;
	public String name;
	public boolean autoBuffs = true;

	public ExtendedCooldownDescriptor build() {
		return new CooldownDescriptorImpl(this);
	}

	public CdBuilder buffIds(long... buffIds) {
		this.buffIds = buffIds;
		this.autoBuffs = false;
		return this;
	}

	public CdBuilder noAutoBuffs() {
		this.autoBuffs = false;
		return this;
	}

	public CdBuilder maxCharges(int maxCharges) {
		this.maxCharges = maxCharges;
		return this;
	}

	public CdBuilder cooldown(double cd) {
		this.cooldown = cd;
		return this;
	}

	public CdBuilder duration(double durationOverride) {
		this.durationOverride = durationOverride;
		return this;
	}

	public CdBuilder name(String name) {
		this.name = name;
		return this;
	}

	// TODO: technically, Job/JobType is also in the CSV...
	public CdBuilder(CooldownType type, boolean defaultPersOverlay, long[] abilityIds) {
		this.type = type;
		this.defaultPersOverlay = defaultPersOverlay;
		this.abilityIds = abilityIds;
	}

	public ActionInfo getActionInfo() {
		return getActionInfo(abilityIds[0]);
	}

	public static ActionInfo getActionInfo(long id) {
		ActionInfo actionInfo = ActionLibrary.forId(id);
		if (actionInfo == null) {
			throw new RuntimeException(String.format("Could not find ActionInfo for action %X", id));
		}
		return actionInfo;
	}

	public List<ActionInfo> getAllActionInfo() {
		return Arrays.stream(abilityIds).mapToObj(CdBuilder::getActionInfo).toList();
	}

	public double getCooldown() {
		if (cooldown == null) {
			return getActionInfo().getCd();
		}
		else {
			return cooldown;
		}
	}

	public String getName() {
		if (name == null) {
			return getAllActionInfo().stream()
					.map(ActionInfo::name)
					.map(ActionUtils.adjustNameReverse())
					.collect(Collectors.joining("/"));
		}
		else {
			return name;
		}
	}

	public JobType getJobType() {
		if (jobType == null) {
			return switch (Integer.parseInt(getActionInfo().categoryRaw())) {
				case 113 -> TANK;
				// It's actually not but I don't have a category for DoW/DoM yet
				case 161 -> TANK;
				// This is actually DoM
				case 120 -> CASTER;
				case 114 -> MELEE_DPS;
				case 116 -> CASTER;
				default -> null;
			};
		}
		else {
			return jobType;
		}
	}

	public Job getJob() {
		if (job == null) {
			return switch (Integer.parseInt(getActionInfo().categoryRaw())) {
				case 20, 38 -> PLD;
				case 22, 44 -> WAR;
				case 98 -> DRK;
				case 149 -> GNB;
				case 25 -> WHM;
				case 29 -> SCH;
				case 99 -> AST;
				case 181 -> SGE;
				case 21 -> MNK;
				case 23 -> DRG;
				case 93 -> NIN;
				case 111 -> SAM;
				case 180 -> RPR;
				case 24 -> BRD;
				case 96 -> MCH;
				case 150 -> DNC;
				case 28 -> SMN;
				case 112 -> RDM;
				default -> null;
			};
		}
		else {
			return job;
		}
	}

	public int getMaxCharges() {
		if (maxCharges == null) {
			return getActionInfo().maxCharges();
		}
		else {
			return maxCharges;
		}
	}
}
