package gg.xp.xivsupport.events.actlines.events.jobgauge;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.JobGaugeUpdate;
import gg.xp.xivsupport.events.actlines.events.RawJobGaugeEvent;
import gg.xp.xivsupport.events.actlines.events.jobguage.*;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivdata.data.Job;
import org.testng.Assert;
import org.testng.annotations.Test;

public class JobGaugeHandlersTest {

	private final XivCombatant dummyCombatant = new XivCombatant(0, "Test");

	@Test
	public void testWhmGauge() {
		// Test WHM gauge parsing
		// lilyDuration at [4],[3], lilyCount at [5], bloodLily at [6]
		byte[] data = new byte[10];
		data[3] = 0x10; // Low byte of duration
		data[4] = 0x20; // High byte of duration
		data[5] = 2;    // lilyCount
		data[6] = 1;    // bloodLily

		RawJobGaugeEvent raw = new RawJobGaugeEvent(dummyCombatant, Job.WHM, data);
		JobGaugeUpdate out = JobGaugeHandlers.parse(raw);

		Assert.assertTrue(out instanceof WhmGaugeEvent);

		WhmGaugeEvent event = (WhmGaugeEvent) out;
		Assert.assertEquals(event.getLilyCount(), 2);
		Assert.assertEquals(event.getBloodLily(), 1);
		Assert.assertEquals(event.getLilyDuration().toMillis(), 0x2010);
		Assert.assertEquals(event.getJob(), Job.WHM);
	}

	@Test
	public void testSchGauge() {
		// Test SCH gauge parsing
		// aetherflow at [1], faerieGauge at [2], seraphDuration at [4],[3], unknown5 at [5]
		byte[] data = new byte[10];
		data[1] = 3;    // aetherflow
		data[2] = 50;   // faerieGauge
		data[3] = 0x30; // Low byte of seraph duration
		data[4] = 0x40; // High byte of seraph duration
		data[5] = 6;    // unknown5 (6 when seraph is active)

		RawJobGaugeEvent raw = new RawJobGaugeEvent(dummyCombatant, Job.SCH, data);
		JobGaugeUpdate out = JobGaugeHandlers.parse(raw);

		Assert.assertTrue(out instanceof SchGaugeEvent);

		SchGaugeEvent event = (SchGaugeEvent) out;
		Assert.assertEquals(event.getAetherflow(), 3);
		Assert.assertEquals(event.getFaerieGauge(), 50);
		Assert.assertEquals(event.getSeraphDuration().toMillis(), 0x4030);
		Assert.assertEquals(event.getUnknown5(), 6);
		Assert.assertEquals(event.getJob(), Job.SCH);
	}

	@Test
	public void testAstGauge() {
		// Test AST gauge parsing
		// cardHeld at [6] & 0xf, minorHeld at ([6] >> 8) & 0xf
		// slot1 at [7] & 3, slot2 at ([7] >> 2) & 3, slot3 at ([7] >> 4) & 3
		byte[] data = new byte[10];
		data[6] = 0x05; // cardHeld = 5 (Ewer)
		data[7] = 0b00011011; // slot3=1, slot2=2, slot1=3

		RawJobGaugeEvent raw = new RawJobGaugeEvent(dummyCombatant, Job.AST, data);
		JobGaugeUpdate out = JobGaugeHandlers.parse(raw);

		Assert.assertTrue(out instanceof AstGaugeEvent);

		AstGaugeEvent event = (AstGaugeEvent) out;
		Assert.assertEquals(event.getCardHeld(), 5);
		Assert.assertEquals(event.getMinorHeld(), 0);
		Assert.assertEquals(event.getSlot1(), 3);
		Assert.assertEquals(event.getSlot2(), 2);
		Assert.assertEquals(event.getSlot3(), 1);
		Assert.assertEquals(event.getJob(), Job.AST);
	}

