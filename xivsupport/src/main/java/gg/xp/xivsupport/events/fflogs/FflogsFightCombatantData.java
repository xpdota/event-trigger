package gg.xp.xivsupport.events.fflogs;

import java.util.List;

// TODO: these should default to an empty list
public record FflogsFightCombatantData(
	List<Long> friendlyPlayers,
	List<Long> enemyPlayers,
	List<FflogsPetData> friendlyPets,
	List<FflogsPetData> enemyPets,
	List<FflogsNpcData> friendlyNPCs,
	List<FflogsNpcData> enemyNPCs
) {
}
