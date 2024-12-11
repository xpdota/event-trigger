package gg.xp.xivsupport.events.fflogs;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record FflogsReportLocator(String report, @Nullable Integer fight) {

	private static final Logger log = LoggerFactory.getLogger(FflogsReportLocator.class);
	private static final Pattern urlPattern = Pattern.compile("reports/([^#/?&]+).*fight=(\\d+|last)");
	private static final Pattern urlPatternNoFight = Pattern.compile("reports/([^#/]+)");

	public static FflogsReportLocator fromURL(String url) {
		Matcher matcher = urlPattern.matcher(url);
		Matcher noFightMatcher;
		if (matcher.find()) {
			String report = matcher.group(1);
			String fightRaw = matcher.group(2);
			int fight = fightRaw.equals("last") ? -1 : Integer.parseInt(fightRaw);
			log.info("Parsed fflogs URL: report {}, fight {}, url '{}'", report, fight, url);
			return new FflogsReportLocator(report, fight);
		}
		else if ((noFightMatcher = urlPatternNoFight.matcher(url)).find()) {
			String report = noFightMatcher.group(1);
			log.info("Parsed fflogs URL: report {}, no fight, url '{}'", report, url);
			return new FflogsReportLocator(report, null);
		}
		else {
			log.warn("Failed to parse fflogs URL '{}'", url);
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
