package gg.xp.xivsupport.events.triggers.easytriggers;

public class DeserializationSecurityTest {
	private static final String triggerDataNew = """
			[
			  {
			    "enabled": true,
			    "eventType": "gg.xp.xivsupport.events.actlines.events.AbilityCastStart",
			    "conditions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter",
			        "operator": "EQ",
			        "expected": 173
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourcePartyMemberFilter",
			        "invert": false
			      }
			    ],
			    "actions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.actions.DurationBasedCalloutAction",
			        "tts": "{event.source} is raising {event.target}",
			        "text": "{event.source} is raising {event.target} ({event.estimatedRemainingDuration})",
			        "colorRaw": -993263514,
			        "plusDuration": true,
			        "hangTime": 1234,
			        "useIcon": true,
			        "uuid": "883ecb35-8324-411b-9d0f-cd131de42a57"
			      }
			    ],
			    "name": "Rez Start"
			  },
			  {
			    "enabled": true,
			    "eventType": "gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent",
			    "conditions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter",
			        "operator": "EQ",
			        "expected": 173
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourcePartyMemberFilter",
			        "invert": false
			      }
			    ],
			    "actions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.actions.CalloutAction",
			        "tts": "{event.source} just raised {event.target}",
			        "text": "{event.source} just raised {event.target}",
			        "colorRaw": -6684928,
			        "hangTime": 3333,
			        "useIcon": true,
			        "uuid": "4fcdcfb5-4f74-4a49-b425-2faf7460caae"
			      }
			    ],
			    "name": "Give me a name"
			  }
						]
						""";
}
