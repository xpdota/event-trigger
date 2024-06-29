package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.PlayerStats;
import gg.xp.xivsupport.events.actlines.events.PlayerStatsUpdatedEvent;
import gg.xp.xivsupport.gameversion.GameVersionController;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

import static gg.xp.xivsupport.events.actlines.parsers.Line12Parser.NEW_STATS_LINE_FORMAT_VERSION;

@SuppressWarnings("unused")
public class Line12ParserLegacy extends AbstractACTLineParser<Line12ParserLegacy.Fields> implements FilteredEventHandler {

	private final GameVersionController versionController;

	public Line12ParserLegacy(PicoContainer container) {
		super(container, 12, Fields.class);
		this.versionController = container.getComponent(GameVersionController.class);
	}

	@Override
	public boolean enabled(EventContext context) {
		return versionController.isOlderThan(NEW_STATS_LINE_FORMAT_VERSION);
	}

	enum Fields {
		jobId, strength, dexterity, vitality, intelligence, mind, piety, attackPower, directHit, criticalHit, attackMagicPotency, healMagicPotency, determination, skillSpeed, spellSpeed, unknown0, tenacity, localContentId
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		PlayerStats stats = new PlayerStats(
				Job.getById(fields.getInt(Fields.jobId)),
				fields.getInt(Fields.strength),
				fields.getInt(Fields.dexterity),
				fields.getInt(Fields.vitality),
				fields.getInt(Fields.intelligence),
				fields.getInt(Fields.mind),
				fields.getInt(Fields.piety),
				fields.getInt(Fields.attackPower),
				fields.getInt(Fields.directHit),
				fields.getInt(Fields.criticalHit),
				fields.getInt(Fields.attackMagicPotency),
				fields.getInt(Fields.healMagicPotency),
				fields.getInt(Fields.determination),
				fields.getInt(Fields.skillSpeed),
				fields.getInt(Fields.spellSpeed),
				fields.getInt(Fields.tenacity));
		return new PlayerStatsUpdatedEvent(stats);
	}

	@Override
	protected EntityLookupMissBehavior entityLookupMissBehavior() {
		return EntityLookupMissBehavior.IGNORE;
	}
}
