package gg.xp.events.ws;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.EventDistributor;
import gg.xp.events.EventMaster;
import gg.xp.events.debug.DebugCommand;
import gg.xp.events.state.RefreshCombatantsRequest;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ActWsLogSource {

	private static final Logger log = LoggerFactory.getLogger(ActWsLogSource.class);

	private final class ActWsClientInternal extends WebSocketClient {

		public ActWsClientInternal() {
			super(URI.create("ws://127.0.0.1:10501/ws"));
		}

		public ActWsClientInternal(String uriStr) {
			super(URI.create(uriStr));
		}

		@Override
		public void onOpen(ServerHandshake serverHandshake) {
			log.info("Open: {}", serverHandshake);
		}

		@Override
		public void onMessage(String s) {
			if (s.contains("LogLine") || s.contains("combatants")) {
				log.trace("Message: {}", s);
			}
			else {
				log.info("Message: {}", s);
			}
			eventConsumer.accept(new ActWsRawMsg(s));
		}

		@Override
		public void onClose(int i, String s, boolean b) {
			log.info("Close: {};{};{}", i, s, b);
			eventConsumer.accept(new ReconnectActWsEvent());
		}

		@Override
		public void onError(Exception e) {
			log.error("Error, reconnecting", e);
			eventConsumer.accept(new ReconnectActWsEvent());
		}
	}

	private final Consumer<Event> eventConsumer;
	private final ActWsClientInternal client;

	public ActWsLogSource(EventMaster master) {
		this.eventConsumer = master::pushEvent;
		EventDistributor<Event> distributor = master.getDistributor();
		distributor.registerHandler(DebugCommand.class, this::getCombatantsDbg);
		distributor.registerHandler(DebugCommand.class, this::forceReconnect);
		distributor.registerHandler(RefreshCombatantsRequest.class, this::getCombatants);
		distributor.registerHandler(ReconnectActWsEvent.class, this::reconnectActWs);
		this.client = new ActWsClientInternal();
	}

	private final AtomicInteger rseqCounter = new AtomicInteger();

	// NOT tagged with handler since we don't want this to be auto-scanned
	public void getCombatantsDbg(EventContext<Event> context, DebugCommand event) {
		if (event.getCommand().equals("combatants")) {
			context.accept(new RefreshCombatantsRequest());
		}
	}

	public void forceReconnect(EventContext<Event> context, DebugCommand event) {
		if (event.getCommand().equals("reconnect_ws")) {
			client.getConnection().close();
		}
	}

	public void getCombatants(EventContext<Event> context, RefreshCombatantsRequest event) {
		client.send(String.format("{\"call\":\"getCombatants\",\"rseq\":%s}", rseqCounter.getAndIncrement()));
	}

	public void reconnectActWs(EventContext<Event> context, ReconnectActWsEvent event) {
		log.info("Reconnect requested");
		try {
			client.reconnectBlocking();
		}
		catch (InterruptedException e) {
			// TODO: retry logic
			log.error("Could not reconnect. ", e);
			return;
		}
		subscribeEvents();

	}


	public void start() {
		// TODO: auto retry and reconnection
		try {
			client.connectBlocking();
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
		subscribeEvents();
	}

	private void subscribeEvents() {
		log.info("Subscribing to WS events");
		client.send("{\"call\":\"subscribe\",\"events\":[\"ChangePrimaryPlayer\"]}");
		client.send("{\"call\":\"subscribe\",\"events\":[\"ChangeZone\"]}");
		client.send("{\"call\":\"subscribe\",\"events\":[\"PartyChanged\"]}");
//		client.send("{\"call\":\"subscribe\",\"events\":[\"onPlayerChangedEvent\"]}");
		client.send("{\"call\":\"subscribe\",\"events\":[\"onInCombatChangedEvent\"]}");
		client.send("{\"call\":\"subscribe\",\"events\":[\"LogLine\"]}");
		client.send("{\"call\":\"subscribe\",\"events\":[\"onPartyWipe\"]}");
		log.info("Subscribed to WS events");

	}

	public static void main(String[] args) throws InterruptedException {
		// /MiniParse has player info and stuff
//		WebSocketClient wsc = new ActWsClientInternal();
//		wsc.connectBlocking();
//		wsc.send("{\"call\":\"subscribe\",\"events\":[\"PartyChanged\"]}");
////		wsc.send("{\"call\":\"subscribe\",\"events\":[\"onPlayerChangedEvent\"]}");
//		wsc.send("{\"call\":\"subscribe\",\"events\":[\"ChangePrimaryPlayer\"]}");
//		wsc.send("{\"call\":\"subscribe\",\"events\":[\"ChangeZone\"]}");
//		wsc.send("{\"call\":\"subscribe\",\"events\":[\"onInCombatChangedEvent\"]}");
//		wsc.send("{\"call\":\"subscribe\",\"events\":[\"LogLine\"]}");

		while (true) {
			Thread.sleep(10000);
		}
	}


}
