package gg.xp.xivsupport.models;

import gg.xp.xivdata.data.*;

import java.io.Serial;
import java.io.Serializable;

public final class XivWorld implements Serializable {
	@Serial
	private static final long serialVersionUID = 754017335186075592L;

	private final int id;

	private XivWorld(int id) {
		this.id = id;
	}

	/**
	 * Deprecated - if you actually want the default "unknown" world, then use {@link #unknown()}
	 *
	 * @return The default world instance.
	 */
	@Deprecated
	public static XivWorld of() {
		return INSTANCE;
	}

	public static XivWorld unknown() {
		return INSTANCE;
	}

	public static XivWorld forId(int id) {
		return new XivWorld(id);
	}

	public WorldInfo getWorldInfo() {
		return WorldDcLibrary.getWorld(id);
	}

	public DcInfo getDcInfo() {
		return WorldDcLibrary.getDc(getWorldInfo().dcId());
	}

	private static final XivWorld INSTANCE = new XivWorld(0);

	@Override
	public String toString() {
		return getWorldInfo().name();
	}
}
