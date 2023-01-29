package gg.xp.telestosupport.subscriber;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.telestosupport.TelestoMain;
import gg.xp.telestosupport.TelestoStatusUpdatedEvent;
import gg.xp.xivsupport.sys.Threading;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

/*
	Class for the whole "subscribe to telesto stuff" thing.
	Not currently shipped (i.e. not listed as a dependency in the 'launcher' module),
	because the trid subscriptions don't work *at all* due to a typo in Telesto.
 */

public class TelestoSubscriber {

	private static final Logger log = LoggerFactory.getLogger(TelestoSubscriber.class);
	private final EventMaster master;
	private final TelestoMain telestoMain;
	private final Object epLock = new Object();
	private final ObjectMapper mapper = new ObjectMapper();
	private volatile @Nullable Endpoint endpoint;

	private static final int ID = 678_473;
	private static final String TR_SUB_ID = "te_tr_subscription";

	public TelestoSubscriber(EventMaster master, TelestoMain telestoMain) {
		this.master = master;
		this.telestoMain = telestoMain;
	}

	@HandleEvents(order = 1_000)
	public void init(EventContext context, TelestoStatusUpdatedEvent event) {
		master.pushEvent(telestoMain.makeMessage(ID, "Unsubscribe", Map.of("id", TR_SUB_ID), false));
		master.pushEvent(telestoMain.makeMessage(ID, "Subscribe", Map.of("id", TR_SUB_ID, "type", "trid", "endpoint", getIncomingEndpointURLstring()), false));
	}

	public String getIncomingEndpointURLstring() {
		return getIncomingEndpointURL().toString();
	}

	public URL getIncomingEndpointURL() {
		return getEndpoint().getUrl();
	}

	private Endpoint getEndpoint() {
		if (endpoint == null) {
			synchronized (epLock) {
				if (endpoint == null) {
					return endpoint = new Endpoint();
				}
			}
		}
		return endpoint;
	}


	private final class Endpoint {
		private static final ThreadFactory tf = Threading.namedDaemonThreadFactory("TelestoResponseHandler");
		private final HttpServer server;

		private Endpoint() {
			try {
				server = HttpServer.create(new InetSocketAddress(0), 20);
				server.setExecutor(command -> tf.newThread(command).start());
				server.createContext("/", this::handle);
				server.start();
				log.info("Telesto receiver started on port {}", getLocalPort());
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private void handle(HttpExchange httpExchange) {
			try {

				TelestoSubscriptionMessage message = mapper.readValue(httpExchange.getRequestBody(), new TypeReference<>() {
				});
//				log.info("Response: {}", message);
				httpExchange.sendResponseHeaders(200, -1);
				httpExchange.close();
				master.pushEvent(message);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private int getLocalPort() {
			return server.getAddress().getPort();
		}

		private URL getUrl() {
			// TODO: allow hostname to be overridden
			try {
				return new URL("http", "localhost", getLocalPort(), "/");
			}
			catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
