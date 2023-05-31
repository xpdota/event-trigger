package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.Cooldown;
import gg.xp.xivdata.data.ExtendedCooldownDescriptor;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.StatusAppliedEffect;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.CdTrackingKey;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ScanMe
public class CooldownHelper {

	private final StatusEffectRepository buffTracker;
	private final CdTracker cdTracker;
	private final XivState state;


	public CooldownHelper(StatusEffectRepository buffTracker, CdTracker cdTracker, XivState state) {
		this.buffTracker = buffTracker;
		this.cdTracker = cdTracker;
		this.state = state;
	}

	public List<CooldownStatus> getCooldowns(Predicate<XivCombatant> sourceFilter, Predicate<ExtendedCooldownDescriptor> cdFilter) {
		Predicate<XivCombatant> actualSourceFilter = xc -> sourceFilter.test(xc.walkParentChain());
		Map<CdTrackingKey, AbilityUsedEvent> cds = cdTracker
				.getCds(e -> cdFilter.test(e.getKey().getCooldown()) && actualSourceFilter.test(e.getKey().getSource()));
		List<BuffApplied> buffs = buffTracker.getBuffsAndPreapps().stream()
				.filter(ba -> actualSourceFilter.test(ba.getSource()))
				// Filter out pets
				.filter(ba -> ba.getTarget().getType() != CombatantType.PET)
				.toList();
		return cds.entrySet()
				.stream().map(e -> {
					CdTrackingKey key = e.getKey();
					// The ability
					AbilityUsedEvent abilityUsed = e.getValue();
					// The timer
					Instant replenishedAt = cdTracker.getReplenishedAt(key);
					// The cooldown definition
					ExtendedCooldownDescriptor cd = key.getCooldown();
					// Establish a filter for what buffs to look at
					Predicate<BuffApplied> buffFilter;
					if (cd.autoBuffs()) {
						// For auto buffs - look at what buffs the ability actually applied
						Set<Long> buffIds = abilityUsed.getEffects().stream().map(effect -> {
							if (effect instanceof StatusAppliedEffect sae) {
								return sae.getStatus().getId();
							}
							return null;
						}).filter(Objects::nonNull).collect(Collectors.toSet());
						buffFilter = b -> buffIds.contains(b.getBuff().getId());
					}
					else {
						// For manual buffs - check against the cooldown defs
						buffFilter = b -> cd.buffIdMatches(b.getBuff().getId());
					}
					// Logic for showing buffs that missed you
					/*
						There are several kinds of abilities:
						1. Normal raid buffs - hits everyone (ideally)
							For these, we want to consider missing the player as a "miss"
						2. BRD-style buffs - the buff on BRD itself is NOT the same as on another player
						3. Statuses that go on an enemy (reprisal, chain, etc)

						Unfortunately, #2 will be a PITA for the time being and will probably require some rework.
						However, #1 can be semi-supported now by simply sorting the player's own buff in front.
					 */
					// Further filter buffs
					@Nullable BuffApplied buffApplied = buffs.stream()
							.filter(buffFilter)
							.filter(b -> b.getSource().walkParentChain().equals(abilityUsed.getSource().walkParentChain()))
							// Prioritize whatever is on the player
							.max(Comparator.<BuffApplied, Integer>comparing(ba -> {
										return ba.getTarget().isThePlayer() ? 1 : 0;
									})
									.thenComparing(BuffApplied::getEffectiveHappenedAt))
							.orElse(null);
					return new CooldownStatus(key, abilityUsed, buffApplied, replenishedAt);
				}).toList();
	}

	public @Nullable CooldownStatus getPersonalCd(Cooldown cd) {
		return getCooldowns(XivCombatant::isThePlayer, cd::equals).stream().findFirst().orElse(null);
	}

	public @Nullable CooldownStatus getCdStatusForPlayer(XivPlayerCharacter player, ExtendedCooldownDescriptor ecd) {
		return getCooldowns(source -> source.equals(player), cd -> cd.equals(ecd))
				.stream()
				.findFirst()
				.orElse(null);
	}
}
