package gg.xp.xivdata.data;

public class CombatJobSelection extends JobSelection {

	@Override
	public boolean isTypeAllowed(JobType type) {
		return type.isCombatJob();
	}

	public static CombatJobSelection all() {
		CombatJobSelection js = new CombatJobSelection();
		js.setEnabledForAll(true);
		return js;
	}

	public static CombatJobSelection none() {
		return new CombatJobSelection();
	}

	@Override
	public String toString() {
		return super.toString();
	}
}
