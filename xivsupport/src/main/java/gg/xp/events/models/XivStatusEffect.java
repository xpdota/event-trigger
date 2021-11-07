package gg.xp.events.models;

public class XivStatusEffect {
	private final long id;
	private final String name;

	public XivStatusEffect(long id, String name) {
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
		return String.format("XivStatusEffect(0x%X:%s)", id, name);
	}

}
