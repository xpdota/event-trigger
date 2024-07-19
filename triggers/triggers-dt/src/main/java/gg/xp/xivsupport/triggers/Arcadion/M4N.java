package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.vfx.StatusLoopVfxApplied;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcAbilityUsedCallout;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@CalloutRepo(name = "M4N", duty = KnownDuty.M4N)
public class M4N extends AutoChildEventHandler implements FilteredEventHandler {
	public static final Logger log = LoggerFactory.getLogger(M4N.class);

	@NpcCastCallout({0x92BD, 0x92BF}) //second ID is while wings are active
	private final ModifiableCallout<AbilityCastStart> sidewiseSparkWest = ModifiableCallout.durationBasedCall("Sidewise Spark: East safe", "East safe");
	@NpcCastCallout({0x92BC, 0x92BE})
	private final ModifiableCallout<AbilityCastStart> sidewiseSparkEast = ModifiableCallout.durationBasedCall("Sidewise Spark: West safe", "West safe");
	@NpcAbilityUsedCallout(0x92AB)
	private final ModifiableCallout<AbilityUsedEvent> stampedingThunderWest = new ModifiableCallout<>("Stampeding Thunder: Go East", "East safe");
	@NpcAbilityUsedCallout(0x92AC)
	private final ModifiableCallout<AbilityUsedEvent> stampedingThunderEast = new ModifiableCallout<>("Stampeding Thunder: Go West", "West safe");

	public M4N(XivState state) {
		this.state = state;
	}
	private final XivState state;

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M4N);
	}

	public static final List<Long> validStacks = List.of(723L,724L);

	public ArenaSector getSafeDirectionForBA(BuffApplied ba) {
		//723 hits north, go south
		//724 hits south, go north
		long stacks = ba.getRawStacks();
		return stacks == 723 ? ArenaSector.SOUTH : ArenaSector.NORTH;
	}

	private final ModifiableCallout<?> gunStart = new ModifiableCallout<>("Gun Blasts: Start", "Start {safe}");
	private final ModifiableCallout<?> gunSafe = new ModifiableCallout<>("Gun Blasts: Next Safe", "{safe}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> gunBlast = SqtTemplates.sq(30_000, AbilityCastStart.class,
			acs -> acs.abilityIdMatches(0x92B0, 0x9B4F, 0x9B56, 0x92AD, 0x9B55, 0x9B57),
			(e1, s) -> {
				log.info("Gun Blasts: Start");
				int iterations = switch((int)e1.getAbility().getId()) {
					case 0x92B0, 0x92AD -> 2;
					case 0x9B4F, 0x9B55 -> 3;
					case 0x9B56, 0x9B57 -> 4;
					default -> 0;
				};

				//Buff 0xB9A
				ArenaSector buff1 = getSafeDirectionForBA(s.waitEvent(BuffApplied.class, ba -> validStacks.contains(ba.getRawStacks()) && ba.buffIdMatches(0xB9A)));
				log.info("Gun Blasts Start: {}", buff1);
				s.setParam("safe", buff1);
				s.updateCall(gunStart);

				List<BuffApplied> furtherBuffs = s.waitEvents(iterations, BuffApplied.class, ba -> validStacks.contains(ba.getRawStacks()) && ba.buffIdMatches(0xB9A));

				boolean wasNorth = buff1 != ArenaSector.SOUTH;
				//repeats up to 4 times
				for(int i = 0; i < iterations; i++) {
					//many IDs. Some may be missing. Ability name "Wicked Cannon"
					//0x4E40, 0x9BAC, 0x92AE, 0x9BBE, 0x92AF
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x4E40, 0x9BAC, 0x92AE, 0x9BBE, 0x92AF) && aue.isFirstTarget());
					ArenaSector safeSide = getSafeDirectionForBA(furtherBuffs.get(i));
					log.info("Gun Blasts {}: {}", i, safeSide);
					if(wasNorth && safeSide == ArenaSector.SOUTH) {
						s.setParam("safe", ArenaSector.SOUTH);
						s.updateCall(gunSafe);
						wasNorth = false;
					}
					else if(!wasNorth && safeSide == ArenaSector.NORTH) {
						s.setParam("safe", ArenaSector.NORTH);
						s.updateCall(gunSafe);
						wasNorth = true;
					}
				}
			});

	public static final List<Long> validIDs = List.of(793L, 794L);

	private final ArenaPos arenaPos = new ArenaPos(100, 100, 5, 5);

	private final ModifiableCallout<?> shadowSafe = new ModifiableCallout<>("Shadow's Sabbath", "{safe} safe");

	@AutoFeed
	//TODO:these calls overlap slightly when it does four in a row, especially when she also does a cleave
	private final SequentialTrigger<BaseEvent> shadowsSabbeth = SqtTemplates.sq(20_000, StatusLoopVfxApplied.class,
			slva -> validIDs.contains(slva.getStatusLoopVfx().getId()),
			(e1, s) -> {
				ArenaSector addDir = arenaPos.forCombatant(e1.getTarget());
				boolean hittingLeft = e1.getStatusLoopVfx().getId() == 794;
				//794 left
				//793 right
				ArenaSector safeDir = switch(addDir) {
					case SOUTH -> hittingLeft ? ArenaSector.EAST : ArenaSector.WEST;
					case NORTH -> hittingLeft ? ArenaSector.WEST : ArenaSector.EAST;
					case EAST -> hittingLeft ? ArenaSector.NORTH : ArenaSector.SOUTH;
					case WEST -> hittingLeft ? ArenaSector.SOUTH : ArenaSector.NORTH;
					default -> ArenaSector.UNKNOWN;
				};

				s.setParam("safe", safeDir);
				s.updateCall(shadowSafe);
			});
}
