package gg.xp.xivdata.data;

import gg.xp.xivdata.data.rsv.*;

import java.io.Serializable;

public record NpcYellInfo(int id, String text) implements Serializable {
	@Override
	public String text() {
		return DefaultRsvLibrary.tryResolve(text);
	}
}
