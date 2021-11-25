package gg.xp.xivsupport.events.misc.pulls;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.VictoryEvent;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivZone;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pull {

	private final int pullNum;
	private final Event start;
	private Event combatStart;
	private Event end;
	private final XivZone zone;
	private final Map<Long, XivPlayerCharacter> players = new HashMap<>(8);
	private final Map<Long, XivCombatant> enemies = new HashMap<>();
	// TODO: also hold events? Might be a creative way to prevent pruning on "current" events.

	public Pull(int pullNum, Event start, XivZone zone) {
		this.pullNum = pullNum;
		this.start = start;
		this.zone = zone;
	}

	public int getPullNum() {
		return pullNum;
	}

	public @NotNull Event getStart() {
		return start;
	}

	public @Nullable Event getCombatStart() {
		return combatStart;
	}

	void setCombatStart(@NotNull Event combatStart) {
		this.combatStart = combatStart;
	}

	public @Nullable Event getEnd() {
		return end;
	}

	void setEnd(@NotNull Event end) {
		this.end = end;
	}

	public @NotNull XivZone getZone() {
		return zone;
	}

	public @NotNull List<XivPlayerCharacter> getPlayers() {
		return new ArrayList<>(players.values());
	}

	public @NotNull List<XivCombatant> getEnemies() {
		return new ArrayList<>(enemies.values());
	}

	// TODO: is performance going to be okay?
	public void addPlayer(XivPlayerCharacter player) {
		players.put(player.getId(), player);
	}

	public void addEnemy(XivCombatant enemy) {
		enemies.put(enemy.getId(), enemy);
	}

	// TODO: clock skew from client to server might cause an issue here
	public @NotNull Instant startTime() {
		return start.getHappenedAt();
	}

	public @Nullable Instant combatStartTime() {
		return combatStart == null ? null : combatStart.getHappenedAt();
	}

	public @Nullable Instant endTime() {
		return end == null ? null : end.getHappenedAt();
	}

	public @NotNull Duration getDuration() {
		Instant start = startTime();
		Instant end = endTime();
		if (end == null) {
			end = Instant.now();
		}
		return Duration.between(start, end);
	}

	public @Nullable Duration getCombatDuration() {
		Instant start = combatStartTime();
		if (start == null) {
			return null;
		}
		Instant end = endTime();
		if (end == null) {
			end = Instant.now();
		}
		return Duration.between(start, end);
	}

	public PullStatus getStatus() {
		if (end instanceof VictoryEvent) {
			return PullStatus.VICTORY;
		}
		if (end instanceof ZoneChangeEvent) {
			return PullStatus.LEFT_ZONE;
		}
		if (end instanceof DebugCommand) {
			return PullStatus.MANUAL_END;
		}
		if (end != null) {
			return PullStatus.WIPED;
		}
		if (combatStart != null) {
			return PullStatus.COMBAT;
		}
		return PullStatus.PRE_PULL;
	}
}
