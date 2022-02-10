package gg.xp.xivsupport.events.triggers.duties.timelines;

import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BasicCustomSettingDefaultNull;
import gg.xp.xivsupport.persistence.settings.RegexSetting;
import gg.xp.xivsupport.persistence.settings.StringSetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class CustomTimelineEntryOld {
//
//	private final StringSetting name;
//	private final BasicCustomSettingDefaultNull<Double> duration;
//	private final BasicCustomSettingDefaultNull<Double> jump;
//	private final BasicCustomSettingDefaultNull<Double> time;
//	private final BasicCustomSettingDefaultNull<Double> windowStart;
//	private final BasicCustomSettingDefaultNull<Double> windowEnd;
//	private final RegexSetting sync;
//
//	public CustomTimelineEntryOld(PersistenceProvider pers, long zoneId, int uniqueId) {
//		name = new StringSetting(pers, makeKey(zoneId, uniqueId, "name"), "Name Goes Here");
//		duration = new BasicCustomSettingDefaultNull<>(Double.class, pers, makeKey(zoneId, uniqueId, "duration"));
//		jump = new BasicCustomSettingDefaultNull<>(Double.class, pers, makeKey(zoneId, uniqueId, "jump"));
//		time = new BasicCustomSettingDefaultNull<>(Double.class, pers, makeKey(zoneId, uniqueId, "time"));
//		windowStart = new BasicCustomSettingDefaultNull<>(Double.class, pers, makeKey(zoneId, uniqueId, "windowStart"));
//		windowEnd = new BasicCustomSettingDefaultNull<>(Double.class, pers, makeKey(zoneId, uniqueId, "windowEnd"));
//		sync = new BasicCustomSettingDefaultNull<>(Pattern.class, pers, makeKey(zoneId, uniqueId, "sync"));
//	}
//
//	private static final String makeKey(long zoneId, int uniqueId, String name) {
//		return String.format("timeline.custom.%s.%s.%s", zoneId, uniqueId, name);
//	}
//
//	@Override
//	public double time() {
//		return time.get();
//	}
//
//	@Override
//	public @Nullable String name() {
//		return name.get();
//	}
//
//	@Override
//	public @Nullable Pattern sync() {
//		return sync.get();
//	}
//
//	@Override
//	public @Nullable Double duration() {
//		return duration.get();
//	}
//
//	@Override
//	public @NotNull TimelineWindow timelineWindow() {
//		return new TimelineWindow(windowStart.get(), windowEnd.get());
//	}
//
//	@Override
//	public @Nullable Double jump() {
//		return jump.get();
//	}
}
