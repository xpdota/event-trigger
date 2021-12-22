package gg.xp.xivsupport.models;

import java.io.Serial;
import java.io.Serializable;

public class XivWorld implements Serializable {
	@Serial
	private static final long serialVersionUID = 754017335186075592L;
	private XivWorld() {}
	private XivWorld(int stuff) {}

	public static XivWorld of() {
		return INSTANCE;
	}

	public static XivWorld createXivWorld(int stuff) {
		return INSTANCE;
	}

	private static final XivWorld INSTANCE = new XivWorld();

	@Override
	public String toString() {
		return "TODO";
	}
}
