package gg.xp.xivsupport.events.fflogs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record FflogsReportLocator(String report, int fight) {

	private static final Pattern urlPattern = Pattern.compile("reports\\/([^#]+).*fight=(\\d+|last)");

	public static FflogsReportLocator fromURL(String url) {
		Matcher matcher = urlPattern.matcher(url);
		if (matcher.find()) {
			String report = matcher.group(1);
			String fightRaw = matcher.group(2);
			int fight = fightRaw.equals("last") ? -1 : Integer.parseInt(fightRaw);
			if (fight == -1) {
				throw new IllegalArgumentException("You must specify fight number - 'last' is not yet supported");
			}
			return new FflogsReportLocator(report, fight);
		}
		else {
			throw new IllegalArgumentException("This doesn't look like a valid fflogs fight URL: " + url);
		}
	}
}
