package gg.xp.xivdata.data;

import gg.xp.xivdata.data.rsv.*;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;

// TODO: cooldown and stuff? What else would we want here?
public record ActionInfo(
		long actionid,
		String name,
		long iconId,
		long cdRaw,
		int maxCharges,
		String categoryRaw,
		boolean isPlayerAbility,
		long castTimeRaw) {
	public @Nullable ActionIcon getIcon() {
		return ActionLibrary.iconForInfo(this);
	}

	public URL getXivapiUrl() {
		long number = iconId;
		long stub = (number / 1000) * 1000;
		String xivapiUrl = String.format("https://xivapi.com/i/%06d/%06d_hr1.png", stub, number);
		try {
			return new URL(xivapiUrl);
		}
		catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String name() {
		return DefaultRsvLibrary.tryResolve(name);
	}

	public double getCd() {
		return cdRaw / 10.0;
	}

	public double getCastTime() {
		return castTimeRaw / 10.0;
	}
}
