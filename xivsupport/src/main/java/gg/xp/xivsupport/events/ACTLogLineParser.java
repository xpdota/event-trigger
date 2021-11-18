package gg.xp.xivsupport.events;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventHandler;

import java.util.regex.Pattern;

public class ACTLogLineParser implements EventHandler<ACTLogLineEvent> {

	/*
	Example:
	21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|106D41EA|Some Player|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|cd69a51d5f584b836fa20c4a5b356612
	 */
	private static final Pattern LINE21_PATTERN = Pattern.compile("^21\\|(?<ts>[^|]*)\\|(?<casterId>[^|]*)\\|(?<casterName>[^|]*)\\|(?<abilityId>[^|]*)\\|(?<abilityName>[^|]*)\\|(?<targetId>[^|]*)\\|(?<targetName>[^|]*)\\|");

	@Override
//	@HandleEvents
	public void handle(EventContext context, ACTLogLineEvent event) {
//		String logLine = event.getLogLine();
//		// This could obviously be cleaned up when we support more events
//		Matcher matcher21 = LINE21_PATTERN.matcher(logLine);
//		if (matcher21.find()) {
//			context.accept(
//					new AbilityUsedEvent(
//							new XivAbility(Integer.parseInt(matcher21.group("abilityId"), 16), matcher21.group("abilityName")),
//							new XivEntity(Integer.parseInt(matcher21.group("casterId"), 16), matcher21.group("casterName")),
//							new XivEntity(Integer.parseInt(matcher21.group("targetId"), 16), matcher21.group("targetName")),
//							fields.getHex(Line21Parser.Fields.flags), fields.getLong(Line21Parser.Fields.damage)));
//		}
	}
}
