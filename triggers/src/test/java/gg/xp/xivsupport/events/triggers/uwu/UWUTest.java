package gg.xp.xivsupport.events.triggers.uwu;

import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;

import java.util.List;

public class UWUTest extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/uwu.log";
	}

	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				call(5965, "Slipstream", "Slipstream (2.2)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@3f5d1e0b |
				call(21300, "Slipstream", "Slipstream (2.2)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@4bfb83ae |
				call(49311, "Stack", "Stack (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@36c6fc09 |
				call(55456, "Stack", "Stack (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@6cb491c0 |
				call(73881, "Raidwide", "Raidwide (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@3bce771b |
				call(119503, "Slipstream", "Slipstream (2.2)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@2cbf753f |
				call(126942, "Garuda Woken", "Garuda Woken"), // from BuffApplied{buff=XivStatusEffect(0x5F9:Woken), duration=indef, source=XivEntity(ENVIRONMENT), target=XivCombatant(0x4003A43E:Garuda:NPC at Pos(100.11, 97.18, -0.00 : 3.11) NPC 8722:1644), stacks=0, isRefresh=false} |
				call(145642, "Raidwide", "Raidwide (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@2d98e215 |
				call(179294, "Searing Wind on Astro Player", "Searing Wind on Astro Player (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@656a4cf7 |
				call(182142, "", "Searing Wind on Astro Player (18.0)"), // from BuffApplied{buff=XivStatusEffect(0x62A:Searing Wind), duration=PT18S, source=XivCombatant(0x4003A43D:Ifrit:NPC at Pos(105.64, 102.43, 0.00 : -2.11) NPC 8730:1185), target=XivPlayerCharacter(0x10694638:Astro Player, AST, TODO, 70, false), stacks=0, isRefresh=false} |
				call(201263, "Ifrit Woken", "Ifrit Woken"), // from BuffApplied{buff=XivStatusEffect(0x5F9:Woken), duration=indef, source=XivEntity(ENVIRONMENT), target=XivCombatant(0x4003A43D:Ifrit:NPC at Pos(106.22, 102.68, 0.00 : 1.70) NPC 8730:1185), stacks=0, isRefresh=false} |
				call(208656, "Raidwide", "Raidwide (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@2dd489ee |
				call(218002, "Searing Wind on Astro Player", "Searing Wind on Astro Player (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@7a1517e6 |
				call(220848, "", "Searing Wind on Astro Player (30.0)"), // from BuffApplied{buff=XivStatusEffect(0x62A:Searing Wind), duration=PT30S, source=XivCombatant(0x4003A43D:Ifrit:NPC at Pos(102.89, 103.53, 0.00 : -3.00) NPC 8730:1185), target=XivPlayerCharacter(0x10694638:Astro Player, AST, TODO, 70, false), stacks=0, isRefresh=false} |
				call(236068, "Searing Wind on YOU", "Searing Wind on YOU (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@1077cafb |
				call(238921, "", "Searing Wind on YOU (30.0)"), // from BuffApplied{buff=XivStatusEffect(0x62A:Searing Wind), duration=PT30S, source=XivCombatant(0x4003A43D:Ifrit:NPC at Pos(108.08, 109.58, 0.00 : 0.74) NPC 8730:1185), target=XivPlayerCharacter(0x10669D22:Scholar Player, SCH, TODO, 70, true), stacks=0, isRefresh=false} |
				call(243115, "Stack on BRD Player", "Stack on BRD Player"), // from HeadMarkerEvent(117 on XivPlayerCharacter(0x105E0EF5:BRD Player, BRD, TODO, 70, false)) |
				call(258859, "Raidwide", "Raidwide (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@27cd31f6 |
				call(292051, "Right", "Right"), // from gg.xp.xivsupport.events.actlines.events.ActionSyncEvent@2edf4718 |
				call(321942, "Titan Woken", "Titan Woken"), // from BuffApplied{buff=XivStatusEffect(0x5F9:Woken), duration=indef, source=XivEntity(ENVIRONMENT), target=XivCombatant(0x4003A439:Titan:NPC at Pos(86.35, 100.97, 0.00 : 0.29) NPC 8727:1801), stacks=0, isRefresh=false} |
				call(456163, "Ultima", "Ultima (4.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@7c27cf02 |
				call(510471, "Homing Laser on GUB Player", "Homing Laser on GUB Player (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@52a9f26f |
				call(551941, "Ifrit, Garuda, Titan", "Ifrit → Garuda → Titan"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@1c252b4d |
				call(563076, "Garuda", "Garuda → Titan"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@48164856 |
				call(569315, "Back then Right"),
				call(579468, "Titan", "Titan"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@a0bd189 |
				call(592791, "Homing Laser on WAR Main", "Homing Laser on WAR Main (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@17ac702d |
				call(611683, "Stack on SMN Player", "Stack on SMN Player"), // from HeadMarkerEvent(117 on XivPlayerCharacter(0x106AC23F:SMN Player, SMN, TODO, 70, false)) |
				call(618903, "Searing Wind on YOU", "Searing Wind on YOU (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@22e64ed6 |
				call(621755, "", "Searing Wind on YOU (30.0)"), // from BuffApplied{buff=XivStatusEffect(0x62A:Searing Wind), duration=PT30S, source=XivCombatant(0x4003A43D:Ifrit:NPC at Pos(86.29, 113.69, 0.00 : 2.90) NPC 8730:1185), target=XivPlayerCharacter(0x10669D22:Scholar Player, SCH, TODO, 70, true), stacks=0, isRefresh=false} |
				call(652632, "Homing Laser on GUB Player", "Homing Laser on GUB Player (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@58c8e624 |
				call(681127, "Left Laser", "Left Laser (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@c9bf32d |
				call(685271, "Left Laser", "Left Laser (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@78c2d78b |
				call(689416, "Middle Laser", "Middle Laser (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@6842a6bb |
				call(690173, "Stack on SMN Player", "Stack on SMN Player"), // from HeadMarkerEvent(117 on XivPlayerCharacter(0x106AC23F:SMN Player, SMN, TODO, 70, false)) |
				call(706968, "Ultima", "Ultima (4.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@77a48451 |
				call(37645, "Slipstream", "Slipstream (2.2)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@24f50313 |
				call(53033, "Slipstream", "Slipstream (2.2)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@4a65e689 |
				call(81100, "Stack", "Stack (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@37efe600 |
				call(87201, "Stack", "Stack (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@64ba7aa0 |
				call(105601, "Raidwide", "Raidwide (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@685b00bf |
				call(151183, "Slipstream", "Slipstream (2.2)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@d6ff293 |
				call(158625, "Garuda Woken", "Garuda Woken"), // from BuffApplied{buff=XivStatusEffect(0x5F9:Woken), duration=indef, source=XivEntity(ENVIRONMENT), target=XivCombatant(0x4003AC6D:Garuda:NPC at Pos(99.35, 99.63, -0.00 : 3.13) NPC 8722:1644), stacks=0, isRefresh=false} |
				call(176725, "Raidwide", "Raidwide (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@4578cad8 |
				call(210355, "Searing Wind on YOU", "Searing Wind on YOU (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@72a0c7ad |
				call(213206, "", "Searing Wind on YOU (18.0)"), // from BuffApplied{buff=XivStatusEffect(0x62A:Searing Wind), duration=PT18S, source=XivCombatant(0x4003AC6C:Ifrit:NPC at Pos(102.31, 94.38, -0.00 : -0.32) NPC 8730:1185), target=XivPlayerCharacter(0x10669D22:Scholar Player, SCH, TODO, 70, true), stacks=0, isRefresh=false} |
				call(233066, "Ifrit Woken", "Ifrit Woken"), // from BuffApplied{buff=XivStatusEffect(0x5F9:Woken), duration=indef, source=XivEntity(ENVIRONMENT), target=XivCombatant(0x4003AC6C:Ifrit:NPC at Pos(101.64, 96.36, -0.00 : -3.12) NPC 8730:1185), stacks=0, isRefresh=false} |
				call(239701, "Raidwide", "Raidwide (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@424e6fbe |
				call(249104, "Searing Wind on YOU", "Searing Wind on YOU (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@30c7e60e |
				call(251957, "", "Searing Wind on YOU (30.0)"), // from BuffApplied{buff=XivStatusEffect(0x62A:Searing Wind), duration=PT30S, source=XivCombatant(0x4003AC6C:Ifrit:NPC at Pos(104.08, 96.42, -0.00 : -1.08) NPC 8730:1185), target=XivPlayerCharacter(0x10669D22:Scholar Player, SCH, TODO, 70, true), stacks=0, isRefresh=false} |
				call(267191, "Searing Wind on Astro Player", "Searing Wind on Astro Player (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@33ead4c4 |
				call(270035, "", "Searing Wind on Astro Player (30.0)"), // from BuffApplied{buff=XivStatusEffect(0x62A:Searing Wind), duration=PT30S, source=XivCombatant(0x4003AC6C:Ifrit:NPC at Pos(108.96, 91.94, -0.00 : 2.47) NPC 8730:1185), target=XivPlayerCharacter(0x10694638:Astro Player, AST, TODO, 70, false), stacks=0, isRefresh=false} |
				call(274221, "Stack on SMN Player", "Stack on SMN Player"), // from HeadMarkerEvent(117 on XivPlayerCharacter(0x106AC23F:SMN Player, SMN, TODO, 70, false)) |
				call(290947, "Raidwide", "Raidwide (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@4703f0bb |
				call(324182, "Left", "Left"), // from gg.xp.xivsupport.events.actlines.events.TickEvent@7681447a |
				call(354164, "Titan Woken", "Titan Woken"), // from BuffApplied{buff=XivStatusEffect(0x5F9:Woken), duration=indef, source=XivEntity(ENVIRONMENT), target=XivCombatant(0x4003AC68:Titan:NPC at Pos(113.97, 99.99, -0.00 : -2.90) NPC 8727:1801), stacks=0, isRefresh=false} |
				call(487006, "Ultima", "Ultima (4.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@79f92b32 |
				call(541293, "Homing Laser on GUB Player", "Homing Laser on GUB Player (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@1c508c3d |
				call(582810, "Ifrit, Garuda, Titan", "Ifrit → Garuda → Titan"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@440e2124 |
				call(593943, "Garuda", "Garuda → Titan"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@38d00f4b |
				call(600221, "Back then Right"),
				call(610324, "Titan", "Titan"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@84003d8 |
				call(623686, "Homing Laser on WAR Main", "Homing Laser on WAR Main (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@10e94789 |
				call(642565, "Stack on BRD Player", "Stack on BRD Player"), // from HeadMarkerEvent(117 on XivPlayerCharacter(0x105E0EF5:BRD Player, BRD, TODO, 70, false)) |
				call(649779, "Searing Wind on YOU", "Searing Wind on YOU (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@255b43b1 |
				call(652630, "", "Searing Wind on YOU (30.0)"), // from BuffApplied{buff=XivStatusEffect(0x62A:Searing Wind), duration=PT30S, source=XivCombatant(0x4003AC6C:Ifrit:NPC at Pos(113.69, 113.69, 0.00 : -2.24) NPC 8730:1185), target=XivPlayerCharacter(0x10669D22:Scholar Player, SCH, TODO, 70, true), stacks=0, isRefresh=false} |
				call(683459, "Homing Laser on DRG Player", "Homing Laser on DRG Player (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@1dc9aa06 |
				call(715579, "Right Laser", "Right Laser (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@151f24a4 |
				call(719724, "Right Laser", "Right Laser (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@4f90fc81 |
				call(723868, "Left Laser", "Left Laser (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@30a97809 |
				call(724624, "Stack on SMN Player", "Stack on SMN Player"), // from HeadMarkerEvent(117 on XivPlayerCharacter(0x106AC23F:SMN Player, SMN, TODO, 70, false)) |
				call(29812, "Mistral on you", "Mistral on you"), // from HeadMarkerEvent(16 on XivPlayerCharacter(0x10669D22:Scholar Player, SCH, TODO, 70, true)) |
				call(29901, "Slipstream", "Slipstream (2.2)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@60e07d80 |
				call(45250, "Slipstream", "Slipstream (2.2)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@c365f3c |
				call(73315, "Stack", "Stack (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@448adecf |
				call(79419, "Stack", "Stack (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@3f989f67 |
				call(97822, "Raidwide", "Raidwide (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@5c77f969 |
				call(143418, "Slipstream", "Slipstream (2.2)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@62068264 |
				call(150855, "Garuda Woken", "Garuda Woken"), // from BuffApplied{buff=XivStatusEffect(0x5F9:Woken), duration=indef, source=XivEntity(ENVIRONMENT), target=XivCombatant(0x4003B509:Garuda:NPC at Pos(99.38, 99.23, -0.00 : -2.98) NPC 8722:1644), stacks=0, isRefresh=false} |
				call(174476, "Raidwide", "Raidwide (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@70e8444d |
				call(208121, "Searing Wind on YOU", "Searing Wind on YOU (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@bf08e90 |
				call(210970, "", "Searing Wind on YOU (18.0)"), // from BuffApplied{buff=XivStatusEffect(0x62A:Searing Wind), duration=PT18S, source=XivCombatant(0x4003B508:Ifrit:NPC at Pos(106.03, 97.18, -0.00 : -1.07) NPC 8730:1185), target=XivPlayerCharacter(0x10669D22:Scholar Player, SCH, TODO, 70, true), stacks=0, isRefresh=false} |
				call(230799, "Ifrit Woken", "Ifrit Woken"), // from BuffApplied{buff=XivStatusEffect(0x5F9:Woken), duration=indef, source=XivEntity(ENVIRONMENT), target=XivCombatant(0x4003B508:Ifrit:NPC at Pos(106.03, 97.18, -0.00 : 2.21) NPC 8730:1185), stacks=0, isRefresh=false} |
				call(237484, "Raidwide", "Raidwide (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@68af8236 |
				call(246845, "Searing Wind on YOU", "Searing Wind on YOU (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@3e6c28e5 |
				call(249695, "", "Searing Wind on YOU (30.0)"), // from BuffApplied{buff=XivStatusEffect(0x62A:Searing Wind), duration=PT30S, source=XivCombatant(0x4003B508:Ifrit:NPC at Pos(102.46, 102.53, 0.00 : -2.87) NPC 8730:1185), target=XivPlayerCharacter(0x10669D22:Scholar Player, SCH, TODO, 70, true), stacks=0, isRefresh=false} |
				call(264946, "Searing Wind on WAR Main", "Searing Wind on WAR Main (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@c94c949 |
				call(267799, "", "Searing Wind on WAR Main (30.0)"), // from BuffApplied{buff=XivStatusEffect(0x62A:Searing Wind), duration=PT30S, source=XivCombatant(0x4003B508:Ifrit:NPC at Pos(108.45, 108.78, 0.00 : 0.71) NPC 8730:1185), target=XivPlayerCharacter(0x10026D97:WAR Main, WAR, TODO, 70, false), stacks=0, isRefresh=false} |
				call(271986, "Stack on DRG Player", "Stack on DRG Player"), // from HeadMarkerEvent(117 on XivPlayerCharacter(0x10679943:DRG Player, DRG, TODO, 70, false)) |
				call(26644, "Mistral on you", "Mistral on you"), // from HeadMarkerEvent(16 on XivPlayerCharacter(0x10669D22:Scholar Player, SCH, TODO, 70, true)) |
				call(26734, "Slipstream", "Slipstream (2.2)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@39f4245 |
				call(42068, "Slipstream", "Slipstream (2.2)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@17b5de32 |
				call(70127, "Stack", "Stack (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@48c1f721 |
				call(76225, "Stack", "Stack (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@62b5a1f1 |
				call(94632, "Raidwide", "Raidwide (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@1b415a88 |
				call(140240, "Slipstream", "Slipstream (2.2)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@22bc439c |
				call(147683, "Garuda Woken", "Garuda Woken"), // from BuffApplied{buff=XivStatusEffect(0x5F9:Woken), duration=indef, source=XivEntity(ENVIRONMENT), target=XivCombatant(0x4003B7FF:Garuda:NPC at Pos(99.99, 98.80, -0.00 : 3.01) NPC 8722:1644), stacks=0, isRefresh=false} |
				call(170103, "Raidwide", "Raidwide (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@6583c015 |
				call(203734, "Searing Wind on YOU", "Searing Wind on YOU (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@2a765f7d |
				call(206587, "", "Searing Wind on YOU (18.0)"), // from BuffApplied{buff=XivStatusEffect(0x62A:Searing Wind), duration=PT18S, source=XivCombatant(0x4003B7FE:Ifrit:NPC at Pos(102.19, 95.14, -0.00 : -0.37) NPC 8730:1185), target=XivPlayerCharacter(0x10669D22:Scholar Player, SCH, TODO, 70, true), stacks=0, isRefresh=false} |
				call(224165, "Ifrit Woken", "Ifrit Woken"), // from BuffApplied{buff=XivStatusEffect(0x5F9:Woken), duration=indef, source=XivEntity(ENVIRONMENT), target=XivCombatant(0x4003B7FE:Ifrit:NPC at Pos(102.19, 95.14, -0.00 : 2.99) NPC 8730:1185), stacks=0, isRefresh=false} |
				call(233121, "Raidwide", "Raidwide (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@3b72d5a1 |
				call(242523, "Searing Wind on Astro Player", "Searing Wind on Astro Player (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@7b12c1f0 |
				call(245373, "", "Searing Wind on Astro Player (30.0)"), // from BuffApplied{buff=XivStatusEffect(0x62A:Searing Wind), duration=PT30S, source=XivCombatant(0x4003B7FE:Ifrit:NPC at Pos(106.25, 94.32, -0.00 : -0.80) NPC 8730:1185), target=XivPlayerCharacter(0x10694638:Astro Player, AST, TODO, 70, false), stacks=0, isRefresh=false} |
				call(260624, "Searing Wind on YOU", "Searing Wind on YOU (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@ea25ef |
				call(263477, "", "Searing Wind on YOU (30.0)"), // from BuffApplied{buff=XivStatusEffect(0x62A:Searing Wind), duration=PT30S, source=XivCombatant(0x4003B7FE:Ifrit:NPC at Pos(109.09, 91.60, -0.00 : 2.37) NPC 8730:1185), target=XivPlayerCharacter(0x10669D22:Scholar Player, SCH, TODO, 70, true), stacks=0, isRefresh=false} |
				call(267666, "Stack on SMN Player", "Stack on SMN Player"), // from HeadMarkerEvent(117 on XivPlayerCharacter(0x106AC23F:SMN Player, SMN, TODO, 70, false)) |
				call(278582, "Raidwide", "Raidwide (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@18ff1f38 |
				call(311427, "Right", "Right"), // from gg.xp.xivsupport.events.actlines.events.TickEvent@350d0e5b |
				call(341477, "Titan Woken", "Titan Woken"), // from BuffApplied{buff=XivStatusEffect(0x5F9:Woken), duration=indef, source=XivEntity(ENVIRONMENT), target=XivCombatant(0x4003B7FA:Titan:NPC at Pos(100.54, 113.85, 0.00 : 1.81) NPC 8727:1801), stacks=0, isRefresh=false} |
				call(461090, "Ultima", "Ultima (4.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@58fd444b |
				call(515376, "Homing Laser on GUB Player", "Homing Laser on GUB Player (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@969d7e6 |
				call(556838, "Ifrit, Garuda, Titan", "Ifrit → Garuda → Titan"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@690f0f2c |
				call(567954, "Garuda", "Garuda → Titan"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@34adab80 |
				call(574186, "Back then Right"),
				call(584340, "Titan", "Titan"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@6c07e42 |
				call(597623, "Homing Laser on WAR Main", "Homing Laser on WAR Main (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@41514f9b |
				call(616540, "Stack on NIN Main", "Stack on NIN Main"), // from HeadMarkerEvent(117 on XivPlayerCharacter(0x1082C4CB:NIN Main, NIN, TODO, 70, false)) |
				call(623765, "Searing Wind on YOU", "Searing Wind on YOU (1.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@50365ef2 |
				call(626619, "", "Searing Wind on YOU (30.0)"), // from BuffApplied{buff=XivStatusEffect(0x62A:Searing Wind), duration=PT30S, source=XivCombatant(0x4003B7FE:Ifrit:NPC at Pos(113.69, 113.69, 0.00 : -2.25) NPC 8730:1185), target=XivPlayerCharacter(0x10669D22:Scholar Player, SCH, TODO, 70, true), stacks=0, isRefresh=false} |
				call(657514, "Homing Laser on GUB Player", "Homing Laser on GUB Player (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@91e0edf |
				call(684587, "Left Laser", "Left Laser (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@c2943d0 |
				call(688732, "Middle Laser", "Middle Laser (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@47cfe859 |
				call(692877, "Middle Laser", "Middle Laser (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@25ecef9c |
				call(693635, "Stack on BRD Player", "Stack on BRD Player"), // from HeadMarkerEvent(117 on XivPlayerCharacter(0x105E0EF5:BRD Player, BRD, TODO, 70, false)) |
				call(710433, "Ultima", "Ultima (4.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@5cfd6508 |
				call(748917, "Garuda, Ifrit, Titan", "Garuda → Ifrit → Titan"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@1a0c8a84 |
				call(762015, "Raidwide", "Raidwide (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@22b924bc |
				call(769063, "Ifrit", "Ifrit → Titan"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@60ebbf78 |
				call(783248, "Raidwide", "Raidwide (2.7)"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@57095348 |
				call(789178, "Titan", "Titan"), // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@292fbb87 |
				call(802432, "Raidwide", "Raidwide (2.7)") // from gg.xp.xivsupport.events.actlines.events.AbilityCastStart@4258f6da |
		);
	}
}