	@Test
	public void testSgeGauge() {
		// Test SGE gauge parsing
		// addersGallProgress at [2],[1], fullStacks at [3], adderSting at [4], eukrasiaActive at [5]
		byte[] data = new byte[10];
		data[1] = 0x20; // Low byte of progress
		data[2] = 0x10; // High byte of progress
		data[3] = 2;    // fullStacks (out of 255 due to & 0xff)
		data[4] = 1;    // adderSting
		data[5] = 1;    // eukrasiaActive

		RawJobGaugeEvent raw = new RawJobGaugeEvent(dummyCombatant, Job.SGE, data);
		JobGaugeUpdate out = JobGaugeHandlers.parse(raw);

		Assert.assertTrue(out instanceof SgeGaugeEvent);

		SgeGaugeEvent event = (SgeGaugeEvent) out;
		double expectedProgress = 2 + 0x1020 / (double) JobGaugeConstants.SGE_GAUGE_RECHARGE_TIME;
		Assert.assertEquals(event.getAddersGallNow(), expectedProgress, 0.01);
		Assert.assertEquals(event.getJob(), Job.SGE);
	}

	@Test
	public void testSgeGaugeEukrasiaOff() {
		// Test SGE gauge with Eukrasia off
		byte[] data = new byte[10];
		data[1] = 0x00;
		data[2] = 0x00;
		data[3] = 1;
		data[4] = 0;
		data[5] = 0; // eukrasiaActive = false

		RawJobGaugeEvent raw = new RawJobGaugeEvent(dummyCombatant, Job.SGE, data);
		JobGaugeUpdate out = JobGaugeHandlers.parse(raw);

		Assert.assertTrue(out instanceof SgeGaugeEvent);
	}

	@Test
	public void testPldGauge() {
		// Test PLD gauge parsing
		// oathGauge at [1]
		byte[] data = new byte[10];
		data[1] = 100; // oathGauge

		RawJobGaugeEvent raw = new RawJobGaugeEvent(dummyCombatant, Job.PLD, data);
		JobGaugeUpdate out = JobGaugeHandlers.parse(raw);

		Assert.assertTrue(out instanceof PldGaugeEvent);

		PldGaugeEvent event = (PldGaugeEvent) out;
		Assert.assertEquals(event.getOathGauge(), 100);
		Assert.assertEquals(event.getJob(), Job.PLD);
	}

	@Test
	public void testWarGauge() {
		// Test WAR gauge parsing
		// beastGauge at [1]
		byte[] data = new byte[10];
		data[1] = 50; // beastGauge

		RawJobGaugeEvent raw = new RawJobGaugeEvent(dummyCombatant, Job.WAR, data);
		JobGaugeUpdate out = JobGaugeHandlers.parse(raw);

		Assert.assertTrue(out instanceof WarGaugeEvent);

		WarGaugeEvent event = (WarGaugeEvent) out;
		Assert.assertEquals(event.getBeastGauge(), 50);
		Assert.assertEquals(event.getJob(), Job.WAR);
	}

	@Test
	public void testDrkGauge() {
		// Test DRK gauge parsing
		// bloodGauge at [1], darkSideDuration at [4],[3], esteemDuration at [8],[7]
		byte[] data = new byte[10];
		data[1] = 80;   // bloodGauge
		data[3] = 0x50; // Low byte of darkSide duration
		data[4] = 0x60; // High byte of darkSide duration
		data[7] = 0x70; // Low byte of esteem duration
		data[8] = (byte) 0x80; // High byte of esteem duration

		RawJobGaugeEvent raw = new RawJobGaugeEvent(dummyCombatant, Job.DRK, data);
		JobGaugeUpdate out = JobGaugeHandlers.parse(raw);

		Assert.assertTrue(out instanceof DrkGaugeEvent);

		DrkGaugeEvent event = (DrkGaugeEvent) out;
		Assert.assertEquals(event.getBloodGauge(), 80);
		Assert.assertEquals(event.getDarkSideDuration().toMillis(), 0x6050);
		Assert.assertEquals(event.getEsteemDuration().toMillis(), 0x8070);
		Assert.assertEquals(event.getJob(), Job.DRK);
	}

