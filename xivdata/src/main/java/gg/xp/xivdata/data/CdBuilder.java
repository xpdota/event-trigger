package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
	public List<CdAuxAbility> auxAbilities = new ArrayList<>();

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

	public CdBuilder auxAbility(long abilityId, double durationModifier) {
		return auxAbility(new CdAuxAbility(abilityId, durationModifier));
	}

	public CdBuilder auxAbility(CdAuxAbility aux) {
		this.auxAbilities.add(aux);
		return this;
	}

	// TODO: technically, Job/JobType is also in the CSV...
	public CdBuilder(CooldownType type, boolean defaultPersOverlay, long[] abilityIds) {
		this.type = type;
		this.defaultPersOverlay = defaultPersOverlay;
		this.abilityIds = abilityIds;
	}

	public ActionInfo getActionInfoRequired() {
		if (abilityIds.length == 0) {
			throw new IllegalStateException("Cannot ask for action info when the cooldown has no ability IDs");
		}
		return getActionInfoRequired(abilityIds[0]);
	}

	public @Nullable ActionInfo getActionInfoOpt() {
		if (abilityIds.length == 0) {
			return null;
		}
		return getActionInfoOpt(abilityIds[0]);
	}

	public static @Nullable ActionInfo getActionInfoOpt(long id) {
		return ActionLibrary.forId(id);
	}

	public static ActionInfo getActionInfoRequired(long id) {
		ActionInfo actionInfo = getActionInfoOpt(id);
		if (actionInfo == null) {
			throw new RuntimeException(String.format("Could not find ActionInfo for action %X", id));
		}
		return actionInfo;
	}

	public List<ActionInfo> getAllActionInfo() {
		return Arrays.stream(abilityIds).mapToObj(CdBuilder::getActionInfoOpt).filter(Objects::nonNull).toList();
	}

	public double getCooldown() {
		if (cooldown == null) {
			return getActionInfoRequired().getCd();
		}
		else {
			return cooldown;
		}
	}

	public String getName() {
		if (name == null) {
			List<ActionInfo> all = getAllActionInfo();
			if (all.isEmpty()) {
				if (abilityIds.length == 0) {
					return "None";
				}
				return String.format("Unknown (0x%X, %s)", abilityIds[0], abilityIds[0]);
			}
			return all.stream()
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
			ActionInfo ai = getActionInfoOpt();
			if (ai == null) {
				return UNKNOWN;
			}
			return switch (Integer.parseInt(ai.categoryRaw())) {
				case 66, 105, 123, 171 -> PRANGED;
				case 67, 76, 84, 114, 148 -> MELEE_DPS;
				case 89, 116, 175, 198 -> CASTER;
				case 113, 121, 166 -> TANK;
				case 117, 125, 128, 165 -> HEALER;
				// It's actually not but I don't have a category for DoW/DoM yet
				case 120, 199 -> CASTER;
				// This is actually DoM, but no category for that yet
				case 161 -> TANK;
				default -> UNKNOWN;
			};
		}
		else {
			return jobType;
		}
	}

	// TODO: just pull this from data files
	public @Nullable Job getJob() {
		if (job == null) {
			ActionInfo ai = getActionInfoOpt();
			if (ai == null) {
				return null;
			}
			int rawJobCategory = Integer.parseInt(ai.categoryRaw());
			return switch (rawJobCategory) {
				case 2, 20, 38 -> PLD;
				case 3, 21, 41 -> MNK;
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
				case 196 -> VPR;
				case 197 -> PCT;
				default -> null;
			};
		}
		else {
			return job;
		}
	}

	public int getMaxCharges() {
		if (maxCharges == null) {
			ActionInfo ai = getActionInfoOpt();
			if (ai == null) {
				return 1;
			}
			return ai.maxCharges();
		}
		else {
			return maxCharges;
		}
	}

	public List<CdAuxAbility> getAuxAbilities() {
		return Collections.unmodifiableList(auxAbilities);
	}
}
