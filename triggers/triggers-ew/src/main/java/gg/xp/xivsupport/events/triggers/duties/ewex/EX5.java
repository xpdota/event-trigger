package gg.xp.xivsupport.events.triggers.duties.ewex;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.*;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.util.RepeatSuppressor;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Triggers originally written by Iminha
 */
@CalloutRepo(name = "Rubicante Extreme", duty = KnownDuty.RubicanteEx)
public class EX5 extends AutoChildEventHandler implements FilteredEventHandler {

    private static final Logger log = LoggerFactory.getLogger(EX5.class);

    private final ModifiableCallout<AbilityCastStart> inferno = ModifiableCallout.durationBasedCall("Inferno", "Raidwide");
    private final ModifiableCallout<AbilityCastStart> infernoWinged = ModifiableCallout.durationBasedCall("Inferno: Spread", "Spread with bleed");
    private final ModifiableCallout<AbilityCastStart> blazingRapture = ModifiableCallout.durationBasedCall("Blazing Rapture", "Heavy raidwide");
    private final ModifiableCallout<AbilityCastStart> shatteringHeat = ModifiableCallout.durationBasedCall("Shattering Heat", "Buster on {event.target}");
    private final ModifiableCallout<AbilityCastStart> scaldingSignal = ModifiableCallout.durationBasedCall("Scalding Signal", "Out, spread");
    private final ModifiableCallout<AbilityCastStart> scaldingRing = ModifiableCallout.durationBasedCall("Scalding Ring", "In, spread");
    private final ModifiableCallout<AbilityCastStart> awayfrom = ModifiableCallout.durationBasedCall("Something", "Away from {clock}");
    private final ModifiableCallout<AbilityCastStart> nextto = ModifiableCallout.durationBasedCall("Something", "Near {clock}");
    private final ModifiableCallout<AbilityCastStart> dualfire = ModifiableCallout.durationBasedCall("Dualfire", "Double buster");
    private final ModifiableCallout<AbilityCastStart> archInferno = ModifiableCallout.durationBasedCall("Arch Inferno", "Light parties");
    private final ModifiableCallout<AbilityCastStart> spikeOfFlame = ModifiableCallout.durationBasedCall("Spike of Flame", "Spread");
    private final ModifiableCallout<AbilityCastStart> twinfoldFLame = ModifiableCallout.durationBasedCall("Twinfold Flame", "Partners");
    private final ModifiableCallout<AbilityCastStart> fourfoldFlame = ModifiableCallout.durationBasedCall("Fourfold Flame", "Healer stacks");
    private final ModifiableCallout<HeadMarkerEvent> limitCutNumber = new ModifiableCallout<>("Limit Cut number", "{number}", 20_000);
    private final ModifiableCallout<AbilityCastStart> radialFlagration = ModifiableCallout.durationBasedCall("Radial Flagration", "Proteans");
    private final ModifiableCallout<TetherEvent> ghastlyWind = new ModifiableCallout<>("Ghastly Wind", "Point tether out");
    private final ModifiableCallout<TetherEvent> shatteringHeatTether = new ModifiableCallout<>("Shattering Heat tether", "Tank tether on YOU");
    private final ModifiableCallout<AbilityCastStart> sweepingImmolationSpread = ModifiableCallout.durationBasedCall("Sweeping Immolation: Spread", "Behind and Spread");
    private final ModifiableCallout<AbilityCastStart> sweepingImmolationStack = ModifiableCallout.durationBasedCall("Sweeping Immolatiom: Stack", "Behind and Stack");

