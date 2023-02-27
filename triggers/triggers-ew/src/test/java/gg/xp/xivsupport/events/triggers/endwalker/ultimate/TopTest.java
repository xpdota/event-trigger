package gg.xp.xivsupport.events.triggers.endwalker.ultimate;

import gg.xp.xivsupport.events.triggers.duties.ewult.OmegaUltimate;
import gg.xp.xivsupport.events.triggers.util.AmVerificationValues;
import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;
import org.picocontainer.MutablePicoContainer;

import java.util.List;

import static gg.xp.xivdata.data.Job.*;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.ATTACK1;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.ATTACK2;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.ATTACK3;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.ATTACK4;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.ATTACK_NEXT;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.BIND1;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.BIND2;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.BIND3;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.BIND_NEXT;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.CLEAR;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.CROSS;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.IGNORE1;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.IGNORE2;

public class TopTest extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/top.log";
	}

	@Override
	protected void configure(MutablePicoContainer pico) {
		OmegaUltimate omega = pico.getComponent(OmegaUltimate.class);
		omega.getLooperAM().set(true);
		omega.getPantoAmEnable().set(true);
		omega.getPsAmEnable().set(true);
		omega.getSniperAmEnable().set(true);
		omega.getMonitorAmEnable().set(true);
		omega.getDeltaAmEnable().set(true);
		omega.getSigmaAmEnable().set(true);
		omega.getSigmaAmDelay().set(15);
		omega.getOmegaAmEnable().set(true);
		omega.getOmegaFirstSetDelay().set(20);
		omega.getOmegaSecondSetDelay().set(3);
	}

	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				call(15244, "Four with DRK Player", "Four with DRK Player"),
				call(20480, "1", "1"),
				call(28570, "Take tether", "Take tether"),
				call(37597, "3", "3"),
				call(46582, "Take tower", "Take tower (8.9)"),
				call(68968, "Four", "Four with DRG Player - Missile in (29.7)"),
				call(80944, "2 - Stay Stacked", "2 - Stay Stacked"),
				call(86930, "3 - Stay Stacked", "3 - Stay Stacked"),
				call(92962, "Missile Now", "Missile Now (5.7)"),
				call(100513, "Cleave Baits", "Cleave Baits"),
				call(102432, "Position for Cleave", "Position for Cleave"),
				call(107522, "Avoid Cleaves", "Avoid Cleaves"),
				call(139040, "Attack F", "Attack F"),
				call(154330, "Check M/F Sword/Shield", "Check M/F Sword/Shield"),
				call(168050, "Square, far from SMN Player", "Square, far from SMN Player (19.4)"),
				call(173237, "Eye Laser", "Eye Laser (1.0)"),
				call(174535, "Knockback Stacks", "Knockback Stacks"),
				call(175071, "Far Stacks on DRK Player and GNB Player", "Far Stacks on DRK Player and GNB Player"),
				call(176769, "No Swap", "No Swap"),
				call(204258, "Limitless Synergy", "Limitless Synergy"),
				call(214493, "Give Tethers to Tanks", "Give Tethers to Tanks"),
				call(228533, "Spread", "Spread"),
				call(233796, "Stack", "Stack"),
				call(240637, "Raidwide", "Raidwide"),
				call(283045, "Sniper Soon", "Sniper Soon (18.2)"),
				call(291493, "In", "In"),
				call(299674, "In, Dodge Hand", "In, Dodge Hand"),
				call(309636, "Raidwide", "Raidwide (4.7)"),
				call(314728, "Red has Defa", "Red has Defa"),
				call(317850, "Stack between Blue", "Stack between Blue (19.7)"),
				call(338629, "Get Rot, Stretch Tether", "Get Rot, Stretch Tether (9.0)"),
				call(344882, "Stack in Blue", "Stack in Blue (14.8)"),
				call(359681, "Pass Rot then Spread", "Pass Rot then Spread (7.8)"),
				call(367723, "Get Defa from Red", "Get Defa from Red (11.9)"),
				call(380732, "Get Rot, Shrink Tether", "Get Rot, Shrink Tether (8.9)"),
				call(389133, "Defa in Red", "Defa in Red (12.7)"),
				call(401788, "Spread for Rot", "Spread for Rot (8.1)"),
				call(415090, "Raidwide", "Raidwide (7.7)"),
				call(434447, "No Monitor, Boss Cleaving West", "No Monitor, Boss Cleaving West"),
				call(473916, "Spread", "Spread (4.7)"),
				call(476421, "Stacks on DRK Player, DRG Player", "Stacks on DRK Player, DRG Player"),
				call(479280, "Stacks", "Stacks (4.7)"),
				call(484463, "Spread Outside", "Spread Outside"),
				call(486557, "Stacks on SGE Player, SMN Player", "Stacks on SGE Player, SMN Player"),
				call(489106, "In Now then Stacks", "In Now then Stacks (5.0)"),
				call(494595, "Spread Outside", "Spread Outside"),
				call(496782, "Stacks on YOU, MNK Player", "Stacks on YOU, MNK Player"),
				call(499329, "Stacks Outside", "Stacks Outside (5.0)"),
				call(504289, "Move In", "Move In"),
				call(506435, "Move In", "Move In"),
				call(511528, "Heavy Raidwide", "Heavy Raidwide (7.7)"),
				call(539146, "Buster on GNB Player", "Buster on GNB Player (4.7)"),
				call(555678, "Raidwide", "Raidwide (4.7)"),
				call(563946, "Local with MNK Player", "Local with MNK Player"),
				call(583831, "Bait", "Bait"),
				call(589327, "Move In, No Monitor", "Move In, No Monitor"),
				call(597101, "Distant World", "Distant World"),
				call(621008, "Buster on GNB Player", "Buster on GNB Player (4.7)"),
				call(637599, "Raidwide", "Raidwide (4.7)"),
				call(645648, "Circle, far from DRK Player", "Circle, far from DRK Player (32.0)"),
				call(655840, "Marker on You and Buddy", "Marker on You and Buddy"),
				call(664960, "Knockback into Tower", "Knockback into Tower"),
				call(677487, "One Stack", "One Stack"),
				call(727298, "Raidwide", "Raidwide (4.7)"),
				call(735484, "Long Distant", "Long Distant (49.9)"),
				call(769984, "Long Distant", "Long Distant (15.4)")
		);
	}

	@Override
	protected List<AmVerificationValues> getExpectedAms() {
		return List.of(
				// Looper
				mark(15244, ATTACK1, SMN),
				mark(15244, ATTACK2, DRG),
				mark(15244, ATTACK3, MNK),
				mark(15244, ATTACK4, DRK),
				mark(15244, BIND1, SGE),
				mark(15244, BIND2, GNB),
				mark(15244, BIND3, DNC),
				mark(15244, CROSS, WHM),
				clearAll(50325),
				// Panto
				mark(68833, ATTACK1, DRK),
				mark(68833, ATTACK2, MNK),
				mark(68833, ATTACK3, SMN),
				mark(68833, ATTACK4, DRG),
				mark(68833, BIND1, GNB),
				mark(68833, BIND2, SGE),
				mark(68833, BIND3, DNC),
				mark(68833, CROSS, WHM),
				clearAll(103860),
				// PS
				mark(160405, ATTACK1, DNC),
				mark(160405, ATTACK2, MNK),
				mark(160405, ATTACK3, SMN),
				mark(160405, ATTACK4, DRK),
				mark(160405, BIND1, SGE),
				mark(160405, BIND2, DRG),
				mark(160405, BIND3, WHM),
				mark(160405, CROSS, GNB),
				clearAll(185666),
				// Sniper
				clearAll(282287),
				mark(283358, ATTACK1, DRG),
				mark(283358, ATTACK2, SMN),
				mark(283358, ATTACK3, WHM),
				mark(283358, ATTACK4, SGE),
				mark(283358, BIND1, GNB),
				mark(283358, IGNORE1, DNC),
				mark(283358, BIND2, MNK),
				mark(283358, IGNORE2, DRK),
				clearAll(299674),
				// Monitor
				clearAll(434447),
				mark(435475, BIND_NEXT, DRG),
				mark(435475, BIND_NEXT, GNB),
				mark(435475, BIND_NEXT, DNC),
				mark(435475, ATTACK_NEXT, MNK),
				mark(435475, ATTACK_NEXT, DRK),
				mark(435475, ATTACK_NEXT, SMN),
				mark(435475, ATTACK_NEXT, WHM),
				mark(435475, ATTACK_NEXT, SGE),
				clearAll(444411),
				// Delta
				mark(563946, IGNORE1, SGE),
				mark(563946, IGNORE2, WHM),
				mark(607693, CLEAR, SGE),
				mark(607693, CLEAR, WHM),
				// Sigma pre-clear
				clearAll(637599),
				// Sigma
				mark(662100, IGNORE1, SGE),
				mark(662100, IGNORE2, DRG),
				mark(662100, ATTACK1, DRK),
				mark(662100, ATTACK2, SMN),
				mark(662100, ATTACK3, DNC),
				mark(662100, BIND1, WHM),
				mark(662100, BIND2, MNK),
				mark(662100, BIND3, GNB),
				clearAll(702351),
				// Omega 1st set
				clearAll(727298),
				mark(755540, IGNORE1, SMN),
				mark(755540, IGNORE2, GNB),
				mark(755540, ATTACK1, DRK),
				mark(755540, ATTACK2, MNK),
				mark(755540, ATTACK3, DNC),
				mark(755540, ATTACK4, SGE),
				mark(755540, BIND1, WHM),
				mark(755540, BIND2, DRG),
				// Omega 2nd set
				clearAll(767438),
				mark(773205, IGNORE1, DRK),
				mark(773205, IGNORE2, WHM),
				mark(773205, ATTACK1, GNB),
				mark(773205, ATTACK2, MNK),
				mark(773205, ATTACK3, DRG),
				mark(773205, ATTACK4, DNC),
				mark(773205, BIND1, SGE),
				mark(773205, BIND2, SMN),
				clearAll(785325)
		);
	}
}
