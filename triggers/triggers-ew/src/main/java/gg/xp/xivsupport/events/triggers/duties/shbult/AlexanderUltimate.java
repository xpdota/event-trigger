package gg.xp.xivsupport.events.triggers.duties.shbult;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.events.triggers.support.PlayerStatusCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AlexanderUltimate extends AutoChildEventHandler implements FilteredEventHandler {

    private static final Logger log = LoggerFactory.getLogger(AlexanderUltimate.class);

    //P1: LL

    //P2: BJCC

    //P3: Alex

    //P4: Perfect
    @PlayerStatusCallout(0x869)
    private final ModifiableCallout<BuffApplied> lightBeacon = new ModifiableCallout<BuffApplied>("Light Beacon", "Light Beacon").autoIcon();
    @PlayerStatusCallout(0x86B)
    private final ModifiableCallout<BuffApplied> darkBeacon = new ModifiableCallout<BuffApplied>("Dark Beacon", "Dark Beacon").autoIcon();
    @PlayerStatusCallout(0x868)
    private final ModifiableCallout<BuffApplied> lightPlayer = new ModifiableCallout<BuffApplied>("Dark", "Dark").autoIcon();
    @PlayerStatusCallout(0x86A)
    private final ModifiableCallout<BuffApplied> darkPlayer = new ModifiableCallout<BuffApplied>("Light", "Light").autoIcon();
    @NpcCastCallout(0x487E)
    private final ModifiableCallout<AbilityCastStart> motion = ModifiableCallout.durationBasedCall("Ordained Motion", "Motion");
    @NpcCastCallout(0x487F)
    private final ModifiableCallout<AbilityCastStart> stillness = ModifiableCallout.durationBasedCall("Ordained Stillness", "Stillness");
    @NpcCastCallout(0x488A)
    private final ModifiableCallout<AbilityCastStart> opticalSightIndividual = ModifiableCallout.durationBasedCall("Optical Sight: Individual", "Spread");
    @NpcCastCallout(0x488B)
    private final ModifiableCallout<AbilityCastStart> opticalSightCollective = ModifiableCallout.durationBasedCall("Optical Sight: Collective", "Stacks");


    private final XivState state;
    private final StatusEffectRepository buffs;
    private final ActiveCastRepository casts;

    public AlexanderUltimate(XivState state, StatusEffectRepository buffs, ActiveCastRepository casts) {
        this.state = state;
        this.buffs = buffs;
        this.casts = casts;
    }

    @Override
    public boolean enabled(EventContext context) {
        return state.dutyIs(KnownDuty.TEA);
    }

    private XivState getState() {
        return state;
    }

    private StatusEffectRepository getBuffs() {
        return buffs;
    }

    private final ModifiableCallout<?> fateAStack = new ModifiableCallout<>("Fate Cal A: Stack", "Stack");
    private final ModifiableCallout<?> fateADefa = new ModifiableCallout<>("Fate Cal A: Defamation", "Defamation");
    private final ModifiableCallout<?> fateASpread = new ModifiableCallout<>("Fate Cal A: Severity", "Don't Stack");
    private final ModifiableCallout<?> fateANothing = new ModifiableCallout<>("Fate Cal A: Nothing", "Nothing, Stack");

    private final ModifiableCallout<?> fateAmotion1 = new ModifiableCallout<>("Fate Cal A: Motion 1", "Motion first");
    private final ModifiableCallout<?> fateAmotion2 = new ModifiableCallout<>("Fate Cal A: Motion 2", "Motion second");
    private final ModifiableCallout<?> fateAstill1 = new ModifiableCallout<>("Fate Cal A: Stillness 1", "Stillness first");
    private final ModifiableCallout<?> fateAstill2 = new ModifiableCallout<>("Fate Cal A: Stillness 2", "Stillness second");

    private final ModifiableCallout<?> fateAleftSafe = new ModifiableCallout<>("Fate Cal A: Left Safe", "Party left, Defamation right");
    private final ModifiableCallout<?> fateArightSafe = new ModifiableCallout<>("Fate Cal A: Right Safe", "Party Right, Defamation left");

    @AutoFeed
    private final SequentialTrigger<BaseEvent> fateCalA = SqtTemplates.sq(60_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(0x487B), //Fate projection cast
            (e1, s) -> {
                //4B0D motion1 telegraph
                //4B0E still1 telegraph
                //4899 motion2 telegraph
                //489A still2 telegraph
                //489F sacrament telegraph
                List<TetherEvent> cloneTethers = s.waitEvents(8, TetherEvent.class, te -> te.tetherIdMatches(0x62));
                cloneTethers.sort((t1, t2) -> {
                    XivCombatant clone1 = t1.getTargetMatching(cbt -> !cbt.isPc());
                    XivCombatant clone2 = t2.getTargetMatching(cbt -> !cbt.isPc());
                    return clone2.getId() > clone1.getId() ? 1: -1; //Sorts 8 7 6 5 etc
                });
                Optional<TetherEvent> yourTether = cloneTethers.stream().filter(te -> te.eitherTargetMatches(getState().getPlayer())).findFirst();
                if (yourTether.isPresent()) {
                    XivCombatant yourClone = yourTether.get().getTargetMatching(cbt -> !cbt.isThePlayer());
                    int cloneIndex = cloneTethers.indexOf(yourTether.get());
                    switch(cloneIndex) {
                        case 0 -> s.accept(fateAStack.getModified());
                        case 1 -> s.accept(fateADefa.getModified());
                        case 2, 3, 4 -> s.accept(fateASpread.getModified());
                        case 5, 6, 7 -> s.accept(fateANothing.getModified());
                        default -> log.info("fateCalA: Couldn't find clone (or index)");
                    }
                } else {
                    log.info("fateCalA: Couldn't find your tether");
                }

                AbilityUsedEvent ordained1 = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x4B0D, 0x4B0E));
                if(ordained1.abilityIdMatches(0x4B0D)) {
                    s.accept(fateAmotion1.getModified());
                } else {
                    s.accept(fateAstill1.getModified());
                }
                s.waitMs(200);

                AbilityUsedEvent ordained2 = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x4899, 0x489A));
                if(ordained2.abilityIdMatches(0x4899)) {
                    s.accept(fateAmotion2.getModified());
                } else {
                    s.accept(fateAstill2.getModified());
                }

                //Taken from cactbot
                // Alexanders from left to right are:
                // 0: 78.28883, 91.00694 (~-67 degrees from north)
                // 1: 91.00694, 78.28883 (~-22 degrees from north)
                // 2: 108.9931, 78.28883 (~+22 degrees from north)
                // 3: 121.7112, 91.00694 (~+67 degrees from north)
                // center: 100, 100 (with +x = east and +y = south)
                List<AbilityUsedEvent> lasers = s.waitEvents(3, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x489F));
                Optional<AbilityUsedEvent> centerAlexCast = lasers.stream().filter(aue -> aue.getSource().getPos().y() < 90).findFirst(); //Finds alex at 78, skips alexs at 91
                if(centerAlexCast.isPresent()) {
                    XivCombatant centerAlex = centerAlexCast.get().getSource();
                    boolean isLeftAlex = centerAlex.getPos().x() < 100;
                    if(isLeftAlex) { //Left safe, right for defa
                        s.accept(fateAleftSafe.getModified());
                    } else { //Right safe, left for defa
                        s.accept(fateArightSafe.getModified());
                    }
                } else {
                    log.info("fateCalA: Couldn't find the casting center alex");
                }
            });
}
