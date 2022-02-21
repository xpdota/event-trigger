package gg.xp.xivdata.data;

// TODO: cooldown and stuff? What else would we want here?
public record ActionInfo(
		long actionid,
		String name,
		long iconId
) {
	public ActionIcon getIcon() {
		return ActionLibrary.iconForInfo(this);
	}
}
