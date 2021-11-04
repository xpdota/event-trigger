package gg.xp.events;

public class XivAbility {
	private final int id;
	private final String name;

	public XivAbility(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