    private final ModifiableCallout<BuffApplied> flamespireOut = new ModifiableCallout<>("Flamespire Brand: Flare", "Flare, out soon");
    private final ModifiableCallout<BuffApplied> flamespireIn = new ModifiableCallout<>("Flamespire Brand: Nothing", "Stack middle soon");
    private final ModifiableCallout<BuffApplied> flamespireSpread = new ModifiableCallout<>("Flamespire Brand: Spread", "Spread");
    private final ModifiableCallout<BuffApplied> flamespireOutME = new ModifiableCallout<>("Flamespire Brand: Flare + Safety", "Flare, out soon, {safe} safe");
    private final ModifiableCallout<BuffApplied> flamespireInME = new ModifiableCallout<>("Flamespire Brand: Nothing + Safety", "Stack middle soon, {safe} safe");

    private final ModifiableCallout<HeadMarkerEvent> limitCut1= new ModifiableCallout<>("Limit Cut: 1", "1", 20_000);
    private final ModifiableCallout<HeadMarkerEvent> limitCut2= new ModifiableCallout<>("Limit Cut: 2", "2", 20_000);
    private final ModifiableCallout<HeadMarkerEvent> limitCut3= new ModifiableCallout<>("Limit Cut: 3", "3", 20_000);
    private final ModifiableCallout<HeadMarkerEvent> limitCut4= new ModifiableCallout<>("Limit Cut: 4", "4", 20_000);
    private final ModifiableCallout<HeadMarkerEvent> limitCut5= new ModifiableCallout<>("Limit Cut: 5", "5", 20_000);
    private final ModifiableCallout<HeadMarkerEvent> limitCut6= new ModifiableCallout<>("Limit Cut: 6", "6", 20_000);
    private final ModifiableCallout<HeadMarkerEvent> limitCut7= new ModifiableCallout<>("Limit Cut: 7", "7", 20_000);
    private final ModifiableCallout<HeadMarkerEvent> limitCut8= new ModifiableCallout<>("Limit Cut: 8", "8", 20_000);

    public EX5(XivState state, StatusEffectRepository buffs) {
        this.state = state;
        this.buffs = buffs;
    }

    private final XivState state;
    private final StatusEffectRepository buffs;

    private XivState getState() {
        return this.state;
    }

    private StatusEffectRepository getBuffs() {
        return this.buffs;
    }

    @Override
    public boolean enabled(EventContext context) {
        return state.dutyIs(KnownDuty.RubicanteEx);
    }

    private Long firstHeadmark;

    public int getHeadmarkerOffset(HeadMarkerEvent event) {
        if (firstHeadmark == null) {
            firstHeadmark = event.getMarkerId();
        }

        return (int) (event.getMarkerId() - firstHeadmark);
    }

    @HandleEvents
    public void reset(EventContext context, PullStartedEvent event) {
        firstHeadmark = null;
        marksOfPurgatory = null;
        rubicante = null;
        circlesOfHell = null;
        innerCircle = null;
        middleCircle = null;
        outerCircle = null;
    }

    @HandleEvents(order = -50_000)
    public void headmarkSolver(EventContext context, HeadMarkerEvent event) {
        getHeadmarkerOffset(event);
    }

    private final RepeatSuppressor repeatSuppressor = new RepeatSuppressor(Duration.ofMillis(200));

    //7f6d auto
    //8024 -> used event when he starts ordeal
    //80E9 ordeal of purgation
    @HandleEvents
    public void abilityCast(EventContext context, AbilityCastStart event) {
        int id = (int) event.getAbility().getId();
        final ModifiableCallout<AbilityCastStart> call;
        switch (id) {
            case 0x7D2C -> call = inferno;
            case 0x7CF9 -> call = archInferno;
            case 0x7D0F -> call = infernoWinged;
            case 0x7D07 -> call = blazingRapture;
            case 0x7D25 -> call = scaldingRing;
            case 0x7D24 -> call = scaldingSignal;
            case 0x7D2E -> call = dualfire;
            case 0x7D2D -> call = shatteringHeat;
            case 0x7CFE -> call = radialFlagration;
            case 0x7D20 -> call = sweepingImmolationSpread;
            case 0x7D21 -> call = sweepingImmolationStack;
            case 0x7D03 -> {
                if(repeatSuppressor.check(event))
                    call = fourfoldFlame;
                else
                    return;
            }
            case 0x7D04 -> {
                if(repeatSuppressor.check(event))
                    call = twinfoldFLame;
                else
                    return;
            }
            case 0x7D02 -> {
                if(repeatSuppressor.check(event))
                    call = spikeOfFlame;
                else
                    return;
            }
            default -> {
                return;
            }
        }
        context.accept(call.getModified(event));
    }

