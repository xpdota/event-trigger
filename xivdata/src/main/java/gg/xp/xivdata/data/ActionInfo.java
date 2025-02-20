package gg.xp.xivdata.data;

import gg.xp.xivdata.data.rsv.*;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public record ActionInfo(
		long actionid,
		String name,
		long iconId,
		long cdRaw,
		int maxCharges,
		int categoryRaw,
		boolean isPlayerAbility,
		long castTimeRaw,
		int castType,
		int effectRange,
		int xAxisModifier,
		int coneAngle,
		boolean isConeAngleKnown,
		String description
		) implements Serializable {
	public @Nullable ActionIcon getIcon() {
		return ActionLibrary.iconForInfo(this);
	}

	public URL getXivapiUrl() {
		long number = iconId;
		long stub = (number / 1000) * 1000;
		// Example: https://beta.xivapi.com/api/1/asset/ui/icon/218000/218443.tex?format=png
		String xivapiUrl = String.format("https://beta.xivapi.com/api/1/asset/ui/icon/%s/%s.tex?format=png", stub, number);
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
