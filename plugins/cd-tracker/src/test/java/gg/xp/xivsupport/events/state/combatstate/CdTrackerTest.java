package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.context.BasicStateStore;
import gg.xp.reevent.events.BasicEventDistributor;
import gg.xp.reevent.events.BasicEventQueue;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventMaster;
import gg.xp.xivdata.data.Cooldown;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.cdsupport.CustomCooldownManager;
import gg.xp.xivsupport.events.CloseTo;
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
import org.hamcrest.MatcherAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CdTrackerTest {

	private static final Logger log = LoggerFactory.getLogger(CdTrackerTest.class);

	private static final Cooldown reprisal = Cooldown.Reprisal;
	private static final Cooldown draw = Cooldown.Draw;
	private final XivPlayerCharacter player = new XivPlayerCharacter(0x10000001, "Me, The Player", Job.GNB, XivWorld.of(), true, 1, new HitPoints(123, 123), ManaPoints.of(123, 123), new Position(0, 0, 0, 0), 0, 0, 1, 80, 0, 0);
	private final XivPlayerCharacter otherCharInParty = new XivPlayerCharacter(0x10000002, "Someone Else In My Party", Job.GNB, XivWorld.of(), false, 1, new HitPoints(123, 123), ManaPoints.of(123, 123), new Position(0, 0, 0, 0), 0, 0, 1, 80, 0, 0);
	private final XivPlayerCharacter otherCharNotInParty = new XivPlayerCharacter(0x10000003, "Someone Else Not In Party", Job.GNB, XivWorld.of(), false, 1, new HitPoints(123, 123), ManaPoints.of(123, 123), new Position(0, 0, 0, 0), 0, 0, 0, 80, 0, 0);
	private final XivCombatant theBoss = new XivCombatant(0x40000001, "The Boss", false, false, 2, new HitPoints(123, 123), ManaPoints.of(123, 123), new Position(0, 0, 0, 0), 123, 456, 0, 80, 0, 0);

	private AbilityUsedEvent reprisalUsedByPc() {
		return new AbilityUsedEvent(
				new XivAbility(reprisal.getPrimaryAbilityId(), "Reprisal"),
				player,
				theBoss,
				Collections.singletonList(new StatusAppliedEffect(0, 0, 1193, 0, true)),
				123,
				0,
				1
		);
	}

	private AbilityUsedEvent drawUsedByPc() {
		return new AbilityUsedEvent(
				new XivAbility(draw.getPrimaryAbilityId(), "Draw"),
				player,
				player,
				Collections.emptyList(),
//				Collections.singletonList(new StatusAppliedEffect(0, 0, 1193, 0, true)),
				123,
				0,
				1
		);
	}

	;

	private AbilityUsedEvent reprisalUsedByPcSecondTarget() {
		return new AbilityUsedEvent(
				new XivAbility(reprisal.getPrimaryAbilityId(), "Reprisal"),
				player,
				theBoss,
				Collections.singletonList(new StatusAppliedEffect(0, 0, 1193, 0, true)),
				123,
				1,
				1
		);
	}

	;

	private AbilityUsedEvent reprisalUsedByPartyMember() {
		return new AbilityUsedEvent(
				new XivAbility(reprisal.getPrimaryAbilityId(), "Reprisal"),
				otherCharInParty,
				theBoss,
				Collections.singletonList(new StatusAppliedEffect(0, 0, 1193, 0, true)),
				123,
				0,
				1
		);
	}

	;

	private AbilityUsedEvent reprisalUsedByNonMember() {
		return new AbilityUsedEvent(
				new XivAbility(reprisal.getPrimaryAbilityId(), "Reprisal"),
				otherCharNotInParty,
				theBoss,
				Collections.singletonList(new StatusAppliedEffect(0, 0, 1193, 0, true)),
				123,
				0,
				1
		);
	}

	;


	private CdTracker makeState() {
		XivStateDummy state = new XivStateDummy(new PrimaryLogSource());
		state.setPlayer(player);
		state.setCombatants(List.of(
				player, otherCharInParty, otherCharNotInParty, theBoss
		));
		state.setPartyList(List.of(player, otherCharInParty));
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		CustomCooldownManager ccm = new CustomCooldownManager(pers, new EventMaster(new BasicEventDistributor(new BasicStateStore()), new BasicEventQueue()));
		return new CdTracker(pers, state, ccm);
	}


	@Test
	void testBasicCdTts() {
		CdTracker tracker = makeState();
		TestEventContext context = new TestEventContext();
		int precallTime = 3000;
		tracker.getCdTriggerAdvancePersonal().set(precallTime);
		// Should default to over
		tracker.getPersonalCdSettings().get(reprisal).getTtsReady().set(true);
		tracker.getPersonalCdSettings().get(reprisal).getOverlay().set(false);
		AbilityUsedEvent myEvent = reprisalUsedByPc();
		tracker.cdUsed(context, myEvent);
		List<Event> events = context.getEnqueued();
		Assert.assertEquals(events.size(), 1);
		Event event = events.get(0);
		if (event instanceof CdTracker.DelayedCdCallout dcc) {
			Assert.assertSame(dcc.originalEvent, myEvent);
			MatcherAssert.assertThat(dcc.delayedEnqueueAt() - dcc.getTimeBasis(), new CloseTo(60_000 - precallTime, 100));
		}
		Map<CdTrackingKey, AbilityUsedEvent> personal = tracker.getOverlayPersonalCds();
		Assert.assertEquals(personal.size(), 0);
		Map<CdTrackingKey, AbilityUsedEvent> cds = tracker.getCds((cd) -> true);
		Assert.assertEquals(cds.size(), 1);
		CdTrackingKey key = cds.keySet().iterator().next();
		Instant replenishedAt = tracker.getReplenishedAt(key);
		Instant happenedAt = myEvent.getEffectiveHappenedAt();
		long delta = Duration.between(happenedAt, replenishedAt).toMillis();
		MatcherAssert.assertThat(delta, new CloseTo(60_000, 100));
	}

	@Test
	void testBasicCdOverlayData() {
		CdTracker tracker = makeState();
		TestEventContext context = new TestEventContext();
		// Should default to over
		tracker.getPersonalCdSettings().get(reprisal).getTtsReady().set(false);
		tracker.getPersonalCdSettings().get(reprisal).getOverlay().set(true);
		AbilityUsedEvent myEvent = reprisalUsedByPc();
		tracker.cdUsed(context, myEvent);
		List<Event> events = context.getEnqueued();
		Assert.assertEquals(events.size(), 0);
		Map<CdTrackingKey, AbilityUsedEvent> personal = tracker.getOverlayPersonalCds();
		Assert.assertEquals(personal.size(), 1);
		Map.Entry<CdTrackingKey, AbilityUsedEvent> entry = personal.entrySet().iterator().next();
		Assert.assertSame(entry.getValue(), myEvent);

		Assert.assertSame(entry.getKey().getCooldown(), reprisal);
		Assert.assertSame(entry.getKey().getSource(), player);
	}

	// TODO: more tests
	@Test
	void testCharges() {
		CdTracker tracker = makeState();
		TestEventContext context = new TestEventContext();
		int precallTime = 3000;
		tracker.getCdTriggerAdvancePersonal().set(precallTime);
		// Should default to over
		tracker.getPersonalCdSettings().get(draw).getTtsReady().set(true);
		tracker.getPersonalCdSettings().get(draw).getOverlay().set(true);
		AbilityUsedEvent myEvent1 = drawUsedByPc();
		tracker.cdUsed(context, myEvent1);
		{
			List<Event> events = context.getEnqueued();
			Assert.assertEquals(events.size(), 1);
			Event event = events.get(0);
			if (event instanceof CdTracker.DelayedCdCallout dcc) {
				Assert.assertSame(dcc.originalEvent, myEvent1);
				MatcherAssert.assertThat(dcc.delayedEnqueueAt() - dcc.getTimeBasis(), new CloseTo(30_000 - precallTime, 100));
			}
			Map<CdTrackingKey, AbilityUsedEvent> personal = tracker.getOverlayPersonalCds();
			Assert.assertEquals(personal.size(), 1);
			Map<CdTrackingKey, AbilityUsedEvent> cds = tracker.getCds((cd) -> true);
			Assert.assertEquals(cds.size(), 1);
			CdTrackingKey key = cds.keySet().iterator().next();
			Instant replenishedAt = tracker.getReplenishedAt(key);
			Instant happenedAt = myEvent1.getEffectiveHappenedAt();
			long delta = Duration.between(happenedAt, replenishedAt).toMillis();
			MatcherAssert.assertThat(delta, new CloseTo(30_000, 100));
		}

		AbilityUsedEvent myEvent2 = drawUsedByPc();
		tracker.cdUsed(context, myEvent2);
		{
			List<Event> events = context.getEnqueued();
			Assert.assertEquals(events.size(), 2);
			Event event1 = events.get(0);
			if (event1 instanceof CdTracker.DelayedCdCallout dcc) {
				Assert.assertSame(dcc.originalEvent, myEvent1);
				MatcherAssert.assertThat(dcc.delayedEnqueueAt() - dcc.getTimeBasis(), new CloseTo(30_000 - precallTime, 100));
			}
			Event event2 = events.get(1);
			if (event2 instanceof CdTracker.DelayedCdCallout dcc) {
				Assert.assertSame(dcc.originalEvent, myEvent2);
				MatcherAssert.assertThat(dcc.delayedEnqueueAt() - dcc.getTimeBasis(), new CloseTo(2 * 30_000 - precallTime, 100));
			}
			Map<CdTrackingKey, AbilityUsedEvent> personal = tracker.getOverlayPersonalCds();
			Assert.assertEquals(personal.size(), 1);
			Map<CdTrackingKey, AbilityUsedEvent> cds = tracker.getCds((cd) -> true);
			Assert.assertEquals(cds.size(), 1);
			CdTrackingKey key = cds.keySet().iterator().next();
			Instant replenishedAt = tracker.getReplenishedAt(key);
			Instant happenedAt = myEvent1.getEffectiveHappenedAt();
			long delta = Duration.between(happenedAt, replenishedAt).toMillis();
			MatcherAssert.assertThat(delta, new CloseTo(30_000 * 2, 100));
		}
	}
}































