package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CalloutRepo(name = "M8S", duty = KnownDuty.M8S)
public class M8S extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(M8S.class);
	private XivState state;
	private ActiveCastRepository casts;
	private StatusEffectRepository buffs;

	public M8S(XivState state, ActiveCastRepository casts, StatusEffectRepository buffs) {
		this.state = state;
		this.casts = casts;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M8S);
	}

//	private final ModifiableCallout<AbilityCastStart> extraplanarPursuit = ModifiableCallout.durationBasedCall("Extraplanar Pursuit", "Raidwide");
//	private final ModifiableCallout<AbilityCastStart> millennialDecay = ModifiableCallout.durationBasedCall("Millennial Decay", "Raidwide");
//
//	// TODO: you have to look at his nouliths to determine whether it's cardinal or intercardinal dodge
//	private final ModifiableCallout<AbilityCastStart> windfang = ModifiableCallout.durationBasedCall("Windfang", "Conal Pair Stack, In");
//	private final ModifiableCallout<AbilityCastStart> stonefang = ModifiableCallout.durationBasedCall("Stonefang", "Conal Spread, Out");

	/*
	P1

		Fangs
		Windfang = Conal pair stack, in
		Stonefang = Conal spread, out
		His floating nouliths thingos determine whether it's a cardinal dodge or intercardinal dodge

		Reigns
		Revolutionary Reign = Dodge initial clones, dodge line dash, then AWAY from boss (large chariot) into T/HDD/HDD/T conal stacks
		Eminent Reign = Dodge initial clones, dodge line dash, then CLOSE to boss (frontal 150deg cone) into T/HDD/HDD/T conal stacks

		"Pantokrator" Part 1
		First dragon head spawns either N or S and subsequent heads spawn in either a CW or CCW direction, boss casts a knockback.
		For my group we always orient to the first head and make that our relative north, split light parties left/right of the head.
		Either all supports or all DPSes get marked with first puddle drops.
		These puddle drops are always placed in intercardinals.
		Those not marked for first puddle drops get knocked to the safe cardinals.
		Once first puddles are dropped, the other roles get marked for puddles, while heads are going off in a CW/CCW direction.
		Depending on orientation, the next puddles are placed in the adjacent intercardinals in the direction of travel
			(e.g. if first puddles are placed NW and SW, and the heads go in a CCW direction, second puddles are placed SW and SE).

		"Pantokrator" Part 2
		Boss summons 4 dragon heads and casts another knockback.
		Either all DPSes or all supports get tethered to a head and need to stretch the tether directly opposite,
			but also stand behind the head they are stretching the tether to, to avoid the conal cleave going out directly opposite.
			The other role soaks a tower in the free cardinals/intercardinals. We colour code the tower soaks.

		Tracking Tremors
		8x heavy hitting party stack.

		Great Divide
		Tank-stack line cleave.

		Terrestrial Titans
		Slice Is Right mechanic.
		However this time there are only two permutations - from the pillars alone, pairs of directly opposite intercardinals are safe.
		These pairs are determined by where the slice on the pillar points to.
		If, at an intercardinal, you are able to see the bright blue lines bisecting the two pillars, then you are in a potential safe spot.
		If you are unable to see the bright blue lines, then the other pair is safe.
		The safe pair of intercardinals is then narrowed down even further to only one safe spot based on the floating noulith thingos.
		If the beams point to the intercardinals near you, then the opposite side is safe.
	 */

	/*
	Adds
		Everyone gets tethered to either a green head or a yellow head.
		Stretch tethers over to the other head, i.e. if you get tethered to yellow head, stretch and you'll be hitting green head and vice versa.
		Two coloured shapes will spawn - a green orb or a yellow cube.
		Non-tanks will get a coloured, timed debuff that is cleansed by walking through the shape of the same colour.
		This also changes the 'polarity' of whoever just walked through the shape, so after walking through the shape
			they now switch target over to the head they weren't hitting earlier.
		Walking through a shape gives everyone a vuln up, so shape pops have to be staggered.

	Tanks will always want to drag their head in the direction of the differently-coloured shape - the tank pulling
		the yellow head will chase the green orb, vice versa. This can be simplified into a CW/CCW callout.
	When head is getting pulled, prey marker goes on a non-tank on each side and goes off at the same time as a line tankbuster cleave goes on the tank. Technically the prey marker is a single-targeted line cleave, but the rest of the non-tanks on each side can soak it together as damage is non-lethal.

At the end of all 3 sets of shape pops, only the tanks remain on the same head from the original group. By this point, either or both heads are close to dying, so keep DPS up until they're dead. Tanks can also bring them towards each other for easier target switching once either dies.
	 */
}
