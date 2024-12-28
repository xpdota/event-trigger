package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.StatusEffectList;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivStatusEffect;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class Line38Parser extends AbstractACTLineParser<Line38Parser.Fields> {

	private final StatusEffectRepository buffs;

	public Line38Parser(PicoContainer container, StatusEffectRepository buffs) {
		super(container, 38, Fields.class);
		this.buffs = buffs;
	}

	enum Fields {
		id, name, jobLevelData,
		targetCurHp, targetMaxHp, targetCurMp, targetMaxMp, targetShieldPct, targetUnknown2, targetX, targetY, targetZ, targetHeading,
		unknown1, unknown2, unknown3,
		firstFlag
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		fields.setTrustedHp(true);
		XivCombatant target = fields.getEntity(Fields.id, Fields.name, Fields.targetCurHp, Fields.targetMaxHp, Fields.targetCurMp, Fields.targetMaxMp, Fields.targetX, Fields.targetY, Fields.targetZ, Fields.targetHeading, Fields.targetShieldPct);
		// To save processing time, only bother with this if the target has no buffs whatsoever currently on them
		// TODO: is this the best way of doing this?
		if (!buffs.targetHasAnyStatus(target)) {
			List<String> split = fields.getRawLineSplit();
			// Last field is hash
			List<String> remaining = split.subList(Fields.firstFlag.ordinal() + 2, split.size() - 1);
			int count = remaining.size() / 3;
			List<BuffApplied> out = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				int index = i * 3;
				String part1 = remaining.get(index);
				String part2 = remaining.get(index + 1);
				String part3 = remaining.get(index + 2);
				// Ignore blank
				if (part1.isBlank()) {
					continue;
				}
				long part1full = Long.parseLong(part1, 16);
				long effectId = part1full & 0xffff;
				// Ignore empty
				if (effectId == 0 || effectId == 1) {
					continue;
				}
				long rawStacks = part1full >> 16;
				long durationAsInt = Long.parseLong(part2, 16);
				float duration;
				duration = Float.intBitsToFloat((int) durationAsInt);
				if (duration == 0) {
					duration = 9999.0f;
				}
				XivStatusEffect status = new XivStatusEffect(effectId);
				StatusEffectInfo info = status.getInfo();
				// Some buffs have the wrong duration
				if (duration < 9000 && info != null && info.isPermanent()) {
					duration = 9999;
				}
				XivCombatant source = fields.getEntity(Long.parseLong(part3, 16));
				BuffApplied fakeBa = new BuffApplied(status, duration, source, target, rawStacks);
				FakeTimeSource fts = this.fakeTimeSource;
				if (fts != null) {
					fakeBa.setTimeSource(fts);
				}
				out.add(fakeBa);
			}
			return new StatusEffectList(target, out);
		}
		return null;
	}
}
