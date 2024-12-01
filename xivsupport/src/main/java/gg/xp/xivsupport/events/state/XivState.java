package gg.xp.xivsupport.events.state;

import gg.xp.reevent.context.SubState;
import gg.xp.reevent.scan.Alias;
import gg.xp.xivdata.data.Job;
import gg.xp.xivdata.data.XivMap;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.ManaPoints;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivZone;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Alias("xivState")
@Alias("state")
public interface XivState extends SubState {
	// Note: can be null until we have all the required data, but this should only happen very early on in init
	XivPlayerCharacter getPlayer();

	// Note: can be null until we've seen a 01-line
	@Nullable XivZone getZone();

	@Nullable XivMap getMap();

	List<XivPlayerCharacter> getPartyList();

	boolean zoneIs(long zoneId);

	default boolean dutyIs(Duty duty) {
		Long expected = duty.getZoneId();
		return expected != null && zoneIs(expected);
	}

	void removeSpecificCombatant(long idToRemove);

	Map<Long, XivCombatant> getCombatants();

	default @Nullable XivCombatant getCombatant(long id) {
		return getCombatants().get(id);
	}

	default @NotNull XivCombatant getLatestCombatantData(@NotNull XivCombatant cbt) {
		XivCombatant result = getCombatant(cbt.getId());
		// If we no longer know if this combatant, avoid nullity issues by just returning the original data.
		if (result == null) {
			return cbt;
		}
		return result;
	}

	// TODO: does this still need to be a copy?
	List<XivCombatant> getCombatantsListCopy();

	/**
	 * Returns the party slot of the given entity
	 *
	 * @param entity The entity
	 * @return 0-7 based on their party slot, or -1 if they are not in the party
	 */
	int getPartySlotOf(XivEntity entity);

	void provideCombatantHP(XivCombatant target, @NotNull HitPoints hitPoints);

	void provideCombatantMP(XivCombatant target, @NotNull ManaPoints manaPoints);

	void provideCombatantPos(XivCombatant target, Position newPos);

	void provideActFallbackCombatant(XivCombatant cbt);

	void provideTypeOverride(XivCombatant cbt, int type);

	void flushProvidedValues();

	default @Nullable Job getPlayerJob() {
		XivPlayerCharacter player = getPlayer();
		if (player == null) {
			return null;
		}
		else {
			return player.getJob();
		}
	}

	default boolean playerJobMatches(Predicate<Job> condition) {
		Job job = getPlayerJob();
		if (job == null) {
			return false;
		}
		return condition.test(job);
	}

	boolean inCombat();

	void provideCombatantShieldPct(XivCombatant cbt, long shieldPct);

	void provideCombatantRadius(XivCombatant cbt, float radius);

	default @Nullable XivCombatant npcById(long id) {
		return getCombatantsListCopy()
				.stream()
				.filter(cbt -> cbt.npcIdMatches(id))
				.min(Comparator.comparing(XivCombatant::getId))
				.orElse(null);
	}

	default List<XivCombatant> npcsById(long id) {
		return getCombatantsListCopy()
				.stream()
				.filter(cbt -> cbt.npcIdMatches(id))
				.sorted(Comparator.comparing(XivCombatant::getId))
				.toList();
	}

	void provideTransformation(long entityId, short transformationId);

	void provideWeaponId(XivCombatant existing, short weaponId);
}
