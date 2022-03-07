package gg.xp.xivsupport.gui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.xivsupport.eventstorage.EventReader;
import org.intellij.lang.annotations.Language;

public class GuiWithFflogsData {

	public static void main(String[] args) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(theString);
		LaunchImportedFflogs.fromEvents(EventReader.readFflogsJson(root));
	}

	@Language("JSON")
	private static final String theString = """
			{
			"reportData": {
			  "report": {
				"code": "vNGnMwbRDakhQPLm",
				"startTime": 1639362644801,
				"endTime": 1639365786158,
				"masterData": {
				  "actors": [
					{
					  "gameID": 0,
					  "id": -1,
					  "name": "Environment",
					  "petOwner": null,
					  "type": "NPC",
					  "subType": "NPC"
					},
					{
					  "gameID": 1000001,
					  "id": 1,
					  "name": "",
					  "petOwner": null,
					  "type": "NPC",
					  "subType": "Unknown"
					},
					{
					  "gameID": 1000002,
					  "id": 2,
					  "name": "Player One",
					  "petOwner": null,
					  "type": "Player",
					  "subType": "Summoner"
					},
					{
					  "gameID": 1000003,
					  "id": 3,
					  "name": "Player Two",
					  "petOwner": null,
					  "type": "Player",
					  "subType": "Paladin"
					},
					{
					  "gameID": 1000004,
					  "id": 4,
					  "name": "Player Three",
					  "petOwner": null,
					  "type": "Player",
					  "subType": "Monk"
					},
					{
					  "gameID": 1000005,
					  "id": 5,
					  "name": "Player Four",
					  "petOwner": null,
					  "type": "Player",
					  "subType": "Ninja"
					},
					{
					  "gameID": 1000006,
					  "id": 6,
					  "name": "Player Five",
					  "petOwner": null,
					  "type": "Player",
					  "subType": "Warrior"
					},
					{
					  "gameID": 1000007,
					  "id": 7,
					  "name": "Player Six",
					  "petOwner": null,
					  "type": "Player",
					  "subType": "Dancer"
					},
					{
					  "gameID": 1000008,
					  "id": 8,
					  "name": "Player Seven",
					  "petOwner": null,
					  "type": "Player",
					  "subType": "Scholar"
					},
					{
					  "gameID": 1000009,
					  "id": 9,
					  "name": "Player Eight",
					  "petOwner": null,
					  "type": "Player",
					  "subType": "Sage"
					},
					{
					  "gameID": 13498,
					  "id": 10,
					  "name": "Carbuncle",
					  "petOwner": 2,
					  "type": "Pet",
					  "subType": "Pet"
					},
					{
					  "gameID": 1009,
					  "id": 11,
					  "name": "Selene",
					  "petOwner": 8,
					  "type": "Pet",
					  "subType": "Pet"
					},
					{
					  "gameID": 12877,
					  "id": 12,
					  "name": "Zodiark",
					  "petOwner": null,
					  "type": "NPC",
					  "subType": "Boss"
					},
					{
					  "gameID": 1000013,
					  "id": 13,
					  "name": "Multiple Players",
					  "petOwner": null,
					  "type": "Player",
					  "subType": "LimitBreak"
					},
					{
					  "gameID": 10897,
					  "id": 14,
					  "name": "bunshin",
					  "petOwner": 5,
					  "type": "Pet",
					  "subType": "Pet"
					},
					{
					  "gameID": 6982,
					  "id": 15,
					  "name": "Demi-Bahamut",
					  "petOwner": 2,
					  "type": "Pet",
					  "subType": "Pet"
					},
					{
					  "gameID": 13506,
					  "id": 16,
					  "name": "Emerald Garuda",
					  "petOwner": 2,
					  "type": "Pet",
					  "subType": "Pet"
					},
					{
					  "gameID": 14388,
					  "id": 17,
					  "name": "Quetzalcoatl",
					  "petOwner": null,
					  "type": "NPC",
					  "subType": "NPC"
					},
					{
					  "gameID": 2000018,
					  "id": 18,
					  "name": "Quetzalcoatl",
					  "petOwner": null,
					  "type": "NPC",
					  "subType": "NPC"
					},
					{
					  "gameID": 13505,
					  "id": 19,
					  "name": "Ruby Ifrit",
					  "petOwner": 2,
					  "type": "Pet",
					  "subType": "Pet"
					},
					{
					  "gameID": 10487,
					  "id": 20,
					  "name": "Seraph",
					  "petOwner": 8,
					  "type": "Pet",
					  "subType": "Pet"
					},
					{
					  "gameID": 2000021,
					  "id": 21,
					  "name": "Zodiark",
					  "petOwner": null,
					  "type": "NPC",
					  "subType": "NPC"
					},
					{
					  "gameID": 13507,
					  "id": 22,
					  "name": "Topaz Titan",
					  "petOwner": 2,
					  "type": "Pet",
					  "subType": "Pet"
					},
					{
					  "gameID": 13712,
					  "id": 23,
					  "name": "arcane sigil",
					  "petOwner": null,
					  "type": "NPC",
					  "subType": "NPC"
					},
					{
					  "gameID": 13713,
					  "id": 24,
					  "name": "arcane sigil",
					  "petOwner": null,
					  "type": "NPC",
					  "subType": "NPC"
					},
					{
					  "gameID": 10488,
					  "id": 25,
					  "name": "Demi-Phoenix",
					  "petOwner": 2,
					  "type": "Pet",
					  "subType": "Pet"
					},
					{
					  "gameID": 14386,
					  "id": 26,
					  "name": "Behemoth",
					  "petOwner": null,
					  "type": "NPC",
					  "subType": "NPC"
					},
					{
					  "gameID": 2000027,
					  "id": 27,
					  "name": "Behemoth",
					  "petOwner": null,
					  "type": "NPC",
					  "subType": "NPC"
					},
					{
					  "gameID": 1000028,
					  "id": 28,
					  "name": "Multiple Enemies",
					  "petOwner": null,
					  "type": "NPC",
					  "subType": "NPC"
					},
					{
					  "gameID": 14387,
					  "id": 29,
					  "name": "python",
					  "petOwner": null,
					  "type": "NPC",
					  "subType": "NPC"
					},
					{
					  "gameID": 2000030,
					  "id": 30,
					  "name": "python",
					  "petOwner": null,
					  "type": "NPC",
					  "subType": "NPC"
					},
					{
					  "gameID": 13717,
					  "id": 31,
					  "name": "roiling Darkness",
					  "petOwner": null,
					  "type": "NPC",
					  "subType": "NPC"
					},
					{
					  "gameID": 13711,
					  "id": 32,
					  "name": "arcane sigil",
					  "petOwner": null,
					  "type": "NPC",
					  "subType": "NPC"
					},
					{
					  "gameID": 1000033,
					  "id": 33,
					  "name": "Limit Break",
					  "petOwner": null,
					  "type": "Player",
					  "subType": "LimitBreak"
					},
					{
					  "gameID": 1000034,
					  "id": 34,
					  "name": "Tick Effect",
					  "petOwner": null,
					  "type": "Player",
					  "subType": "LimitBreak"
					}
				  ]
				},
				"events": {
				  "data": [
					{
					  "timestamp": 2462088,
					  "type": "combatantinfo",
					  "fight": 6,
					  "sourceID": 8,
					  "gear": [],
					  "auras": [
						{
						  "source": 8,
						  "ability": 1000048,
						  "stacks": 1,
						  "icon": "016000-016202.png",
						  "name": "Well Fed"
						},
						{
						  "source": 8,
						  "ability": 1000297,
						  "stacks": 1,
						  "icon": "012000-012801.png",
						  "name": "Galvanize"
						}
					  ],
					  "level": 90,
					  "strength": 368,
					  "dexterity": 412,
					  "vitality": 2165,
					  "intelligence": 431,
					  "mind": 2323,
					  "piety": 1189,
					  "attack": 368,
					  "directHit": 400,
					  "criticalHit": 858,
					  "attackMagicPotency": 2323,
					  "healMagicPotency": 2323,
					  "determination": 1768,
					  "skillSpeed": 400,
					  "spellSpeed": 803,
					  "tenacity": 400
					},
					{
					  "timestamp": 2462088,
					  "type": "combatantinfo",
					  "fight": 6,
					  "sourceID": 5,
					  "gear": [],
					  "auras": [
						{
						  "source": 7,
						  "ability": 1001824,
						  "stacks": 1,
						  "icon": "013000-013713.png",
						  "name": "Dance Partner"
						},
						{
						  "source": 8,
						  "ability": 1000297,
						  "stacks": 1,
						  "icon": "012000-012801.png",
						  "name": "Galvanize"
						}
					  ],
					  "level": 90
					},
					{
					  "timestamp": 2462088,
					  "type": "combatantinfo",
					  "fight": 6,
					  "sourceID": 2,
					  "gear": [],
					  "auras": [
						{
						  "source": 2,
						  "ability": 1001082,
						  "stacks": 1,
						  "icon": "016000-016516.png",
						  "name": "Squadron Engineering Manual"
						},
						{
						  "source": 2,
						  "ability": 1000048,
						  "stacks": 1,
						  "icon": "016000-016202.png",
						  "name": "Well Fed"
						},
						{
						  "source": 2,
						  "ability": 1001084,
						  "stacks": 1,
						  "icon": "016000-016508.png",
						  "name": "Squadron Rationing Manual"
						},
						{
						  "source": 8,
						  "ability": 1000297,
						  "stacks": 1,
						  "icon": "012000-012801.png",
						  "name": "Galvanize"
						}
					  ],
					  "level": 90
					},
					{
					  "timestamp": 2462088,
					  "type": "combatantinfo",
					  "fight": 6,
					  "sourceID": 6,
					  "gear": [],
					  "auras": [
						{
						  "source": 6,
						  "ability": 1000048,
						  "stacks": 1,
						  "icon": "016000-016202.png",
						  "name": "Well Fed"
						},
						{
						  "source": 8,
						  "ability": 1000297,
						  "stacks": 1,
						  "icon": "012000-012801.png",
						  "name": "Galvanize"
						}
					  ],
					  "level": 90
					},
					{
					  "timestamp": 2462088,
					  "type": "combatantinfo",
					  "fight": 6,
					  "sourceID": 7,
					  "gear": [],
					  "auras": [
						{
						  "source": 7,
						  "ability": 1001823,
						  "stacks": 1,
						  "icon": "013000-013712.png",
						  "name": "Closed Position"
						},
						{
						  "source": 7,
						  "ability": 1000048,
						  "stacks": 1,
						  "icon": "016000-016202.png",
						  "name": "Well Fed"
						},
						{
						  "source": 7,
						  "ability": 1001818,
						  "stacks": 1,
						  "icon": "013000-013705.png",
						  "name": "Standard Step"
						},
						{
						  "source": 8,
						  "ability": 1000297,
						  "stacks": 1,
						  "icon": "012000-012801.png",
						  "name": "Galvanize"
						}
					  ],
					  "level": 90
					},
					{
					  "timestamp": 2462088,
					  "type": "combatantinfo",
					  "fight": 6,
					  "sourceID": 3,
					  "gear": [],
					  "auras": [
						{
						  "source": 3,
						  "ability": 1000079,
						  "stacks": 1,
						  "icon": "012000-012506.png",
						  "name": "Iron Will"
						},
						{
						  "source": 9,
						  "ability": 1002605,
						  "stacks": 1,
						  "icon": "012000-012952.png",
						  "name": "Kardion"
						},
						{
						  "source": 9,
						  "ability": 1002607,
						  "stacks": 1,
						  "icon": "012000-012954.png",
						  "name": "Eukrasian Diagnosis"
						},
						{
						  "source": 3,
						  "ability": 1000050,
						  "stacks": 1,
						  "icon": "010000-010101.png",
						  "name": "Sprint"
						}
					  ],
					  "level": 90
					},
					{
					  "timestamp": 2462088,
					  "type": "combatantinfo",
					  "fight": 6,
					  "sourceID": 9,
					  "gear": [],
					  "auras": [
						{
						  "source": 9,
						  "ability": 1002604,
						  "stacks": 1,
						  "icon": "012000-012951.png",
						  "name": "Kardia"
						},
						{
						  "source": 9,
						  "ability": 1000048,
						  "stacks": 1,
						  "icon": "016000-016202.png",
						  "name": "Well Fed"
						},
						{
						  "source": 8,
						  "ability": 1000297,
						  "stacks": 1,
						  "icon": "012000-012801.png",
						  "name": "Galvanize"
						},
						{
						  "source": 9,
						  "ability": 1002606,
						  "stacks": 1,
						  "icon": "012000-012953.png",
						  "name": "Eukrasia"
						}
					  ],
					  "level": 90
					},
					{
					  "timestamp": 2462088,
					  "type": "combatantinfo",
					  "fight": 6,
					  "sourceID": 4,
					  "gear": [],
					  "auras": [
						{
						  "source": 4,
						  "ability": 1000046,
						  "stacks": 1,
						  "icon": "016000-016006.png",
						  "name": "Gatherer's Grace"
						},
						{
						  "source": 4,
						  "ability": 1000048,
						  "stacks": 1,
						  "icon": "016000-016202.png",
						  "name": "Well Fed"
						},
						{
						  "source": 4,
						  "ability": 1002513,
						  "stacks": 1,
						  "icon": "012000-012535.png",
						  "name": "Formless Fist"
						},
						{
						  "source": 8,
						  "ability": 1000297,
						  "stacks": 1,
						  "icon": "012000-012801.png",
						  "name": "Galvanize"
						}
					  ],
					  "level": 90
					},
					{
					  "timestamp": 2462088,
					  "type": "calculateddamage",
					  "sourceID": 2,
					  "targetID": 12,
					  "abilityGameID": 3579,
					  "fight": 6,
					  "hitType": 2,
					  "amount": 14410,
					  "unmitigatedAmount": 14410,
					  "directHit": true,
					  "multiplier": 1,
					  "packetID": 52903
					},
					{
					  "timestamp": 2462133,
					  "type": "cast",
					  "sourceID": 3,
					  "targetID": 12,
					  "abilityGameID": 7384,
					  "fight": 6
					},
					{
					  "timestamp": 2462133,
					  "type": "calculateddamage",
					  "sourceID": 3,
					  "targetID": 12,
					  "abilityGameID": 7384,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 3598,
					  "unmitigatedAmount": 3598,
					  "multiplier": 1,
					  "packetID": 52904
					},
					{
					  "timestamp": 2462133,
					  "type": "calculatedheal",
					  "sourceID": 3,
					  "targetID": 3,
					  "abilityGameID": 7384,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 4999,
					  "multiplier": 1,
					  "packetID": 52904,
					  "unpaired": true
					},
					{
					  "timestamp": 2462489,
					  "type": "cast",
					  "sourceID": 5,
					  "targetID": 5,
					  "abilityGameID": 2261,
					  "fight": 6
					},
					{
					  "timestamp": 2462489,
					  "type": "applybuff",
					  "sourceID": 5,
					  "targetID": 5,
					  "abilityGameID": 1000496,
					  "fight": 6,
					  "extraAbilityGameID": 2261
					},
					{
					  "timestamp": 2462489,
					  "type": "applybuffstack",
					  "sourceID": 5,
					  "targetID": 5,
					  "abilityGameID": 1000496,
					  "fight": 6,
					  "stack": 2
					},
					{
					  "timestamp": 2462578,
					  "type": "begincast",
					  "sourceID": 9,
					  "targetID": 12,
					  "abilityGameID": 24318,
					  "fight": 6,
					  "duration": 1460
					},
					{
					  "timestamp": 2462756,
					  "type": "applybuff",
					  "sourceID": 5,
					  "targetID": 5,
					  "abilityGameID": 1000614,
					  "fight": 6,
					  "extraAbilityGameID": 2245,
					  "extraInfo": 50
					},
					{
					  "timestamp": 2462756,
					  "type": "applybuff",
					  "sourceID": 7,
					  "targetID": 5,
					  "abilityGameID": 1002105,
					  "fight": 6
					},
					{
					  "timestamp": 2462756,
					  "type": "applybuff",
					  "sourceID": 7,
					  "targetID": 5,
					  "abilityGameID": 1001847,
					  "fight": 6
					},
					{
					  "timestamp": 2462756,
					  "type": "cast",
					  "sourceID": 7,
					  "targetID": -1,
					  "abilityGameID": 16192,
					  "fight": 6
					},
					{
					  "timestamp": 2462756,
					  "type": "calculateddamage",
					  "sourceID": 7,
					  "targetID": 12,
					  "abilityGameID": 16192,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 15182,
					  "unmitigatedAmount": 15182,
					  "multiplier": 1,
					  "packetID": 52906
					},
					{
					  "timestamp": 2462756,
					  "type": "cast",
					  "sourceID": 7,
					  "targetID": 12,
					  "abilityGameID": 7,
					  "fight": 6,
					  "melee": true
					},
					{
					  "timestamp": 2462756,
					  "type": "calculateddamage",
					  "sourceID": 7,
					  "targetID": 12,
					  "abilityGameID": 7,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 1816,
					  "unmitigatedAmount": 1816,
					  "multiplier": 1,
					  "packetID": 52907
					},
					{
					  "timestamp": 2462756,
					  "type": "refreshbuff",
					  "sourceID": 7,
					  "targetID": 7,
					  "abilityGameID": 1001823,
					  "fight": 6
					},
					{
					  "timestamp": 2462756,
					  "type": "removebuff",
					  "sourceID": 7,
					  "targetID": 7,
					  "abilityGameID": 1001818,
					  "fight": 6
					},
					{
					  "timestamp": 2462756,
					  "type": "applybuff",
					  "sourceID": 7,
					  "targetID": 7,
					  "abilityGameID": 1001821,
					  "fight": 6,
					  "extraAbilityGameID": 16192
					},
					{
					  "timestamp": 2462756,
					  "type": "applybuff",
					  "sourceID": 7,
					  "targetID": 7,
					  "abilityGameID": 1001847,
					  "fight": 6,
					  "extraAbilityGameID": 16192
					},
					{
					  "timestamp": 2462889,
					  "type": "damage",
					  "sourceID": 2,
					  "targetID": 12,
					  "abilityGameID": 3579,
					  "fight": 6,
					  "hitType": 2,
					  "amount": 14410,
					  "unmitigatedAmount": 14410,
					  "directHit": true,
					  "packetID": 52903,
					  "multiplier": 1
					},
					{
					  "timestamp": 2462889,
					  "type": "damage",
					  "sourceID": 3,
					  "targetID": 12,
					  "abilityGameID": 7384,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 3598,
					  "unmitigatedAmount": 3598,
					  "packetID": 52904,
					  "multiplier": 1
					},
					{
					  "timestamp": 2462889,
					  "type": "cast",
					  "sourceID": 12,
					  "targetID": 3,
					  "abilityGameID": 27763,
					  "fight": 6,
					  "melee": true
					},
					{
					  "timestamp": 2462889,
					  "type": "calculateddamage",
					  "sourceID": 12,
					  "targetID": 3,
					  "abilityGameID": 27763,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 2818,
					  "unmitigatedAmount": 2818,
					  "multiplier": 1,
					  "packetID": 52908
					},
					{
					  "timestamp": 2462889,
					  "type": "cast",
					  "sourceID": 3,
					  "targetID": 12,
					  "abilityGameID": 16461,
					  "fight": 6
					},
					{
					  "timestamp": 2462889,
					  "type": "calculateddamage",
					  "sourceID": 3,
					  "targetID": 12,
					  "abilityGameID": 16461,
					  "fight": 6,
					  "hitType": 2,
					  "amount": 3223,
					  "unmitigatedAmount": 3223,
					  "multiplier": 1,
					  "packetID": 52909
					},
					{
					  "timestamp": 2462978,
					  "type": "cast",
					  "sourceID": 8,
					  "targetID": 12,
					  "abilityGameID": 25865,
					  "fight": 6
					},
					{
					  "timestamp": 2462978,
					  "type": "calculateddamage",
					  "sourceID": 8,
					  "targetID": 12,
					  "abilityGameID": 25865,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 6982,
					  "unmitigatedAmount": 6982,
					  "multiplier": 1,
					  "packetID": 52910
					},
					{
					  "timestamp": 2463067,
					  "type": "cast",
					  "sourceID": 5,
					  "targetID": 5,
					  "abilityGameID": 18805,
					  "fight": 6
					},
					{
					  "timestamp": 2463067,
					  "type": "refreshbuff",
					  "sourceID": 7,
					  "targetID": 5,
					  "abilityGameID": 1001824,
					  "fight": 6
					},
					{
					  "timestamp": 2463067,
					  "type": "applybuffstack",
					  "sourceID": 5,
					  "targetID": 5,
					  "abilityGameID": 1000496,
					  "fight": 6,
					  "stack": 6
					},
					{
					  "timestamp": 2463067,
					  "type": "removebuff",
					  "sourceID": 5,
					  "targetID": 5,
					  "abilityGameID": 1000614,
					  "fight": 6
					},
					{
					  "timestamp": 2463288,
					  "type": "damage",
					  "sourceID": 7,
					  "targetID": 12,
					  "abilityGameID": 7,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 1816,
					  "unmitigatedAmount": 1816,
					  "packetID": 52907,
					  "multiplier": 1
					},
					{
					  "timestamp": 2463333,
					  "type": "cast",
					  "sourceID": 4,
					  "targetID": 12,
					  "abilityGameID": 25762,
					  "fight": 6
					},
					{
					  "timestamp": 2463378,
					  "type": "damage",
					  "sourceID": 12,
					  "targetID": 3,
					  "abilityGameID": 27763,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 2818,
					  "unmitigatedAmount": 2818,
					  "packetID": 52908,
					  "multiplier": 1
					},
					{
					  "timestamp": 2463378,
					  "type": "removebuff",
					  "sourceID": 9,
					  "targetID": 3,
					  "abilityGameID": 1002607,
					  "fight": 6,
					  "absorbed": 9745,
					  "absorb": 0
					},
					{
					  "timestamp": 2463378,
					  "type": "refreshbuff",
					  "sourceID": 9,
					  "targetID": 3,
					  "abilityGameID": 1002605,
					  "fight": 6
					},
					{
					  "timestamp": 2463422,
					  "type": "damage",
					  "sourceID": 7,
					  "targetID": 12,
					  "abilityGameID": 16192,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 15182,
					  "unmitigatedAmount": 15182,
					  "packetID": 52906,
					  "multiplier": 1
					},
					{
					  "timestamp": 2463466,
					  "type": "damage",
					  "sourceID": 3,
					  "targetID": 12,
					  "abilityGameID": 16461,
					  "fight": 6,
					  "hitType": 2,
					  "amount": 3223,
					  "unmitigatedAmount": 3223,
					  "packetID": 52909,
					  "multiplier": 1
					},
					{
					  "timestamp": 2463512,
					  "type": "cast",
					  "sourceID": 9,
					  "targetID": 12,
					  "abilityGameID": 24318,
					  "fight": 6
					},
					{
					  "timestamp": 2463512,
					  "type": "calculateddamage",
					  "sourceID": 9,
					  "targetID": 12,
					  "abilityGameID": 24318,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 7950,
					  "unmitigatedAmount": 7950,
					  "multiplier": 1,
					  "packetID": 52913
					},
					{
					  "timestamp": 2463512,
					  "type": "heal",
					  "sourceID": 9,
					  "targetID": 5,
					  "abilityGameID": 1002623,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 0,
					  "overheal": 10342,
					  "tick": true
					},
					{
					  "timestamp": 2463512,
					  "type": "heal",
					  "sourceID": 9,
					  "targetID": 2,
					  "abilityGameID": 1002623,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 0,
					  "overheal": 10429,
					  "tick": true
					},
					{
					  "timestamp": 2463512,
					  "type": "heal",
					  "sourceID": 9,
					  "targetID": 6,
					  "abilityGameID": 1002623,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 0,
					  "overheal": 10597,
					  "tick": true
					},
					{
					  "timestamp": 2463512,
					  "type": "heal",
					  "sourceID": 9,
					  "targetID": 8,
					  "abilityGameID": 1002623,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 0,
					  "overheal": 10448,
					  "tick": true
					},
					{
					  "timestamp": 2463512,
					  "type": "heal",
					  "sourceID": 9,
					  "targetID": 7,
					  "abilityGameID": 1002623,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 0,
					  "overheal": 10668,
					  "tick": true
					},
					{
					  "timestamp": 2463512,
					  "type": "heal",
					  "sourceID": 9,
					  "targetID": 3,
					  "abilityGameID": 1002623,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 2818,
					  "overheal": 7499,
					  "tick": true
					},
					{
					  "timestamp": 2463512,
					  "type": "heal",
					  "sourceID": 9,
					  "targetID": 9,
					  "abilityGameID": 1002623,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 0,
					  "overheal": 10393,
					  "tick": true
					},
					{
					  "timestamp": 2463512,
					  "type": "heal",
					  "sourceID": 9,
					  "targetID": 4,
					  "abilityGameID": 1002623,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 0,
					  "overheal": 10762,
					  "tick": true
					},
					{
					  "timestamp": 2463600,
					  "type": "begincast",
					  "sourceID": 2,
					  "targetID": 12,
					  "abilityGameID": 3579,
					  "fight": 6,
					  "duration": 1450
					},
					{
					  "timestamp": 2463645,
					  "type": "cast",
					  "sourceID": 5,
					  "targetID": 5,
					  "abilityGameID": 18807,
					  "fight": 6
					},
					{
					  "timestamp": 2463645,
					  "type": "removebuffstack",
					  "sourceID": 5,
					  "targetID": 5,
					  "abilityGameID": 1000496,
					  "fight": 6,
					  "stack": 1
					},
					{
					  "timestamp": 2463734,
					  "type": "cast",
					  "sourceID": 3,
					  "targetID": 12,
					  "abilityGameID": 9,
					  "fight": 6
					},
					{
					  "timestamp": 2463734,
					  "type": "calculateddamage",
					  "sourceID": 3,
					  "targetID": 12,
					  "abilityGameID": 9,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 2754,
					  "unmitigatedAmount": 2754,
					  "multiplier": 1,
					  "packetID": 52915
					},
					{
					  "timestamp": 2463779,
					  "type": "damage",
					  "sourceID": 8,
					  "targetID": 12,
					  "abilityGameID": 25865,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 6982,
					  "unmitigatedAmount": 6982,
					  "packetID": 52910,
					  "multiplier": 1
					},
					{
					  "timestamp": 2464134,
					  "type": "cast",
					  "sourceID": 4,
					  "targetID": 12,
					  "abilityGameID": 74,
					  "fight": 6
					},
					{
					  "timestamp": 2464134,
					  "type": "calculateddamage",
					  "sourceID": 4,
					  "targetID": 12,
					  "abilityGameID": 74,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 6904,
					  "unmitigatedAmount": 6904,
					  "directHit": true,
					  "multiplier": 1,
					  "packetID": 52916
					},
					{
					  "timestamp": 2464134,
					  "type": "cast",
					  "sourceID": 4,
					  "targetID": 12,
					  "abilityGameID": 7,
					  "fight": 6,
					  "melee": true
					},
					{
					  "timestamp": 2464134,
					  "type": "calculateddamage",
					  "sourceID": 4,
					  "targetID": 12,
					  "abilityGameID": 7,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 1251,
					  "unmitigatedAmount": 1251,
					  "multiplier": 1,
					  "packetID": 52917
					},
					{
					  "timestamp": 2464134,
					  "type": "cast",
					  "sourceID": 6,
					  "targetID": 12,
					  "abilityGameID": 7386,
					  "fight": 6
					},
					{
					  "timestamp": 2464134,
					  "type": "calculateddamage",
					  "sourceID": 6,
					  "targetID": 12,
					  "abilityGameID": 7386,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 2167,
					  "unmitigatedAmount": 2167,
					  "multiplier": 1,
					  "packetID": 52918
					},
					{
					  "timestamp": 2464134,
					  "type": "removebuff",
					  "sourceID": 4,
					  "targetID": 4,
					  "abilityGameID": 1002513,
					  "fight": 6
					},
					{
					  "timestamp": 2464134,
					  "type": "applybuff",
					  "sourceID": 4,
					  "targetID": 4,
					  "abilityGameID": 1000108,
					  "fight": 6,
					  "extraAbilityGameID": 74
					},
					{
					  "timestamp": 2464134,
					  "type": "applybuff",
					  "sourceID": 4,
					  "targetID": 4,
					  "abilityGameID": 1001861,
					  "fight": 6,
					  "extraAbilityGameID": 74
					},
					{
					  "timestamp": 2464179,
					  "type": "cast",
					  "sourceID": 5,
					  "targetID": 12,
					  "abilityGameID": 2271,
					  "fight": 6
					},
					{
					  "timestamp": 2464179,
					  "type": "calculateddamage",
					  "sourceID": 5,
					  "targetID": 12,
					  "abilityGameID": 2271,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 8950,
					  "unmitigatedAmount": 8950,
					  "multiplier": 1.05,
					  "packetID": 52919
					},
					{
					  "timestamp": 2464179,
					  "type": "removebuff",
					  "sourceID": 5,
					  "targetID": 5,
					  "abilityGameID": 1000496,
					  "fight": 6
					},
					{
					  "timestamp": 2464179,
					  "type": "applybuff",
					  "sourceID": 5,
					  "targetID": 5,
					  "abilityGameID": 1000507,
					  "fight": 6,
					  "extraAbilityGameID": 2271
					},
					{
					  "timestamp": 2464223,
					  "type": "cast",
					  "sourceID": 7,
					  "targetID": 7,
					  "abilityGameID": 15998,
					  "fight": 6
					},
					{
					  "timestamp": 2464223,
					  "type": "cast",
					  "sourceID": 3,
					  "targetID": 12,
					  "abilityGameID": 7,
					  "fight": 6,
					  "melee": true
					},
					{
					  "timestamp": 2464223,
					  "type": "calculateddamage",
					  "sourceID": 3,
					  "targetID": 12,
					  "abilityGameID": 7,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 917,
					  "unmitigatedAmount": 917,
					  "multiplier": 1,
					  "packetID": 52921
					},
					{
					  "timestamp": 2464223,
					  "type": "applybuff",
					  "sourceID": 7,
					  "targetID": 7,
					  "abilityGameID": 1001819,
					  "fight": 6,
					  "extraAbilityGameID": 15998
					},
					{
					  "timestamp": 2464356,
					  "type": "damage",
					  "sourceID": 3,
					  "targetID": 12,
					  "abilityGameID": 9,
					  "fight": 6,
					  "hitType": 1,
					  "amount": 2754,
					  "unmitigatedAmount": 2754,
					  "packetID": 52915,
					  "multiplier": 1
					}
				  ],
				  "nextPageTimestamp": 2469828
				}
			  }
			  }
			 }
								""";
}
