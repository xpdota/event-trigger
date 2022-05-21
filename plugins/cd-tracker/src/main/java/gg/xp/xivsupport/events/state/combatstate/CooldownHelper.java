package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.Cooldown;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.CdTrackingKey;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
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

	public List<CooldownStatus> getCooldowns(Predicate<XivCombatant> sourceFilter, Predicate<Cooldown> cdFilter) {
		Predicate<XivCombatant> actualSourceFilter = xc -> sourceFilter.test(xc.walkParentChain());
		Map<CdTrackingKey, AbilityUsedEvent> cds = cdTracker
				.getCds(e -> cdFilter.test(e.getKey().getCooldown()) && actualSourceFilter.test(e.getKey().getSource()));
		List<BuffApplied> buffs = buffTracker.getBuffsAndPreapps().stream()
				.filter(ba -> actualSourceFilter.test(ba.getSource()))
				.toList();
		return cds.entrySet()
				.stream().map(e -> {
					CdTrackingKey key = e.getKey();
					AbilityUsedEvent abilityUsed = e.getValue();
					Instant replenishedAt = cdTracker.getReplenishedAt(key);
					Cooldown cd = key.getCooldown();
					@Nullable BuffApplied buffApplied = buffs.stream()
							.filter(b -> cd.buffIdMatches(b.getBuff().getId()))
							.filter(b -> b.getSource().walkParentChain().equals(abilityUsed.getSource().walkParentChain()))
							.findFirst()
							.orElse(null);
					return new CooldownStatus(key, abilityUsed, buffApplied, replenishedAt);
				}).toList();
	}

	public @Nullable CooldownStatus getPersonalCd(Cooldown cd) {
		return getCooldowns(XivCombatant::isThePlayer, cd::equals).stream().findFirst().orElse(null);
	}
}
