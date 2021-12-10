package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class FullyResistedEffect extends AbilityEffect {

	public FullyResistedEffect() {
		super(AbilityEffectType.MISS);
	}

	@Override
	public String toString() {
		return "Fully Resisted";
	}
}