    @HandleEvents
    public void headMarker(EventContext context, HeadMarkerEvent event) {
        int offset = getHeadmarkerOffset(event);
        final ModifiableCallout<HeadMarkerEvent> call;
        if(offset >= -263 && offset <= -256 && event.getTarget().isThePlayer()) {
            switch(offset + 264) {
                case 1 -> call = limitCut1;
                case 2 -> call = limitCut2;
                case 3 -> call = limitCut3;
                case 4 -> call = limitCut4;
                case 5 -> call = limitCut5;
                case 6 -> call = limitCut6;
                case 7 -> call = limitCut7;
                case 8 -> call = limitCut8;
                default -> {
                    return;
                }
            }
            context.accept(call.getModified(event));
        }
    }

    @HandleEvents
    public void tetherEvent(EventContext context, TetherEvent event) {
        int id = (int) event.getId();
        final ModifiableCallout<TetherEvent> call;
        if(!event.eitherTargetMatches(getState().getPlayer()))
            return;
        switch (id) {
            case 0xC0 -> call = ghastlyWind;
            case 0x54 -> call = shatteringHeatTether; //TODO: give refire and call only for tanks
            default -> {
                return;
            }
        }
        context.accept(call.getModified(event));
    }

    //Slots:
    //04 = Flamespire brand indicator
    //04 flags:
    //01000100 = cardinals safe?
    //20002 = intercards safe
    //00080004 = clear indicator
    //Buffs:
    //D9B flare
    //D9C stack
    //D9D aoe
    @AutoFeed
    public SequentialTrigger<BaseEvent> flamespireBrandSq = SqtTemplates.sq(22_000, AbilityCastStart.class,
            ace -> ace.abilityIdMatches(0x7D13),
            (e1, s) -> {
                log.info("Flamespire Brand: Start");
                List<BuffApplied> stack = s.waitEventsQuickSuccession(4, BuffApplied.class, ba -> ba.buffIdMatches(0xD9B), Duration.ofMillis(200));
                List<MapEffectEvent> me = s.waitEventsUntil(1, MapEffectEvent.class, mee -> mee.getIndex() == 4, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7D17));
                if(!me.isEmpty()) {
                    String safe;
                    log.info("Flamespire Brand: me: {}", String.format("0x%08X", me.get(0).getFlags()));
                    if(me.get(0).getFlags() != 0x20002 && me.get(0).getFlags() != 0x2000200 && me.get(0).getFlags() != 0x200020 && me.get(0).getFlags() != 0x800080) {
                        safe = "cardinals";
                    } else {
                        safe = "intercardinals";
                    }
                    log.info("Flamespire Brand: {} safe", safe);

                    if(stack.stream().map(BuffApplied::getTarget).anyMatch(XivCombatant::isThePlayer)) {
                        s.accept(flamespireOutME.getModified(Map.of("safe", safe)));
                    } else {
                        s.accept(flamespireInME.getModified(Map.of("safe", safe)));
                    }
                } else {
                    if(stack.stream().map(BuffApplied::getTarget).anyMatch(XivCombatant::isThePlayer)) {
                        s.accept(flamespireOut.getModified());
                    } else {
                        s.accept(flamespireIn.getModified());
                    }
                }


                log.info("Flamespire Brand: Waiting for stack to drop");
                s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(0xD9C)); //ID for stack debuff
                s.accept(flamespireSpread.getModified());
            });

    private static boolean ringMapEffect(MapEffectEvent mee) {
        return (mee.getIndex() == 1 || mee.getIndex() == 2 || mee.getIndex() == 3) && (mee.getFlags() == 0x20001 || mee.getFlags() == 0x200010);
    }

    private static Rotation rotationFromMapEffect(MapEffectEvent mee) {
        int flag = (int) mee.getFlags();
        return switch (flag) {
            case 0x00020001 -> Rotation.CLOCKWISE;
            case 0x00200010 -> Rotation.COUNTERCLOCKWISE;
            default -> Rotation.UNKNOWN;
        };
    }

    private static Ring ringFromCombatant(XivCombatant cbt) {
        int id = (int) cbt.getbNpcId();
        return switch (id) {
            case 15765 -> Ring.INNER;
            case 15766 -> Ring.MID;
            case 15767 -> Ring.OUTER;
            default -> Ring.UNKNOWN;
        };
    }

    private static ArenaSector translatedSector(ArenaSector midLooking, ArenaSector innerFuseLooking) {
        ArenaSector relativeToMidLooking = innerFuseLooking;

        relativeToMidLooking = switch(midLooking) { //Do nothing if already north
            case NORTHEAST -> relativeToMidLooking.plusEighths(-1);
            case EAST -> relativeToMidLooking.plusEighths(-2);
            case SOUTHEAST -> relativeToMidLooking.plusEighths(-3);
            case SOUTH -> relativeToMidLooking.plusEighths(-4);
            case SOUTHWEST -> relativeToMidLooking.plusEighths(-5);
            case WEST ->  relativeToMidLooking.plusEighths(-6);
            case NORTHWEST -> relativeToMidLooking.plusEighths(-7);
            default -> relativeToMidLooking;
        };

        return switch(relativeToMidLooking) { //if looking at the sw or se of mid, translate it
            case SOUTHEAST -> innerFuseLooking.plusEighths(-1);
            case SOUTHWEST -> innerFuseLooking.plusEighths(1);
            default -> innerFuseLooking;
        };
    }

    private enum Ring {
        INNER,
        MID,
        OUTER,
        UNKNOWN
    }

    private enum Rotation {
        CLOCKWISE,
        COUNTERCLOCKWISE,
        UNKNOWN
    }

    private enum Shape {
        SQUARE,
        TRIANGLE
    }

    List<XivCombatant> marksOfPurgatory;
    XivCombatant rubicante;
    List<XivCombatant> circlesOfHell;
    XivCombatant innerCircle;
    XivCombatant middleCircle;
    XivCombatant outerCircle;
    //Slots:
    //00 = Arena fiery or not
    //01 = Inner circle
    //02 = Middle ring
    //03 = Outer ring
    //
    //00 flags:
    //00020001 = Fiery
    //00080004 = Not fiery
    //
    //01/02/03 flags:
    //00020001 = Arrows rotating CW
    //00080004 = Clear CW arrows
    //00200010 = Arrows rotating CCW
    //00400004 = Clear CCW arrows
    //NPC IDs:
    //15759 = triangle
    //15760 = square
    //15765 = inner circle of hell
    //15766 = middle circle of hell
    //15767 = outer circle of hell
    @AutoFeed
    public SequentialTrigger<BaseEvent> hopeAbandonYeSq = SqtTemplates.multiInvocation(75_000, AbilityUsedEvent.class,
            aue -> aue.abilityIdMatches(0x7F27),
            this::hopeAbandonYe1,
            this::hopeAbandonYe2,
            this::hopeAbandonYe3,
            this::hopeAbandonYe2
            );

    private final ModifiableCallout<AbilityCastStart> hopeAbandonYe1Purgation1Triangle = new ModifiableCallout<>("HAY 1: Purgation 1 Cone", "{safe1} or {safe2}");
    private final ModifiableCallout<AbilityCastStart> hopeAbandonYe1Purgation1Square = new ModifiableCallout<>("HAY 1: Purgation 1 Square", "{safe}");
    private final ModifiableCallout<AbilityCastStart> hopeAbandonYe1Purgation2 = new ModifiableCallout<>("HAY 1: Purgation 2", "{safe}");

    public void hopeAbandonYe1(AbilityUsedEvent e1, SequentialTriggerController<BaseEvent> s) {
        log.info("Hope Abandon Ye 1: Start, purgation 1");
        //Map effects are before he starts casting
        List<MapEffectEvent> mapEffects = s.waitEvents(2, MapEffectEvent.class, EX5::ringMapEffect);
        Optional<MapEffectEvent> inner = mapEffects.stream().filter(mee -> mee.getIndex() == 01).findFirst();
        Optional<MapEffectEvent> outer = mapEffects.stream().filter(mee -> mee.getIndex() == 03).findFirst();
        if(inner.isPresent() && outer.isPresent()) {

            //Ordeal of Purgation cast start
            log.info("Hope Abandone Ye 1: Waiting for cast 1 to start");
            s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x80E9));
            refreshHopeAbandonYeActors(s);

            //First pattern is one line, in direction he is facing
            ArenaSector rubiFacing = ArenaPos.combatantFacing(rubicante);
            ArenaSector rubiFacingNew = rubiFacing.plusEighths(rotationFromMapEffect(inner.get()) == Rotation.CLOCKWISE ? 1 : -1);
            ArenaSector midLooking = ArenaPos.combatantFacing(middleCircle);
            ArenaSector flameDestinationSector = translatedSector(midLooking, rubiFacingNew);
            log.info("Flame 1 Destination: {}, rubiFacing: {}, rubiFacingNew: {}, innerRotation: {}", flameDestinationSector, rubiFacing, rubiFacingNew, rotationFromMapEffect(inner.get()));

            //STT
            //S S
            //TTS
            Rotation outerRotation = rotationFromMapEffect(outer.get());
            Map<ArenaSector, Shape> sectorToShape = Map.of(
                    ArenaSector.NORTH, Shape.TRIANGLE,
                    ArenaSector.NORTHEAST, Shape.TRIANGLE,
                    ArenaSector.EAST, Shape.SQUARE,
                    ArenaSector.SOUTHEAST, Shape.SQUARE,
                    ArenaSector.SOUTH, Shape.TRIANGLE,
                    ArenaSector.SOUTHWEST, Shape.TRIANGLE,
                    ArenaSector.WEST, Shape.SQUARE,
                    ArenaSector.NORTHWEST, Shape.SQUARE);

            //inverted rotation
            Shape destinationShape = sectorToShape.get(flameDestinationSector.plusEighths(rotationFromMapEffect(outer.get()) == Rotation.CLOCKWISE ? -1 : 1));
            if(destinationShape == Shape.TRIANGLE) {
                s.accept(hopeAbandonYe1Purgation1Triangle.getModified(Map.of("safe1", flameDestinationSector.plusEighths(-1), "safe2", flameDestinationSector.plusEighths(1))));
            } else {
                s.accept(hopeAbandonYe1Purgation1Square.getModified(Map.of("safe", flameDestinationSector.opposite())));
            }

            //Second Ordeal of Purgation
            s.waitMs(1_000);
            log.info("Hope Abandone Ye 1: Waiting for cast 2 to start");
            s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x80E9));
            refreshHopeAbandonYeActors(s);
            //This pattern is just behind the perpendicular lines, he always faces the line that is most CCW, so just get 5 rotations from his location
            ArenaSector flameSectorDestination2 = ArenaPos.combatantFacing(rubicante).plusEighths(5);
            log.info("Flame 2 Safe spot: {}, rubiFacing: {}", flameSectorDestination2, ArenaPos.combatantFacing(rubicante));
            s.accept(hopeAbandonYe1Purgation2.getModified(Map.of("safe", flameSectorDestination2)));

            //gimmeHopeAbandonYePostMechDebug(s);
        }
    }

    private final ModifiableCallout<AbilityCastStart> hopeAbandonYe2Purgation1 = new ModifiableCallout<>("HAY 2: Purgation 1", "{safe}");
    private final ModifiableCallout<AbilityCastStart> hopeAbandonYe2Purgation2 = new ModifiableCallout<>("HAY 2: Purgation 2", "Between {safe}");

    private void hopeAbandonYe2(AbilityUsedEvent e1, SequentialTriggerController<BaseEvent> s) {
        log.info("Hope Abandon Ye 2: Start, pattern 1");
        //Map effects before the cast begins
        MapEffectEvent middlemapeffect = s.waitEvent(MapEffectEvent.class, EX5::ringMapEffect);
        log.info("Hope Abandon Ye 2: Waiting for cast 1 to start");
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x80E9));
        refreshHopeAbandonYeActors(s);

        //first pattern is gonna be cw safe from the destination of the ccw most line
        ArenaSector rubiFacing = ArenaPos.combatantFacing(rubicante);
        ArenaSector midLooking = ArenaPos.combatantFacing(middleCircle);
        ArenaSector midLookingNew = midLooking.plusEighths(rotationFromMapEffect(middlemapeffect) == Rotation.CLOCKWISE ? 1 : -1);
        ArenaSector flameSectorDestination = translatedSector(midLookingNew, rubiFacing);
        ArenaSector flame1point5SectorDestination = translatedSector(midLookingNew, rubiFacing.plusEighths(2));
        log.info("Flame 1 destination: {}", flameSectorDestination);
        log.info("Flame 1.5 destination: {}", flame1point5SectorDestination);
        if(flameSectorDestination.plusEighths(1) == flame1point5SectorDestination) { //triangles right next to eachother, safe 2 cw from it
            s.accept(hopeAbandonYe2Purgation1.getModified(Map.of("safe", flameSectorDestination.plusEighths(2))));
        } else { //split, safe between them
            s.accept(hopeAbandonYe2Purgation1.getModified(Map.of("safe", flameSectorDestination.plusEighths(1))));
        }

        //Second ordeal of purgation
        s.waitMs(1_000);
        log.info("Hope Abandon Ye 2: Waiting for map effect");
        //Map effects before the cast begins
        MapEffectEvent middlemapeffect2 = s.waitEvent(MapEffectEvent.class, EX5::ringMapEffect);
        log.info("Hope Abandon Ye 2: waiting for second cast");
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x80E9));
        refreshHopeAbandonYeActors(s);

        //second pattern is double squares, leaving one eighth safe, mid rotates
        List<ArenaSector> safe = new ArrayList<>(ArenaSector.all);
        ArenaSector midLooking2 = ArenaPos.combatantFacing(middleCircle);
        ArenaSector midNewLooking = midLooking2.plusEighths(rotationFromMapEffect(middlemapeffect2) == Rotation.CLOCKWISE ? 1 : -1);
        log.info("Hope Abandon Ye 2: mid rot: {}", rotationFromMapEffect(middlemapeffect2));
        ArenaSector fuse1Looking = ArenaPos.combatantFacing(rubicante).plusEighths(-2);
        ArenaSector fuse1SectorDestination = translatedSector(midNewLooking, fuse1Looking);
        ArenaSector fuse2Looking = ArenaPos.combatantFacing(rubicante).plusEighths(2);
        ArenaSector fuse2SectorDestination = translatedSector(midNewLooking, fuse2Looking);
        log.info("Hope Abandon Ye 2 DEBUG: midLooking2: {}, midNewLooking: {}, fuse1Looking: {}, fuse2Looking: {}", midLooking2, midNewLooking, fuse1Looking, fuse2Looking);
        log.info("Hope Abandon Ye 2: squares are at {} and {}", fuse1SectorDestination, fuse2SectorDestination);

        safe.remove(fuse1SectorDestination);
        safe.remove(fuse1SectorDestination.plusEighths(1));
        safe.remove(fuse1SectorDestination.plusEighths(-1));
        safe.remove(fuse2SectorDestination);
        safe.remove(fuse2SectorDestination.plusEighths(1));
        safe.remove(fuse2SectorDestination.plusEighths(-1));

        s.accept(hopeAbandonYe2Purgation2.getModified(Map.of("safe", safe)));
    }

    private final ModifiableCallout<AbilityCastStart> hopeAbandonYe3Purgation1Triangle = new ModifiableCallout<>("HAY 3: Purgation 1 Triangle", "{safe1} or {safe2}");
    private final ModifiableCallout<AbilityCastStart> hopeAbandonYe3Purgation1Square = new ModifiableCallout<>("HAY 3: Purgation 1 Square", "{safe}");
    private final ModifiableCallout<AbilityCastStart> hopeAbandonYe3Purgation2minus1 = new ModifiableCallout<>("HAY 3: Purgation 2 CCW safe", "Counterclockwise from {safe}");
    private final ModifiableCallout<AbilityCastStart> hopeAbandonYe3Purgation2plus3 = new ModifiableCallout<>("Hay 3: Purgation 2 CW safe", "Clockwise from {safe}");

    public void hopeAbandonYe3(AbilityUsedEvent e1, SequentialTriggerController<BaseEvent> s) {
        log.info("Hope Abandon Ye 3: Start, purgation 1");
        List<MapEffectEvent> mapEffects = s.waitEvents(2, MapEffectEvent.class, EX5::ringMapEffect);
        Optional<MapEffectEvent> inner = mapEffects.stream().filter(mee -> mee.getIndex() == 01).findFirst();
        Optional<MapEffectEvent> middle = mapEffects.stream().filter(mee -> mee.getIndex() == 02).findFirst();
        if(inner.isPresent() && middle.isPresent()) {
            //Ordeal of Purgation cast start
            log.info("Hope Abandone Ye 3: Waiting for cast 1 to start");
            s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x80E9));
            refreshHopeAbandonYeActors(s);

            ArenaSector rubiLooking = ArenaPos.combatantFacing(rubicante);
            ArenaSector midLooking = ArenaPos.combatantFacing(middleCircle);
            ArenaSector innerNew = rubiLooking.plusEighths(rotationFromMapEffect(inner.get()) == Rotation.CLOCKWISE ? 1 : -1);
            ArenaSector middleNew = midLooking.plusEighths(rotationFromMapEffect(middle.get()) == Rotation.CLOCKWISE ? 1 : -1);
            ArenaSector flameSectorDestination = translatedSector(middleNew, innerNew);
            if(flameSectorDestination.isCardinal()) { //triangles on cardinals
                s.accept(hopeAbandonYe3Purgation1Triangle.getModified(Map.of("safe1", flameSectorDestination.plusEighths(-1), "safe2", flameSectorDestination.plusEighths(1))));
            } else { //squares on intercards
                s.accept(hopeAbandonYe3Purgation1Square.getModified(Map.of("safe", flameSectorDestination.opposite())));
            }

            log.info("Hope Abandon Ye 3: Waiting for map effects");
            MapEffectEvent mapEffect = s.waitEvent(MapEffectEvent.class, EX5::ringMapEffect);

            log.info("Hope Abandone Ye 3: Waiting for cast 2 to start");
            s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x80E9));
            refreshHopeAbandonYeActors(s);

            //solution by:
            //https://www.youtube.com/watch?v=TNzz215p_N4&ab_channel=Kaouri
            //find middle translation to the left or right of the perpendicular lines
            // (rubifacing + 1), then check -2 and +2 from here on midRingTranslation (rubifacing then -1 or +3)
            // if its -2, call "ccw of [-2]"
            // if its +2, call "cw of [2]"

            ArenaSector rubiLooking2 = ArenaPos.combatantFacing(rubicante);
            ArenaSector midLooking2 = ArenaPos.combatantFacing(middleCircle);
            ArenaSector minus1Sector = rubiLooking2.plusEighths(-1);
            ArenaSector plus3Sector = rubiLooking2.plusEighths(3);
            if(translatedSector(midLooking2, minus1Sector) == minus1Sector) { //if (insert into function) and its unchanged
                s.accept(hopeAbandonYe3Purgation2minus1.getModified(Map.of("safe", rubiLooking2.plusEighths(-1))));
            } else if(translatedSector(midLooking2, plus3Sector) == plus3Sector) {
                s.accept(hopeAbandonYe3Purgation2plus3.getModified(Map.of("safe", rubiLooking2.plusEighths(3))));
            }
        }
    }

    public void refreshHopeAbandonYeActors(SequentialTriggerController<BaseEvent> s) {
        s.waitMs(500); //he could still be turning
        s.refreshCombatants(100);
        log.info("refreshHopeAbandonYeActors: Starting, finding boss");
        Optional<XivCombatant> maybeRubicante;
        do {
            maybeRubicante = getState().getCombatants().values().stream().filter(cbt -> {
                long id = cbt.getbNpcId();
                return id == 15756;
            }).findFirst();
            if(maybeRubicante.isPresent()) {
                rubicante = maybeRubicante.get();
                break;
            } else {
                s.refreshCombatants(200);
            }
        } while (true);

        log.info("refreshHopeAbandonYeActors: Finding Circles of Purgatory (outside)");
        do {
            marksOfPurgatory = getState().getCombatants().values().stream().filter(cbt -> {
                long id = cbt.getbNpcId();
                return id == 15759 || id == 15760;
            }).toList();
            if(marksOfPurgatory.size() < 16) {
                s.refreshCombatants(200);
            } else {
                break;
            }
        } while (true);

        log.info("refreshHopeAbandonYeActors: Finding floor circles");
        do {
            circlesOfHell = getState().getCombatants().values().stream().filter( cbt -> {
                long id = cbt.getbNpcId();
                return id == 15765 || id == 15766 || id == 15767;
            }).toList();
            if(circlesOfHell.size() < 3) {
                s.refreshCombatants(200);
            } else {
                break;
            }
        } while (true);

        Optional<XivCombatant> maybeInner = circlesOfHell.stream().filter(cbt -> cbt.getbNpcId() == 15765).findFirst();
        maybeInner.ifPresent(cbt -> innerCircle = cbt);

        Optional<XivCombatant> maybeMiddle = circlesOfHell.stream().filter(cbt -> cbt.getbNpcId() == 15766).findFirst();
        maybeMiddle.ifPresent(cbt -> middleCircle = cbt);

        Optional<XivCombatant> maybeOuter = circlesOfHell.stream().filter(cbt -> cbt.getbNpcId() == 15767).findFirst();
        maybeOuter.ifPresent(cbt -> outerCircle = cbt);

        log.info("refreshHopeAbandonYeActors: Done.");
    }

    public void gimmeHopeAbandonYePostMechDebug(SequentialTriggerController<BaseEvent> s) {
        //POST-MECH DEBUG
        s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x80E9));
        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7CEF, 0x7CF0));
        List<XivCombatant> circlesOfHellNEW;
        do {
            circlesOfHellNEW = getState().getCombatants().values().stream().filter( cbt -> {
                long id = cbt.getbNpcId();
                return id == 15765 || id == 15766 || id == 15767;
            }).toList();
            if(circlesOfHellNEW.size() < 3) {
                s.refreshCombatants(200);
            } else {
                break;
            }
        } while (true);
        for (XivCombatant cbt : circlesOfHellNEW) {
            Ring ring = ringFromCombatant(cbt);
            ArenaSector facing = ArenaPos.combatantFacing(cbt);
            log.info("AND NOW {} is LOOKING: {}", ring, facing);
        }
    }
}
