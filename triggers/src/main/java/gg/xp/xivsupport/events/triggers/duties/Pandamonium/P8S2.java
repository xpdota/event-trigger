package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.models.ArenaPos;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

// TODO: this is causing there to be a second arena positions tab
@CalloutRepo(name = "P8S Final Boss", duty = KnownDuty.P8S)
public class P8S2 extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P8S2.class);
	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	private final ModifiableCallout<AbilityCastStart> tyrantsUnholyDarkness = ModifiableCallout.durationBasedCall("Tyrant's Unholy Darkness", "Split Buster");
	private final ModifiableCallout<AbilityCastStart> aioniopyr = ModifiableCallout.durationBasedCall("Aioniopyr", "Raidwide with Bleed");
	private final ModifiableCallout<AbilityCastStart> eastCleave = ModifiableCallout.durationBasedCall("Cleaving East", "Left");
	private final ModifiableCallout<AbilityCastStart> westCleave = ModifiableCallout.durationBasedCall("Cleaving West", "Right");
	private final ModifiableCallout<BuffApplied> hc1shortAlpha = new ModifiableCallout<>("HC1: Short Alpha", "Alpha Defamation");
	private final ModifiableCallout<BuffApplied> hc1shortBeta = new ModifiableCallout<>("HC1: Short Beta", "Beta Defamation");
	private final ModifiableCallout<BuffApplied> hc1shortGamma = new ModifiableCallout<>("HC1: Short Gamma", "Gamma Defamation");
	private final ModifiableCallout<BuffApplied> hc1longAlpha = new ModifiableCallout<>("HC1: Long Alpha", "Long Alpha - Avoid Defamation");
	private final ModifiableCallout<BuffApplied> hc1longBeta = new ModifiableCallout<>("HC1: Long Beta", "Long Beta - Avoid Defamation");
	private final ModifiableCallout<BuffApplied> hc1longGamma = new ModifiableCallout<>("HC1: Long Gamma", "Long Gamma - Avoid Defamation");
	private final ModifiableCallout<BuffApplied> hc1multiSplice = new ModifiableCallout<>("HC1: Multisplice", "2-Stack");
	private final ModifiableCallout<BuffApplied> hc1superSplice = new ModifiableCallout<>("HC1: Supersplice", "3-Stack");
	private final ModifiableCallout<?> hc1shortAlphaFup = new ModifiableCallout<>("HC1: Short Alpha Followup", "Avoid Defamation");
	private final ModifiableCallout<?> hc1shortBetaFup = new ModifiableCallout<>("HC1: Short Beta Followup", "Avoid Defamation");
	private final ModifiableCallout<?> hc1shortGammaFup = new ModifiableCallout<>("HC1: Short Gamma Followup", "Avoid Defamation");
	private final ModifiableCallout<?> hc1longAlphaFup = new ModifiableCallout<>("HC1: Long Alpha Followup", "Alpha Defamation");
	private final ModifiableCallout<?> hc1longBetaFup = new ModifiableCallout<>("HC1: Long Beta Followup", "Beta Defamation");
	private final ModifiableCallout<?> hc1longGammaFup = new ModifiableCallout<>("HC1: Long Gamma Followup", "Gamma Defamation");
	private final ModifiableCallout<?> hc1multiSpliceFup = new ModifiableCallout<>("HC1: Multisplice Followup",  "2-Stack");
	private final ModifiableCallout<?> hc1superSpliceFup = new ModifiableCallout<>("HC1: Supersplice Followup",  "3-Stack");
	private final ModifiableCallout<?> hc1doAlchemy = new ModifiableCallout<>("HC1: Do Alchemy", "Alch if tower is {tower1} or {tower2}");
	private final ModifiableCallout<?> hc1dontDoAlchemy = new ModifiableCallout<>("HC1: Don't Do Alchemy", "Avoid Alchemy");
	private final ModifiableCallout<?> hc1doTower = new ModifiableCallout<>("HC1: Do Tower", "Soak Tower if {tower1} or {tower2}");
	private final ModifiableCallout<?> hc1dontDoTower = new ModifiableCallout<>("HC1: Don't Do Tower", "Avoid Tower");
	private final ModifiableCallout<?> hc1doSecondAlch = new ModifiableCallout<>("HC1: Do Second Alchemy", "Do Alchemy");
	private final ModifiableCallout<?> hc1dontDoSecondAlch = new ModifiableCallout<>("HC1: Don't Do Second Alchemy", "Avoid Alchemy");
	private final ModifiableCallout<?> hc1doSecondTowers = new ModifiableCallout<>("HC1: Do Second Tower", "Soak Tower");
