package gg.xp.events.models;

public class XivAbility {
	private final long id;
	private final String name;

	public XivAbility(long id, String name) {
		this.id = id;
		this.name = name;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return String.format("XivAbility(0x%X:%s)", id, name);
	}

}
