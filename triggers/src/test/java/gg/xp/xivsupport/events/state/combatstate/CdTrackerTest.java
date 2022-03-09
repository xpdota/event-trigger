package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.Event;
import gg.xp.xivdata.data.Cooldown;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.TestEventContext;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.StatusAppliedEffect;
import gg.xp.xivsupport.events.state.XivStateDummy;
import gg.xp.xivsupport.models.CdTrackingKey;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.ManaPoints;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivWorld;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CdTrackerTest {

	Cooldown cd = Cooldown.Reprisal;
	private final XivPlayerCharacter player = new XivPlayerCharacter(0x10000001, "Me, The Player", Job.GNB, XivWorld.of(), true, 1, new HitPoints(123, 123), ManaPoints.of(123, 123), new Position(0, 0, 0, 0), 0, 0, 1, 80, 0);
	private final XivPlayerCharacter otherCharInParty = new XivPlayerCharacter(0x10000002, "Someone Else In My Party", Job.GNB, XivWorld.of(), false, 1, new HitPoints(123, 123), ManaPoints.of(123, 123), new Position(0, 0, 0, 0), 0, 0, 1, 80, 0);
	private final XivPlayerCharacter otherCharNotInParty = new XivPlayerCharacter(0x10000003, "Someone Else Not In Party", Job.GNB, XivWorld.of(), false, 1, new HitPoints(123, 123), ManaPoints.of(123, 123), new Position(0, 0, 0, 0), 0, 0, 0, 80, 0);
	private final XivCombatant theBoss = new XivCombatant(0x40000001, "The Boss", false, false, 2, new HitPoints(123, 123), ManaPoints.of(123, 123), new Position(0, 0, 0, 0), 123, 456, 0, 80, 0);
	private final AbilityUsedEvent reprisalUsedByPc = new AbilityUsedEvent(
			new XivAbility(cd.getPrimaryAbilityId(), "Reprisal"),
			player,
			theBoss,
			Collections.singletonList(new StatusAppliedEffect(0, 0, 1193, 0, true)),
			123,
			0,
			1
	);
	private final AbilityUsedEvent reprisalUsedByPcSecondTarget = new AbilityUsedEvent(
			new XivAbility(cd.getPrimaryAbilityId(), "Reprisal"),
			player,
			theBoss,
			Collections.singletonList(new StatusAppliedEffect(0, 0, 1193, 0, true)),
			123,
			1,
			1
	);
	private final AbilityUsedEvent reprisalUsedByPartyMember = new AbilityUsedEvent(
			new XivAbility(cd.getPrimaryAbilityId(), "Reprisal"),
			otherCharInParty,
			theBoss,
			Collections.singletonList(new StatusAppliedEffect(0, 0, 1193, 0, true)),
			123,
			0,
			1
	);
	private final AbilityUsedEvent reprisalUsedByNonMember = new AbilityUsedEvent(
			new XivAbility(cd.getPrimaryAbilityId(), "Reprisal"),
			otherCharNotInParty,
			theBoss,
			Collections.singletonList(new StatusAppliedEffect(0, 0, 1193, 0, true)),
			123,
			0,
			1
	);


	private CdTracker makeState() {
		XivStateDummy state = new XivStateDummy(new PrimaryLogSource());
		state.setPlayer(player);
		state.setCombatants(List.of(
				player, otherCharInParty, otherCharNotInParty, theBoss
		));
		state.setPartyList(List.of(player, otherCharInParty));
		return new CdTracker(new InMemoryMapPersistenceProvider(), state);
	}


	@Test
	void testBasicCdTts() {
		CdTracker tracker = makeState();
		TestEventContext context = new TestEventContext();
		// Should default to over
		tracker.getPersonalCdSettings().get(cd).getTts().set(true);
		tracker.getPersonalCdSettings().get(cd).getOverlay().set(false);
		tracker.cdUsed(context, reprisalUsedByPc);
		List<Event> events = context.getEnqueued();
		Assert.assertEquals(events.size(), 1);
		Event event = events.get(0);
		if (event instanceof CdTracker.DelayedCdCallout dcc) {
			Assert.assertSame(reprisalUsedByPc, dcc.originalEvent);
		}
		Map<CdTrackingKey, AbilityUsedEvent> personal = tracker.getOverlayPersonalCds();
		Assert.assertEquals(personal.size(), 0);
	}

	@Test
	void testBasicCdOverlayData() {
		CdTracker tracker = makeState();
		TestEventContext context = new TestEventContext();
		// Should default to over
		tracker.getPersonalCdSettings().get(cd).getTts().set(false);
		tracker.getPersonalCdSettings().get(cd).getOverlay().set(true);
		tracker.cdUsed(context, reprisalUsedByPc);
		List<Event> events = context.getEnqueued();
		Assert.assertEquals(events.size(), 0);
		Map<CdTrackingKey, AbilityUsedEvent> personal = tracker.getOverlayPersonalCds();
		Assert.assertEquals(personal.size(), 1);
		Map.Entry<CdTrackingKey, AbilityUsedEvent> entry = personal.entrySet().iterator().next();
		Assert.assertSame(entry.getValue(), reprisalUsedByPc);

		Assert.assertSame(entry.getKey().getCooldown(), cd);
		Assert.assertSame(entry.getKey().getSource(), player);
	}

	// TODO: more tests
}































