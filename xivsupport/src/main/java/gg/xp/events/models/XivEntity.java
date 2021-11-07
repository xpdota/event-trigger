package gg.xp.events.models;

public class XivEntity {

	// IMPORTANT: Annoyingly, these all must be 'long' instead of 'int' because the game treats them as
	// unsigned 32-bit, but Java treats them as signed, so values above 7FFFFFFF cause an overflow
	private final long id;
	private final String name;

	public XivEntity(long id, String name) {
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
		if (isEnvironment()) {
			return "XivEntity(ENVIRONMENT)";
		}
		return String.format("XivEntity(0x%X:%s)", id, name);
	}

	public boolean isEnvironment() {
		return id == 0xE0000000L;
	}
}
