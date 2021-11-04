package gg.xp.events;

public class XivEntity {

	private final int id;
	private final String name;

	public XivEntity(int id, String name) {
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
