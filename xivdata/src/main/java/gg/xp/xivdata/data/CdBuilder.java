package gg.xp.xivdata.data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static gg.xp.xivdata.data.Job.*;
import static gg.xp.xivdata.data.JobType.*;

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
				case 66, 105, 123, 171 -> PRANGED;
				case 67, 76, 84, 114, 148 -> MELEE_DPS;
				case 89, 116, 175 -> CASTER;
				case 113, 121, 166 -> TANK;
				case 117, 125, 128, 165 -> HEALER;
				// It's actually not but I don't have a category for DoW/DoM yet
				case 120 -> CASTER;
				// This is actually DoM, but no category for that yet
				case 161 -> TANK;
				default -> null;
			};
		}
		else {
			return jobType;
		}
	}

	// TODO: just pull this from data files
	public Job getJob() {
		if (job == null) {
			int rawJobCategory = Integer.parseInt(getActionInfo().categoryRaw());
			return switch (rawJobCategory) {
				case 2, 20, 38 -> PLD;
				case 3, 21 -> MNK;
				case 4, 22, 44 -> WAR;
				case 5, 23, 47 -> DRG;
				case 6, 24, 50 -> BRD;
				case 7, 25, 53 -> WHM;
				case 8, 26, 55 -> BLM;
				case 9 -> CRP;
				case 10 -> BSM;
				case 11 -> ARM;
				case 12 -> GSM;
				case 13 -> LTW;
				case 14 -> WVR;
				case 15 -> ALC;
				case 16 -> CUL;
				case 17 -> MIN;
				case 18 -> BTN;
				case 19 -> FSH;
				case 27 -> ACN;
				case 28, 68, 69 -> SMN;
				case 29 -> SCH;
				case 91, 92, 93, 103 -> NIN;
				case 96 -> MCH;
				case 98 -> DRK;
				case 99 -> AST;
				case 111 -> SAM;
				case 112 -> RDM;
				case 129 -> BLU;
				case 149 -> GNB;
				case 150 -> DNC;
				case 180 -> RPR;
				case 181 -> SGE;
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
