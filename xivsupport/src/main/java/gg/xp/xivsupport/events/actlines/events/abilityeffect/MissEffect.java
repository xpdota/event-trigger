package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class MissEffect extends AbilityEffect {

	public MissEffect() {
		super(AbilityEffectType.MISS);
	}

	@Override
	public String toString() {
		return "Miss";
	}
}
