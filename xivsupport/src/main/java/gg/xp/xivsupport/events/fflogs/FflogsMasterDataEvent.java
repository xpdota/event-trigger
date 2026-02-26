package gg.xp.xivsupport.events.fflogs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.BaseEvent;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class FflogsMasterDataEvent extends BaseEvent {

	@Serial
	private static final long serialVersionUID = 6406548116923334551L;
	private final List<Actor> actors;
	private final FflogsFightCombatantData combatantData;

	public record Actor(
			long gameID,
			long id,
			String name,
			@Nullable Long petOwner,
			String type,
			String subType
	) {
	}

	public record ActorWithCount(
			Actor actor,
			int count
	) {
	}

	public FflogsMasterDataEvent(List<Actor> actors, FflogsFightCombatantData combatantData) {
		this.actors = new ArrayList<>(actors);
		this.combatantData = combatantData;
	}

	public List<Actor> getActors() {
		return Collections.unmodifiableList(actors);
	}

	public FflogsFightCombatantData getCombatantData() {
		return combatantData;
	}

	public List<ActorWithCount> getFilteredActors() {
		// These map from an actor ID (not game ID) to
		Map<Long, Integer> relevantIds = new HashMap<>();
		Stream.concat(combatantData.friendlyPlayers().stream(), combatantData.enemyPlayers().stream())
						.forEach(n -> relevantIds.put(n, 1));
		Stream.concat(combatantData.friendlyNPCs().stream(), combatantData.enemyNPCs().stream()).forEach(n -> {
			relevantIds.put(n.id(), n.instanceCount());
		});
		Stream.concat(combatantData.friendlyPets().stream(), combatantData.enemyPets().stream()).forEach(n -> {
			relevantIds.put(n.id(), n.instanceCount());
		});
		return actors.stream().flatMap(a -> {
			Integer count = relevantIds.get(a.id());
			if (count == null) {
				return Stream.empty();
			}
			else {
				return Stream.of(new ActorWithCount(a, count));
			}
		}).toList();
	}


}
