package gg.xp.xivsupport.callouts;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.callouts.conversions.GlobalArenaSectorConverter;
import gg.xp.xivsupport.callouts.conversions.PlayerNameConversion;
import gg.xp.xivsupport.events.actlines.events.NameIdPair;
import gg.xp.xivsupport.gui.util.HasFriendlyName;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.EnumSetting;
import gg.xp.xivsupport.persistence.settings.StringSetting;

import java.time.Duration;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ScanMe
public class SingleValueReplacement {
	private final BooleanSetting replaceYou;
	private final StringSetting replacementForYou;
	private final BooleanSetting appendYou;
	private final EnumSetting<PlayerNameConversion> pcNameStyle;
	private final GlobalArenaSectorConverter asc;

	public SingleValueReplacement(PersistenceProvider pers, GlobalArenaSectorConverter asc) {
		this.replaceYou = new BooleanSetting(pers, "callout-processor.replace-you", true);
		this.replacementForYou = new StringSetting(pers, "callout-process.you-replacement", "YOU");
		this.appendYou = new BooleanSetting(pers, "callout-process.append-you", false);
		appendYou.addListener(() -> {
			if (appendYou.get()) {
				replaceYou.set(false);
			}
		});
		replaceYou.addListener(() -> {
			if (replaceYou.get()) {
				appendYou.set(false);
			}
		});
		this.pcNameStyle = new EnumSetting<>(pers, "callout-processor.pc-style", PlayerNameConversion.class, PlayerNameConversion.FULL_NAME);
		this.asc = asc;
	}

	// Default conversions
	@SuppressWarnings("unused")
	public String singleReplacement(Object rawValue) {
		if (rawValue == null) {
			return "null";
		}
		if (rawValue instanceof String strVal) {
			return strVal;
		}
		else if (rawValue instanceof XivCombatant cbt) {
			if (rawValue instanceof XivPlayerCharacter xpc) {
				if (cbt.isThePlayer()) {
					if (replaceYou.get()) {
						return replacementForYou.get();
					}
					else if (appendYou.get()) {
						return pcNameStyle.get().convert(xpc) + ' ' + replacementForYou.get();
					}
				}
				return pcNameStyle.get().convert(xpc);
			}
			else {
				return cbt.getName();
			}
		}
		else if (rawValue instanceof NameIdPair pair) {
			return pair.getName();
		}
		else if (rawValue instanceof Duration dur) {
			if (dur.isZero()) {
				return "NOW";
			}
			return String.format("%.01f", dur.toMillis() / 1000.0);
		}
		else if (rawValue instanceof Supplier supp) {
			Object realValue = supp.get();
			// Prevent infinite loops if a supplier produces another supplier
			if (realValue instanceof Supplier) {
				return realValue.toString();
			}
			else {
				return singleReplacement(realValue);
			}
		}
		else if (rawValue instanceof ArenaSector as) {
			return asc.convert(as);
		}
		else if (rawValue instanceof HasFriendlyName hfn) {
			return hfn.getFriendlyName();
		}
		else if (rawValue instanceof Collection<?> coll) {
			return coll.stream()
					.map(this::singleReplacement)
					.collect(Collectors.joining(", "));
		}
		else {
			return rawValue.toString();
		}
	}

	public BooleanSetting getReplaceYou() {
		return replaceYou;
	}

	public EnumSetting<PlayerNameConversion> getPcNameStyle() {
		return pcNameStyle;
	}

	public StringSetting getReplacementForYou() {
		return replacementForYou;
	}

	public BooleanSetting getAppendYou() {
		return appendYou;
	}
}
