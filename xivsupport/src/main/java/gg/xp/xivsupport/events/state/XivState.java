package gg.xp.xivsupport.events.state;

import gg.xp.reevent.context.SubState;
import gg.xp.xivdata.data.Job;
import gg.xp.xivdata.data.XivMap;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivZone;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface XivState extends SubState {
	// Note: can be null until we have all the required data, but this should only happen very early on in init
	XivPlayerCharacter getPlayer();

	// Note: can be null until we've seen a 01-line
	XivZone getZone();

	XivMap getMap();

	List<XivPlayerCharacter> getPartyList();

	boolean zoneIs(long zoneId);

	void removeSpecificCombatant(long idToRemove);

	Map<Long, XivCombatant> getCombatants();

	// TODO: does this still need to be a copy?
	List<XivCombatant> getCombatantsListCopy();

	int getPartySlotOf(XivEntity entity);

	void provideCombatantHP(XivCombatant target, @NotNull HitPoints hitPoints);

	void provideCombatantPos(XivCombatant target, Position newPos);

	void flushProvidedValues();

	@Nullable XivCombatant getDeadCombatant(long id);

	default @Nullable Job getPlayerJob() {
		XivPlayerCharacter player = getPlayer();
		if (player == null) {
			return null;
		}
		else {
			return player.getJob();
		}
	}

	boolean inCombat();

}
