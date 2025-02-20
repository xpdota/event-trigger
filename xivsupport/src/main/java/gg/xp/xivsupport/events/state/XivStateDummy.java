package gg.xp.xivsupport.events.state;

import gg.xp.xivdata.data.XivMap;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.ManaPoints;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class XivStateDummy implements XivState {

	private XivPlayerCharacter player;
	private XivZone zone;
	private XivMap map = XivMap.UNKNOWN;
	private List<XivPlayerCharacter> partyList = Collections.emptyList();
	private Map<Long, XivCombatant> combatants = new HashMap<>();

	private final PrimaryLogSource pls;

	public XivStateDummy(PrimaryLogSource pls) {
		this.pls = pls;
	}

	@Override
	public XivPlayerCharacter getPlayer() {
		return player;
	}

	public void setPlayer(XivPlayerCharacter player) {
		this.player = player;
	}

	@Override
	public XivZone getZone() {
		return zone;
	}

	public void setZone(XivZone zone) {
		this.zone = zone;
	}

	@Override
	public XivMap getMap() {
		return map;
	}

	public void setMap(XivMap map) {
		this.map = map;
	}

	@Override
	public List<XivPlayerCharacter> getPartyList() {
		return partyList;
	}

	public void setPartyList(List<XivPlayerCharacter> partyList) {
		this.partyList = partyList;
	}

	@Override
	public boolean zoneIs(long zoneId) {
		return zone != null && zone.getId() == zoneId;
	}

	@Override
	public void removeSpecificCombatant(long idToRemove) {
		combatants.remove(idToRemove);
	}

	@Override
	public Map<Long, XivCombatant> getCombatants() {
		return combatants;
	}

	public void setCombatants(Map<Long, XivCombatant> combatants) {
		this.combatants = combatants;
	}

	public void setCombatants(List<XivCombatant> combatants) {
		this.combatants = combatants.stream()
				.collect(Collectors.toMap(XivEntity::getId, Function.identity()));
	}

	@Override
	public List<XivCombatant> getCombatantsListCopy() {
		return new ArrayList<>(combatants.values());
	}

	@Override
	public int getPartySlotOf(XivEntity entity) {
		//noinspection SuspiciousMethodCalls
		return partyList.indexOf(entity);
	}

	@Override
	public void provideCombatantHP(XivCombatant target, @NotNull HitPoints hitPoints) {
		throw new UnsupportedOperationException("not supported");
	}

	@Override
	public void provideCombatantMP(XivCombatant target, @NotNull ManaPoints manaPoints) {
		throw new UnsupportedOperationException("not supported");
	}

	@Override
	public void provideCombatantPos(XivCombatant target, Position newPos, boolean trusted) {
		throw new UnsupportedOperationException("not supported");
	}

	@Override
	public void provideActFallbackCombatant(XivCombatant cbt) {
		throw new UnsupportedOperationException("not supported");
	}

	@Override
	public void provideTypeOverride(XivCombatant cbt, int type) {
		throw new UnsupportedOperationException("not supported");
	}

	@Override
	public void flushProvidedValues() {
		throw new UnsupportedOperationException("not supported");
	}

	// TODO
	@Override
	public boolean inCombat() {
		return true;
	}

	@Override
	public void provideCombatantShieldPct(XivCombatant cbt, long shieldPct) {
		throw new UnsupportedOperationException("not supported");
	}

	@Override
	public void provideCombatantRadius(XivCombatant cbt, float radius) {
		throw new UnsupportedOperationException("not supported");
	}

	@Override
	public void provideTransformation(long entityId, short transformationId) {
		throw new UnsupportedOperationException("not supported");
	}

	@Override
	public void provideWeaponId(XivCombatant existing, short weaponId) {
		throw new UnsupportedOperationException("not supported");
	}
}
