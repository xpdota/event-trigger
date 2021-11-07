package gg.xp.events.ws;

import gg.xp.events.Event;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
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
			if (s.contains("LogLine")) {
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
		}

		@Override
		public void onError(Exception e) {
			log.error("Error, reconnecting", e);
			reconnect();
		}
	}

	private final Consumer<Event> eventConsumer;
	private final ActWsClientInternal client;

	public ActWsLogSource(Consumer<Event> eventConsumer) {
		this.eventConsumer = eventConsumer;
		this.client = new ActWsClientInternal();
	}

	public void start() {
		// TODO: auto retry and reconnection
		try {
			client.connectBlocking();
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
		client.send("{\"call\":\"subscribe\",\"events\":[\"PartyChanged\"]}");
//		client.send("{\"call\":\"subscribe\",\"events\":[\"onPlayerChangedEvent\"]}");
		client.send("{\"call\":\"subscribe\",\"events\":[\"ChangePrimaryPlayer\"]}");
		client.send("{\"call\":\"subscribe\",\"events\":[\"ChangeZone\"]}");
		client.send("{\"call\":\"subscribe\",\"events\":[\"onInCombatChangedEvent\"]}");
		client.send("{\"call\":\"subscribe\",\"events\":[\"LogLine\"]}");
		client.send("{\"call\":\"subscribe\",\"events\":[\"onPartyWipe\"]}");
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
