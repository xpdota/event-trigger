package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.gui.util.HasFriendlyName;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.XivCombatant;

import java.util.function.Predicate;

public enum EntityType implements HasFriendlyName, Predicate<XivCombatant> {
	ANY("Anything", unused -> true),
	THE_PLAYER("The Player", XivCombatant::isThePlayer),
	ANY_PLAYER("Any Player", XivCombatant::isPc),
	NPC("NPC (Any)", cbt -> !cbt.isPc()),
	NPC_REAL("NPC (Real)", cbt -> cbt.getType() == CombatantType.NPC),
	NPC_FAKE("NPC (Fake)", cbt -> cbt.getType() == CombatantType.FAKE),
	PET("Pet", cbt -> cbt.getType() == CombatantType.PET),
	MY_PET("My Pet", cbt -> cbt.getType() == CombatantType.PET && cbt.walkParentChain().isPc()),
	ENVIRONMENT("Environment", XivCombatant::isEnvironment);


	private final String label;
	private final Predicate<XivCombatant> predicate;

	EntityType(String label, Predicate<XivCombatant> predicate) {
		this.label = label;
		this.predicate = predicate;
	}


	@Override
	public String getFriendlyName() {
		return label;
	}

	@Override
	public boolean test(XivCombatant combatant) {
		return predicate.test(combatant);
	}
}
