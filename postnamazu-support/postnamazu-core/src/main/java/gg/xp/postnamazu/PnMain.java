package gg.xp.postnamazu;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.ws.ActWsLogSource;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.EnumSetting;
import gg.xp.xivsupport.persistence.settings.HttpURISetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.sys.KnownLogSource;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PnMain implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(PnMain.class);
	// Being used as a queue
	private final ExecutorService cmdQueue = Executors.newSingleThreadExecutor();
	private final ExecutorService amQueue = Executors.newSingleThreadExecutor();
	// Handles the actual execution
	private final ExecutorService exs = Executors.newCachedThreadPool();
	private final HttpClient http = HttpClient.newBuilder().build();
	private final ObjectMapper mapper = new ObjectMapper();
	private final HttpURISetting uriSetting;
	private final EnumSetting<PnMode> modeSetting;
	private final EventMaster master;

	private volatile PnStatus status = PnStatus.UNKNOWN;
	private final PrimaryLogSource pls;
	private final IntSetting cmdDelayBase;
	private final IntSetting cmdDelayPlus;
	private final IntSetting amDelayBase;
	private final IntSetting amDelayPlus;
	private final ActWsLogSource ws;

	public PnMain(EventMaster master, PersistenceProvider pers, PrimaryLogSource pls, ActWsLogSource ws) {
		this.master = master;
		this.pls = pls;
		this.ws = ws;
		try {
			uriSetting = new HttpURISetting(pers, "pn-support.uri", new URI("http://localhost:2019/"));
			cmdDelayBase = new IntSetting(pers, "pn-support.base-cmd-delay", 150, 0, 5000);
			cmdDelayPlus = new IntSetting(pers, "pn-support.plus-cmd-delay", 150, 0, 5000);
			amDelayBase = new IntSetting(pers, "pn-support.base-am-delay", 150, 0, 5000);
			amDelayPlus = new IntSetting(pers, "pn-support.plus-am-delay", 150, 0, 5000);
			modeSetting = new EnumSetting<>(pers, "pn-support.mode", PnMode.class, PnMode.OP);
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@HandleEvents
	public void handleOutgoing(EventContext context, PnOutgoingMessage message) {
		Runnable task = () -> {
			try {
				sendMessageDirectly(message.getCommand(), message.getPayload());
				updateStatus(PnStatus.GOOD);
			}
			catch (Throwable e) {
				updateStatus(PnStatus.BAD);
			}
		};
		PnQueueType queue = message.getQueueType();
		switch (queue) {
			case COMMAND -> delayedEnqueue(task, cmdQueue, cmdDelayBase.get(), cmdDelayPlus.get());
			case MARK -> delayedEnqueue(task, amQueue, cmdDelayBase.get(), cmdDelayPlus.get());
			default -> exs.submit(task);
		}
	}

	private void delayedEnqueue(Runnable task, ExecutorService queue, int minDelay, int plusDelay) {
		queue.submit(() -> {
			try {
				// Insert delay to avoid spamming
				int delay = (int) (minDelay + (Math.random() * plusDelay));
				Thread.sleep(delay);
			}
			catch (InterruptedException e) {
				log.error("Interrupted", e);
			}
			finally {
				exs.submit(task);
			}
		});
	}

	private void updateStatus(PnStatus status) {
		this.status = status;
	}

	public PnResponse sendMessageDirectly(String command, Object payload) {
		String body;
		try {
			body = payload instanceof String sp ? sp : mapper.writeValueAsString(payload);
		}
		catch (JacksonException e) {
			throw new RuntimeException(e);
		}
		return switch (modeSetting.get()) {
			case HTTP -> {
				try {
					log.info("PostNamazu http outgoing: ({}): '{}'", command, body);
					HttpResponse<String> response = http.send(
							HttpRequest
									.newBuilder(uriSetting.get().resolve(command))
									.POST(
											HttpRequest.BodyPublishers
													.ofString(body)).build(),
							HttpResponse.BodyHandlers.ofString());
					if (response.statusCode() != 200) {
						log.error("PostNamazu: Response failure: {}: {}", response.statusCode(), response.body());
					}
					yield new PnHttpResponse(response);
				}
				catch (IOException | InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			case OP -> {
				ws.sendObject(Map.of(
						"call", "PostNamazu",
						"c", command,
						"p", body,
						"rseq", "pn-rseq"));
				yield new PnOpResponse();
			}
		};
	}

	@Override
	public boolean enabled(EventContext context) {
		return pls.getLogSource() == KnownLogSource.WEBSOCKET_LIVE;
	}

	public IntSetting getAmDelayBase() {
		return amDelayBase;
	}

	public IntSetting getAmDelayPlus() {
		return amDelayPlus;
	}

	public IntSetting getCmdDelayBase() {
		return cmdDelayBase;
	}

	public IntSetting getCmdDelayPlus() {
		return cmdDelayPlus;
	}

	public PnStatus getStatus() {
		return status;
	}

	public HttpURISetting getUriSetting() {
		return uriSetting;
	}

	public EnumSetting<PnMode> getModeSetting() {
		return modeSetting;
	}
}
