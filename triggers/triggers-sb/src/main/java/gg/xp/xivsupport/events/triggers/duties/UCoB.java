package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.ChatLineEvent;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.XivStateImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CalloutRepo(name = "The Unending Coil of Bahamut", duty = KnownDuty.UCoB)
public class UCoB extends AutoChildEventHandler implements FilteredEventHandler {

	public UCoB(XivState state) {this.state = state; }

	public XivState getState() {return state;}

	private static final Logger log = LoggerFactory.getLogger(UCoB.class);
	private final XivState state;

	//Twintania
	private final ModifiableCallout<AbilityCastStart> twister = ModifiableCallout.durationBasedCall("Twister", "Twister");
	private final ModifiableCallout<AbilityCastStart> plummet = ModifiableCallout.durationBasedCall("Plummet", "Frontal");
	private final ModifiableCallout<HeadMarkerEvent> fireballOnYou = new ModifiableCallout<>("Fireball (headmark)", "Stack");
	private final ModifiableCallout<AbilityCastStart> deathSentece = ModifiableCallout.durationBasedCall("Death Sentence");
	private final ModifiableCallout<AbilityCastStart> deathSenteceOnYou =  ModifiableCallout.durationBasedCall("Death Sentence","Death Sentence on you!");

	private final ModifiableCallout<AbilityCastStart> liquidHell = ModifiableCallout.durationBasedCall("Liquid Hell", "Move");

	//Nael quotes
	private final ModifiableCallout<ChatLineEvent> inStack = new ModifiableCallout<>("O hallowed moon, take fire and scorch my foes!", "in stack");
	private final ModifiableCallout<ChatLineEvent> inOut = new ModifiableCallout<>("O hallowed moon, shine you the iron path!", "in out");
	private final ModifiableCallout<ChatLineEvent> stackOut = new ModifiableCallout<>("Blazing path, lead me to iron rule!", "stack out");
	private final ModifiableCallout<ChatLineEvent> stackIn = new ModifiableCallout<>("Take fire, O hallowed moon!", "stack in");
	private final ModifiableCallout<ChatLineEvent> spreadOut = new ModifiableCallout<>("From on high I descend, the iron path to call!", "spread out");
	private final ModifiableCallout<ChatLineEvent> spreadOutMore = new ModifiableCallout<>("From on high I descend, the iron path to walk!", "spread out");
	private final ModifiableCallout<ChatLineEvent> spreadIn = new ModifiableCallout<>("From on high I descend, the hallowed moon to call!", "Spread in");
	private final ModifiableCallout<ChatLineEvent> tankBusterStack = new ModifiableCallout<>("Fleeting light! 'Neath the red moon, scorch you the earth!", "Away from tank and stack");
	private final ModifiableCallout<ChatLineEvent> spreadTankBuster = new ModifiableCallout<>("Fleeting light! Amid a rain of stars, exalt you the red moon! ", "spread away from tank");

	//Bahamut
	// Consider adding specific triggers for trios
	private final ModifiableCallout<ChatLineEvent> flatten = new ModifiableCallout<>("Flatten", "Flatten on you");
	private final ModifiableCallout<AbilityCastStart> flattenOnYou = ModifiableCallout.durationBasedCall("Flatten", "flatten on you");

	private final ModifiableCallout<ChatLineEvent> InSpreadNeuroLinkSpread = new ModifiableCallout<>("From hallowed moon I descend, a rain of stars to bring!", "In Spread neuro Spread");
	private final ModifiableCallout<ChatLineEvent> SpreadInNeuroLinkSpread = new ModifiableCallout<>("From on high I descend, the moon and stars to bring","Spread In neuro Spread");

	//Adds
	private final ModifiableCallout<ChatLineEvent> inOutSpread = new ModifiableCallout<>("From hallowed moon I bare iron, in my descent to wield", "In out spread");
	private final ModifiableCallout<ChatLineEvent> outBusterStack = new ModifiableCallout<>("Unbending iron, descend with fiery edge!","Out tankbuster Stack");
	private final ModifiableCallout<ChatLineEvent> inBusterStack = new ModifiableCallout<>("From hallowed moon I descend, upon burning earth to tread!", "In tankbuster stack");
	private final ModifiableCallout<ChatLineEvent> outStackBuster = new ModifiableCallout<>("Unbending iron, take fire and descend!", "Out stack tankbuster");

	//Golden
	// Todo: add triggers for Akh morn and Morn Afah
	@Override
	public boolean enabled(EventContext context) {
		return context.getStateInfo().get(XivStateImpl.class).zoneIs(0x2DD);
	}

	@HandleEvents
	public void basicCasts(EventContext context, AbilityCastStart event){
		int id = (int) event.getAbility().getId();
		ModifiableCallout<AbilityCastStart> call;
		switch (id) {
			case 0x26AA -> call = twister;
			case 0x26A8 -> call = plummet;
			case 0x26A9 -> call = deathSentece;
			case 0x26AD -> call = liquidHell;


			default -> {return;}
		}
		context.accept(call.getModified(event));
	}
	@HandleEvents
	public void ChatEvents(EventContext context, ChatLineEvent lineEvent){
		String identifier = lineEvent.getLine().toLowerCase();
		ModifiableCallout<ChatLineEvent> call;
		switch (identifier) {
			case "o hallowed moon, take fire and scorch my foes!" -> call = inStack;
			case "o hallowed moon, shine you the iron path!" -> call = inOut;
			case "blazing path, lead me to iron rule!" -> call = stackOut;
			case "take fire, o hallowed moon!" -> call = stackIn;
			case "from on high i descend, the iron path to call!" -> call = spreadOut;
			case "from on high i descend, the iron path to walk!" -> call = spreadOutMore;
			case "from on high i descend, the hallowed moon to call!" -> call = spreadIn;
			case "fleeting light! 'neath the red moon, scorch you the earth!" -> call = tankBusterStack;
			case "fleeting light! amid a rain of stars, exalt you the red moon!" -> call = spreadTankBuster;
			case "bahamut prime readies flatten." -> call = flatten;
			case "from hallowed moon i descend, a rain of stars to bring" -> call = InSpreadNeuroLinkSpread;
			case "from on high i descend, the moon and stars to bring" -> call = SpreadInNeuroLinkSpread;
			case "from hallowed moon i bare iron, in my descent to wield" -> call = inOutSpread;
			case "unbending iron, descend with fiery edge!" -> call = outBusterStack;
			case "from hallowed moon i descend, upon burning earth to tread!" -> call = inBusterStack;
			case "unbending iron, take fire and descend!" -> call = outStackBuster;

			default -> {return;}

		}
		context.accept(call.getModified(lineEvent));
	}
}