	@Test
	public void testGnbGauge() {
		// Test GNB gauge parsing
		// powderGauge at [1]
		byte[] data = new byte[10];
		data[1] = 2; // powderGauge

		RawJobGaugeEvent raw = new RawJobGaugeEvent(dummyCombatant, Job.GNB, data);
		JobGaugeUpdate out = JobGaugeHandlers.parse(raw);

		Assert.assertTrue(out instanceof GnbGaugeEvent);

		GnbGaugeEvent event = (GnbGaugeEvent) out;
		Assert.assertEquals(event.getPowderGauge(), 2);
		Assert.assertEquals(event.getJob(), Job.GNB);
	}

	@Test
	public void testRprGauge() {
		// Test RPR gauge parsing
		// soulGauge at [1], shroudGauge at [2], enshroudDuration at [4],[3],
		// blueShroudOrbs at [5], pinkShroudOrbs at [6]
		byte[] data = new byte[10];
		data[1] = 50;   // soulGauge
		data[2] = 30;   // shroudGauge
		data[3] = 0x10; // Low byte of enshroud duration
		data[4] = 0x20; // High byte of enshroud duration
		data[5] = 3;    // blueShroudOrbs
		data[6] = 2;    // pinkShroudOrbs

		RawJobGaugeEvent raw = new RawJobGaugeEvent(dummyCombatant, Job.RPR, data);
		JobGaugeUpdate out = JobGaugeHandlers.parse(raw);

		Assert.assertTrue(out instanceof RprGaugeEvent);

		RprGaugeEvent event = (RprGaugeEvent) out;
		Assert.assertEquals(event.getSoulGauge(), 50);
		Assert.assertEquals(event.getShroudGauge(), 30);
		Assert.assertEquals(event.getEnshroudDuration().toMillis(), 0x2010);
		Assert.assertEquals(event.getBlueShroudOrbs(), 3);
		Assert.assertEquals(event.getPinkShroudOrbs(), 2);
		Assert.assertEquals(event.getJob(), Job.RPR);
	}

	@Test
	public void testMchGauge() {
		// Test MCH gauge parsing
		// hyperchargeDuration at [2],[1], queenDuration at [4],[3],
		// heatGauge at [5], batteryGauge at [6]
		byte[] data = new byte[10];
		data[1] = 0x11; // Low byte of hypercharge duration
		data[2] = 0x22; // High byte of hypercharge duration
		data[3] = 0x33; // Low byte of queen duration
		data[4] = 0x44; // High byte of queen duration
		data[5] = 80;   // heatGauge
		data[6] = 90;   // batteryGauge

		RawJobGaugeEvent raw = new RawJobGaugeEvent(dummyCombatant, Job.MCH, data);
		JobGaugeUpdate out = JobGaugeHandlers.parse(raw);

		Assert.assertTrue(out instanceof MchGaugeEvent);

		MchGaugeEvent event = (MchGaugeEvent) out;
		Assert.assertEquals(event.getHyperchargeDuration().toMillis(), 0x2211);
		Assert.assertEquals(event.getQueenDuration().toMillis(), 0x4433);
		Assert.assertEquals(event.getHeatGauge(), 80);
		Assert.assertEquals(event.getBatteryGauge(), 90);
		Assert.assertEquals(event.getJob(), Job.MCH);
	}

	@Test
	public void testUnsupportedJob() {
		// Test that unsupported jobs return null and don't add events
		byte[] data = new byte[10];

		RawJobGaugeEvent raw = new RawJobGaugeEvent(dummyCombatant, Job.BLM, data);
		JobGaugeUpdate out = JobGaugeHandlers.parse(raw);

		Assert.assertNull(out);
	}

	@Test
	public void testAllSupportedJobs() {
		// Verify all supported jobs produce an event
		Job[] supportedJobs = {Job.WHM, Job.SCH, Job.AST, Job.SGE, Job.PLD,
		                       Job.WAR, Job.DRK, Job.GNB, Job.RPR, Job.MCH};

		for (Job job : supportedJobs) {
			byte[] data = new byte[10];
			RawJobGaugeEvent raw = new RawJobGaugeEvent(dummyCombatant, job, data);
			JobGaugeUpdate out = JobGaugeHandlers.parse(raw);
			Assert.assertNotNull(out, "Job " + job + " should produce exactly one event");
			Assert.assertEquals(out.getJob(), job);
		}
	}
}
