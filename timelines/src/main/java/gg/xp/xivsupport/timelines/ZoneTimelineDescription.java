package gg.xp.xivsupport.timelines;

import gg.xp.xivdata.data.*;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class ZoneTimelineDescription {
	private final long zoneId;
	private final @Nullable String filename;
	private final @Nullable String dutyName;

	public ZoneTimelineDescription(
			long zoneId,
			@Nullable String filename
	) {
		this.zoneId = zoneId;
		this.dutyName = ZoneLibrary.capitalizedNameForZone((int) zoneId);
		this.filename = filename;
	}

	public String getDescription() {
		if (filename == null) {
			if (dutyName == null) {
				return "zone %s".formatted(zoneId);
			}
			else {
				return "duty '%s' (zone %s)".formatted(dutyName, zoneId);
			}
		}
		else {
			if (dutyName == null) {
				return "timeline '%s' (zone %s)".formatted(filename, zoneId);
			}
			else {
				return "duty '%s' (timeline '%s', zone '%s')".formatted(dutyName, filename, zoneId);
			}
		}
	}

	public long getZoneId() {
		return zoneId;
	}

	public @Nullable String getFilename() {
		return filename;
	}

	public @Nullable String getDutyName() {
		return dutyName;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (ZoneTimelineDescription) obj;
		return this.zoneId == that.zoneId &&
		       Objects.equals(this.filename, that.filename) &&
		       Objects.equals(this.dutyName, that.dutyName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(zoneId, filename, dutyName);
	}

	@Override
	public String toString() {
		return "ZoneDescription[" +
		       "zoneId=" + zoneId + ", " +
		       "filename=" + filename + ", " +
		       "duty=" + dutyName + ']';
	}

}
