package gg.xp.events.ws;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.EventDistributor;
import gg.xp.events.EventMaster;
import gg.xp.events.debug.DebugCommand;
import gg.xp.events.state.RefreshCombatantsRequest;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ActWsLogSource {

	private static final Logger log = LoggerFactory.getLogger(ActWsLogSource.class);
	private static final ExecutorService taskPool = Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder().namingPattern("ActWsLogPool-%d").build());
	private final Object connectLock = new Object();

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
			subscribeEvents();
			state.setConnected(true);
			eventConsumer.accept(new ActWsConnectedEvent());
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
			state.setConnected(false);
			eventConsumer.accept(new ActWsDisconnectedEvent());
		}

		@Override
		public void onError(Exception e) {
			log.error("WS Error!", e);
			// TODO: hmmm.....is an "error" always fatal here? We *should* reconnect just in case, but I just don't
			// know how to differentiate between fatal (which we should ignore, because onClose will get called next)
			// and non-fatal (we need to reconnect it outselves).
			eventConsumer.accept(new ActWsReconnectRequest());
		}

		private void subscribeEvents() {
			log.info("Subscribing to WS events");
			send("{\"call\":\"subscribe\",\"events\":[\"ChangePrimaryPlayer\"]}");
			send("{\"call\":\"subscribe\",\"events\":[\"ChangeZone\"]}");
			send("{\"call\":\"subscribe\",\"events\":[\"PartyChanged\"]}");
//		send("{\"call\":\"subscribe\",\"events\":[\"onPlayerChangedEvent\"]}");
			send("{\"call\":\"subscribe\",\"events\":[\"onInCombatChangedEvent\"]}");
			send("{\"call\":\"subscribe\",\"events\":[\"LogLine\"]}");
			send("{\"call\":\"subscribe\",\"events\":[\"onPartyWipe\"]}");
			log.info("Subscribed to WS events");

		}
	}

	private final Consumer<Event> eventConsumer;
	private final ActWsClientInternal client;
	private final WsState state;

	public ActWsLogSource(EventMaster master) {
		this.eventConsumer = master::pushEvent;
		EventDistributor<Event> distributor = master.getDistributor();
		distributor.registerHandler(DebugCommand.class, this::getCombatantsDbg);
		distributor.registerHandler(DebugCommand.class, this::forceReconnect);
		distributor.registerHandler(RefreshCombatantsRequest.class, this::getCombatants);
		distributor.registerHandler(ActWsReconnectRequest.class, this::requestReconnect);
		distributor.registerHandler(ActWsDisconnectedEvent.class, this::reconnectActWs);
		this.client = new ActWsClientInternal();
		state = new WsState();
		distributor.getStateStore().putCustom(WsState.class, state);
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
			log.info("Reconnect requested");
			context.accept(new ActWsReconnectRequest());
		}
	}

	public void getCombatants(EventContext<Event> context, RefreshCombatantsRequest event) {
		client.send(String.format("{\"call\":\"getCombatants\",\"rseq\":%s}", rseqCounter.getAndIncrement()));
	}

	public void requestReconnect(EventContext<Event> context, ActWsReconnectRequest event) {
		log.info("Forcing connection closed, should auto-reconnect");
		doClose();
	}

	public void reconnectActWs(EventContext<Event> context, ActWsDisconnectedEvent event) {
		log.info("Disconnected, reconnecting");
		doReconnect();
	}


	private void doClose() {
		taskPool.submit(() -> {
			synchronized (connectLock) {
				if (!state.isConnected()) {
					log.info("Ignoring request to close because we are already disconnected");
					return;
				}
				log.info("Disconnecting");
				try {
					client.closeBlocking();
					log.info("Disconnected");
				}
				catch (InterruptedException e) {
					log.error("Interrupted", e);
				}
			}
		});
	}

	private void doOpen() {
		taskPool.submit(() -> {
			synchronized (connectLock) {
				if (state.isConnected()) {
					log.info("Ignoring request to open because we are already connected");
					return;
				}
				log.info("Connecting");
				try {
					client.connectBlocking();
					log.info("Connected");
				}
				catch (InterruptedException e) {
					log.error("Interrupted", e);
				}
			}
		});
	}

	private void doReconnect() {
		taskPool.submit(() -> {
			synchronized (connectLock) {
				if (state.isConnected()) {
					log.info("Ignoring request to open because we are already connected");
					return;
				}
				log.info("Reconnecting");
				try {
					client.reconnectBlocking();
					log.info("Reconnected");
				}
				catch (InterruptedException e) {
					log.error("Interrupted", e);
				}
			}
		});
	}

	public void start() {
		// TODO: auto retry and reconnection
		log.info("Attempting connection");
		doOpen();
	}


}
