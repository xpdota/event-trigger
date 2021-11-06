package gg.xp.events;

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
}
