package gg.xp.xivsupport.events.fflogs;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record FflogsReportLocator(String report, @Nullable Integer fight) {

	private static final Pattern urlPattern = Pattern.compile("reports\\/([^#]+).*fight=(\\d+|last)");
	private static final Pattern urlPatternNoFight = Pattern.compile("reports\\/([^#]+)");

	public static FflogsReportLocator fromURL(String url) {
		Matcher matcher = urlPattern.matcher(url);
		Matcher noFightMatcher;
		if (matcher.find()) {
			String report = matcher.group(1);
			String fightRaw = matcher.group(2);
			int fight = fightRaw.equals("last") ? -1 : Integer.parseInt(fightRaw);
			return new FflogsReportLocator(report, fight);
		}
		else if ((noFightMatcher = urlPatternNoFight.matcher(url)).find()) {
			String report = noFightMatcher.group(1);
			return new FflogsReportLocator(report, null);
		}
		else {
			throw new IllegalArgumentException("This doesn't look like a valid fflogs fight URL: " + url);
		}
	}

	public boolean fightSpecified() {
		return fight != null;
	}

	public FflogsReportLocator withFight(int fight) {
		return new FflogsReportLocator(report, fight);
	}
}