//				if (alchNow) {
//		s.updateCall(hc1doSecondAlch.getModified());
//		s.waitMs(2_000);
//		s.updateCall(hc1doSecondTowers.getModified());
//	}
//				else {
//		s.updateCall(hc1dontDoSecondAlch.getModified());
//		s.waitMs(2_000);
//		s.updateCall(hc1dontDoSecondTowers.getModified());
//	}

	public P8S2(XivState state, ActiveCastRepository acr, StatusEffectRepository buffs) {
		this.state = state;
		this.acr = acr;
		this.buffs = buffs;
	}

	private final XivState state;

	private XivState getState() {
		return this.state;
	}

	private final ActiveCastRepository acr;

	private ActiveCastRepository getAcr() {
		return acr;
	}

	private final StatusEffectRepository buffs;

	private StatusEffectRepository getBuffs() {
		return buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P8S);
	}

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		int id = (int) event.getAbility().getId();
		ModifiableCallout<AbilityCastStart> call;
		switch (id) {
			// TODO: figure out correct IDs
			case 31197, 31198 -> call = tyrantsUnholyDarkness; // split buster, no bleed
			case 31199 -> call = aioniopyr; // raidwide+bleed
			case 0x79D7 -> call = westCleave;
			case 0x79D8 -> call = eastCleave;

			default -> {
				return;
			}
		}
		context.accept(call.getModified(event));
	}

	private enum TowerColor {
		Blue,
		Purple,
		Green
	}

	// red
	private static final int impAlpha = 0xD02;
	// yellow
	private static final int impBeta = 0xD03;
	// orange
	private static final int impGamma = 0xD04;
	// green on red
	private static final int perfAlpha = 0xD05;
	// yellow on yellow
	private static final int perfBeta = 0xD06;
	// purple on orange
	private static final int perfGamma = 0xD07;
	// 'no' sign
	private static final int inconceivable = 0xD08;
	// red DNA
	private static final int solosplice = 0xD11;
	// yellow DNA
	private static final int multisplice = 0xD12;
	// blue DNA
	private static final int supersplice = 0xD13;

	@AutoFeed
	private final SequentialTrigger<BaseEvent> highConcept1 = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(28938),
			(e1, s) -> {
				BuffApplied playerBuff = s.waitEvent(BuffApplied.class, ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(impAlpha, impBeta, impGamma, supersplice, multisplice));
				long seconds = playerBuff.getInitialDuration().toSeconds();
				ModifiableCallout<BuffApplied> initialCall;
				ModifiableCallout<?> followupCall;
				boolean maybeDoingTower = false;
				boolean doFinalTower = false;
				@Nullable TowerColor neededTowerColor1 = null;
				@Nullable TowerColor neededTowerColor2 = null;
				switch ((int) playerBuff.getBuff().getId()) {
					case impAlpha -> {
						if (seconds < 15) {
							initialCall = hc1shortAlpha;
							followupCall = hc1shortAlphaFup;
							maybeDoingTower = true;
							neededTowerColor1 = TowerColor.Blue;
							neededTowerColor2 = TowerColor.Green;
						}
						else {
							initialCall = hc1longAlpha;
							followupCall = hc1longAlphaFup;
						}
					}
					case impBeta -> {
						if (seconds < 15) {
							initialCall = hc1shortBeta;
							followupCall = hc1shortBetaFup;
							maybeDoingTower = true;
							neededTowerColor1 = TowerColor.Purple;
							neededTowerColor2 = TowerColor.Green;
						}
						else {
							initialCall = hc1longBeta;
							followupCall = hc1longBetaFup;
						}
					}
					case impGamma -> {
						if (seconds < 15) {
							initialCall = hc1shortGamma;
							followupCall = hc1shortGammaFup;
							maybeDoingTower = true;
							neededTowerColor1 = TowerColor.Purple;
							neededTowerColor2 = TowerColor.Blue;
						}
						else {
							initialCall = hc1longGamma;
							followupCall = hc1longGammaFup;
						}
					}
					case multisplice -> {
						initialCall = hc1multiSplice;
						followupCall = hc1multiSpliceFup;
					}
					case supersplice -> {
						initialCall = hc1superSplice;
						followupCall = hc1superSpliceFup;
					}
					default -> {
						log.warn("HC1: Unknown debuff {}", playerBuff);
						return;
					}
				}
				s.updateCall(initialCall.getModified(playerBuff));
				s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(inconceivable));
				if (maybeDoingTower) {
					s.updateCall(hc1doAlchemy.getModified(Map.of("color1", neededTowerColor1, "color2", neededTowerColor2)));
				}
				else {
					s.updateCall(hc1dontDoAlchemy.getModified());
				}
				s.waitMs(5_000);
				if (maybeDoingTower) {
					s.updateCall(hc1doTower.getModified(Map.of("color1", neededTowerColor1, "color2", neededTowerColor2)));
				}
				else {
					s.updateCall(hc1dontDoTower.getModified());
				}
				s.waitMs(5_000);
				String avoidDefa;
				List<BuffApplied> perfectBuffs = getBuffs().getBuffs().stream()
						.filter(ba -> ba.buffIdMatches(perfAlpha, perfBeta, perfGamma))
						.toList();

				if (perfectBuffs.size() != 1) {
					avoidDefa = "Error";
				}
				else {
					switch ((int) perfectBuffs.get(0).getBuff().getId()) {
						case perfAlpha -> avoidDefa = "Alpha";
						case perfBeta -> avoidDefa = "Beta";
						case perfGamma -> avoidDefa = "Gamma";
						default -> avoidDefa = "Error";
					}
				}
				s.updateCall(followupCall.getModified(Map.of("avoidColor", avoidDefa)));
				s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(inconceivable));
				boolean alchNow = getBuffs().statusesOnTarget(getState().getPlayer())
						.stream().anyMatch(ba -> ba.buffIdMatches(perfAlpha, perfBeta, perfGamma));
				if (alchNow) {
					s.updateCall(hc1doSecondAlch.getModified());
				}
				else {
					s.updateCall(hc1dontDoSecondAlch.getModified());
				}
				s.waitMs(2_000);
				s.updateCall(hc1doSecondTowers.getModified());

			});

	/*
	https://discord.com/channels/551474815727304704/968569969103216651/1015795892688797726

	_rsv_28938_-1_1_0_0_SE2DC5B04_EE2DC5B04|High Concept
	_rsv_30996_-1_1_0_0_SE2DC5B04_EE2DC5B04|Conceptual Octaflare
	_rsv_30997_-1_1_0_0_SE2DC5B04_EE2DC5B04|Conceptual Tetraflare
	_rsv_30998_-1_1_0_0_SE2DC5B04_EE2DC5B04|Conceptual Tetraflare
	_rsv_30999_-1_1_0_0_SE2DC5B04_EE2DC5B04|Conceptual Diflare
	_rsv_31000_-1_1_0_0_SE2DC5B04_EE2DC5B04|Emergent Octaflare
	_rsv_31001_-1_1_0_0_SE2DC5B04_EE2DC5B04|Emergent Tetraflare
	_rsv_31003_-1_1_0_0_SE2DC5B04_EE2DC5B04|Emergent Diflare
	_rsv_31005_-1_1_0_0_SE2DC5B04_EE2DC5B04|Octaflare
	_rsv_31006_-1_1_0_0_SE2DC5B04_EE2DC5B04|Tetraflare
	_rsv_31007_-1_1_0_0_SE2DC5B04_EE2DC5B04|Nest of Flamevipers
	_rsv_31008_-1_1_0_0_SE2DC5B04_EE2DC5B04|Nest of Flamevipers
	_rsv_31009_-1_1_0_0_SE2DC5B04_EE2DC5B04|Manifold Flames
	_rsv_31010_-1_1_0_0_SE2DC5B04_EE2DC5B04|Manifold Flames
	_rsv_31021_-1_1_0_0_SE2DC5B04_EE2DC5B04|Eye of the Gorgon
	_rsv_31022_-1_1_0_0_SE2DC5B04_EE2DC5B04|Crown of the Gorgon
	_rsv_31023_-1_1_0_0_SE2DC5B04_EE2DC5B04|Blood of the Gorgon
	_rsv_31024_-1_1_0_0_SE2DC5B04_EE2DC5B04|Breath of the Gorgon
	_rsv_31030_-1_1_0_0_SE2DC5B04_EE2DC5B04|Stomp Dead
	_rsv_31031_-1_1_0_0_SE2DC5B04_EE2DC5B04|Stomp Dead
	_rsv_31032_-1_1_0_0_SE2DC5B04_EE2DC5B04|Blazing Footfalls
	_rsv_31033_-1_1_0_0_SE2DC5B04_EE2DC5B04|Trailblaze
	_rsv_31038_-1_1_0_0_SE2DC5B04_EE2DC5B04|Trailblaze
	_rsv_31040_-1_1_0_0_SE2DC5B04_EE2DC5B04|Rain of Fire
	_rsv_31043_-1_1_0_0_SE2DC5B04_EE2DC5B04|Rain of Fire
	_rsv_31044_-1_1_0_0_SE2DC5B04_EE2DC5B04|Genesis of Flame
	_rsv_31050_-1_1_0_0_SE2DC5B04_EE2DC5B04|Genesis of Flame
	_rsv_31148_-1_1_0_0_SE2DC5B04_EE2DC5B04|High Concept
	_rsv_31149_-1_1_0_0_SE2DC5B04_EE2DC5B04|Conceptual Shift
	_rsv_31150_-1_1_0_0_SE2DC5B04_EE2DC5B04|Conceptual Shift
	_rsv_31151_-1_1_0_0_SE2DC5B04_EE2DC5B04|Conceptual Shift
	_rsv_31152_-1_1_0_0_SE2DC5B04_EE2DC5B04|Conception
	_rsv_31153_-1_1_0_0_SE2DC5B04_EE2DC5B04|Failure of Imagination
	_rsv_31154_-1_1_0_0_SE2DC5B04_EE2DC5B04|Splicer
	_rsv_31155_-1_1_0_0_SE2DC5B04_EE2DC5B04|Splicer
	_rsv_31156_-1_1_0_0_SE2DC5B04_EE2DC5B04|Splicer
	_rsv_31157_-1_1_0_0_SE2DC5B04_EE2DC5B04|Everburn
	_rsv_31158_-1_1_0_0_SE2DC5B04_EE2DC5B04|Arcane Control
	_rsv_31159_-1_1_0_0_SE2DC5B04_EE2DC5B04|Arcane Channel
	_rsv_31160_-1_1_0_0_SE2DC5B04_EE2DC5B04|Arcane Wave
	_rsv_31162_-1_1_0_0_SE2DC5B04_EE2DC5B04|Ego Death
	_rsv_31163_-1_1_0_0_SE2DC5B04_EE2DC5B04|Natural Alignment
	_rsv_31164_-1_1_0_0_SE2DC5B04_EE2DC5B04|Twist Nature
	_rsv_31165_-1_1_0_0_SE2DC5B04_EE2DC5B04|Forcible Trifire
	_rsv_31166_-1_1_0_0_SE2DC5B04_EE2DC5B04|Forcible Difreeze
	_rsv_31167_-1_1_0_0_SE2DC5B04_EE2DC5B04|Forcible Fire III
	_rsv_31168_-1_1_0_0_SE2DC5B04_EE2DC5B04|Forcible Fire II
	_rsv_31169_-1_1_0_0_SE2DC5B04_EE2DC5B04|Forcible Fire IV
	_rsv_31170_-1_1_0_0_SE2DC5B04_EE2DC5B04|Inverse Magicks
	_rsv_31171_-1_1_0_0_SE2DC5B04_EE2DC5B04|Fates Unending
	_rsv_31172_-1_1_0_0_SE2DC5B04_EE2DC5B04|Unsightly Chaos
	_rsv_31173_-1_1_0_0_SE2DC5B04_EE2DC5B04|Unsightly Chaos
	_rsv_31176_-1_1_0_0_SE2DC5B04_EE2DC5B04|Illusory Soul
	_rsv_31177_-1_1_0_0_SE2DC5B04_EE2DC5B04|Soul Strand
	_rsv_31178_-1_1_0_0_SE2DC5B04_EE2DC5B04|Soul Strand
	_rsv_31179_-1_1_0_0_SE2DC5B04_EE2DC5B04|Soul Strand
	_rsv_31180_-1_1_0_0_SE2DC5B04_EE2DC5B04|End of the End
	_rsv_31181_-1_1_0_0_SE2DC5B04_EE2DC5B04|Dirge of Second Death
	_rsv_31182_-1_1_0_0_SE2DC5B04_EE2DC5B04|Limitless Kindling
	_rsv_31184_-1_1_0_0_SE2DC5B04_EE2DC5B04|Somatic Dirge of Second Death
	_rsv_31185_-1_1_0_0_SE2DC5B04_EE2DC5B04|Somatic Tyrant's Fire III
	_rsv_31186_-1_1_0_0_SE2DC5B04_EE2DC5B04|Somatic Dirge of Second Death
	_rsv_31187_-1_1_0_0_SE2DC5B04_EE2DC5B04|Somatic Tyrant's Fire III
	_rsv_31188_-1_1_0_0_SE2DC5B04_EE2DC5B04|Dead Outside
	_rsv_31193_-1_1_0_0_SE2DC5B04_EE2DC5B04|Dominion
	_rsv_31194_-1_1_0_0_SE2DC5B04_EE2DC5B04|Orogenic Annihilation
	_rsv_31195_-1_1_0_0_SE2DC5B04_EE2DC5B04|Orogenic Deformation
	_rsv_31196_-1_1_0_0_SE2DC5B04_EE2DC5B04|Orogenic Shift
	_rsv_31197_-1_1_0_0_SE2DC5B04_EE2DC5B04|Tyrant's Unholy Darkness
	_rsv_31198_-1_1_0_0_SE2DC5B04_EE2DC5B04|Tyrant's Unholy Darkness

	_rsv_31199_-1_1_0_0_SE2DC5B04_EE2DC5B04|Aioniopyr
	_rsv_31204_-1_1_0_0_SE2DC5B04_EE2DC5B04|Ego Death
	_rsv_31205_-1_1_0_0_SE2DC5B04_EE2DC5B04|Somatic End of the End
	_rsv_31210_-1_1_0_0_SE2DC5B04_EE2DC5B04|Ektothermos
	_rsv_31266_-1_1_0_0_SE2DC5B04_EE2DC5B04|Aionagonia
	_rsv_31366_-1_1_0_0_SE2DC5B04_EE2DC5B04|Ego Death
	_rsv_31367_-1_1_0_0_SE2DC5B04_EE2DC5B04|Ego Death
	_rsv_28938_-1_1_0_1_SE2DC5B04_EE2DC5B04|High Concept
	_rsv_30996_-1_1_0_1_SE2DC5B04_EE2DC5B04|Conceptual Octaflare
	_rsv_30997_-1_1_0_1_SE2DC5B04_EE2DC5B04|Conceptual Tetraflare
	_rsv_30998_-1_1_0_1_SE2DC5B04_EE2DC5B04|Conceptual Tetraflare
	_rsv_30999_-1_1_0_1_SE2DC5B04_EE2DC5B04|Conceptual Diflare
	_rsv_31000_-1_1_0_1_SE2DC5B04_EE2DC5B04|Emergent Octaflare
	_rsv_31001_-1_1_0_1_SE2DC5B04_EE2DC5B04|Emergent Tetraflare
	_rsv_31003_-1_1_0_1_SE2DC5B04_EE2DC5B04|Emergent Diflare
	_rsv_31005_-1_1_0_1_SE2DC5B04_EE2DC5B04|Octaflare
	_rsv_31006_-1_1_0_1_SE2DC5B04_EE2DC5B04|Tetraflare
	_rsv_31007_-1_1_0_1_SE2DC5B04_EE2DC5B04|Nest of Flamevipers
	_rsv_31008_-1_1_0_1_SE2DC5B04_EE2DC5B04|…earby influences when this effect expires. // this one clearly failed

	_rsv_3347_-1_1_0_1_S74CFC3B0_E74CFC3B0|Supersplice
	_rsv_3347_-1_1_1_1_S74CFC3B0_E74CFC3B0|Self-concept is being warped beyond recognition, resulting in an adverse reaction determined by nearby influences when this effect expires.
	_rsv_3349_-1_1_0_1_S74CFC3B0_E74CFC3B0|Inverse Magicks
	_rsv_3349_-1_1_1_1_S74CFC3B0_E74CFC3B0|The order of forcible magicks to be cast is inverted.
	_rsv_3350_-1_1_0_1_S74CFC3B0_E74CFC3B0|Soul Stranded
	_rsv_3350_-1_1_1_1_S74CFC3B0_E74CFC3B0|Physical and spiritual forms have been separated.
	_rsv_3351_-1_1_0_1_S74CFC3B0_E74CFC3B0|Eye of the Gorgon
	_rsv_3351_-1_1_1_1_S74CFC3B0_E74CFC3B0|Cursed to unleash a petrifying attack in the direction of gaze when this effect expires.
	_rsv_3352_-1_1_0_1_S74CFC3B0_E74CFC3B0|Crown of the Gorgon
	_rsv_3352_-1_1_1_1_S74CFC3B0_E74CFC3B0|Cursed to unleash a petrifying light upon those nearby when this effect expires.
	_rsv_3406_-1_1_0_1_S74CFC3B0_E74CFC3B0|Everburn
	_rsv_3406_-1_1_1_1_S74CFC3B0_E74CFC3B0|Calling upon the power of a Phoenix concept. Damage dealt is increased.
	_rsv_3412_-1_1_0_1_S74CFC3B0_E74CFC3B0|Natural Alignment
	_rsv_3412_-1_1_1_1_S74CFC3B0_E74CFC3B0|Graven with a sigil and sustaining damage over time. Taking damage from certain actions caused by Twist Nature will result in a destructive forcible failure.
	 */

}