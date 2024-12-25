# About Cast Locations

Cast locations are not particularly straightforward.

## Instant Casts

Let's take an instant cast skill. We get a 21/22-line, and a 264-line.

If the skill is ground-targeted, we will get a 264-line with a specific cast location and rotation.
This is the best possible data and is the best case scenario. In this case, we need no other information
from the log lines.

However, most skills will not have this. We use two other pieces of data:
- The cast angle
- The animation target

The animation target is useful for determining, for example, whether an instant cast AoE is centered on
the caster, or the target (if one exists). For example, Sage's Phlegma is centered on the target, but
Dyskrasia is centered around the caster.

If we do not have those pieces of information, we try to guess based on the CastType.

## Non-Instant Casts

We also want to display an omen on skills that are still casting.

Like the instant cast skills, if we have a location and rotation,
that is the best case scenario. We need no other data.

If the skill is not instant, we use the target of the cast and rotation
to try to determine it. We assume that if the skill is AoE, and has a target,
that it will use that target as the basis for the aoe.

If the skill is not instant, and we can associate it back to a cast (20-line),
and the cast event has a target other than environment or the caster,
then we can assume that the targeted entity is the true aoe target.

# Cast AoEs

The actual cast shape and size is determined mainly by three properties of the action:
- CastType: determines shape of cast (see `OmenType.fromCastType(int)`)
- EffectRange: determines the primary size (e.g. circle radius or rectangle length)
- XAxisModifier: determines the secondary size (rectangle width, unused for circles and cones)

Unfortunately, there are two data points that cannot be derived from this data alone:
- Inner radius of donuts
- Angle of cones

For the inner donut radius, often times it is simply the caster's hitbox, but not always.
For the cone angle, if the skill has an omen (technical term for skill telegraph), it can 
sometimes be derived from the name of the "omen" . For example, Action 21471 (Wingbeat) 
has an omen named "er_gl_fan060_1bf". From the "060" part, we can assume that the angle is
60 degrees.