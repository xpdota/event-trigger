package gg.xp.postnamazu;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.persistence.PersistenceProvider;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PnMain implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(PnMain.class);
	// Being used as a queue
	private static final ExecutorService cmdQueue = Executors.newSingleThreadExecutor();
	private static final ExecutorService amQueue = Executors.newSingleThreadExecutor();
	// Handles the actual execution
	private static final ExecutorService exs = Executors.newCachedThreadPool();
	private final HttpClient http = HttpClient.newBuilder().build();
	private final ObjectMapper mapper = new ObjectMapper();
	private final HttpURISetting uriSetting;
	private final EventMaster master;

	private volatile PnStatus status = PnStatus.UNKNOWN;
	private final PrimaryLogSource pls;
	private final IntSetting cmdDelayBase;
	private final IntSetting cmdDelayPlus;
	private final IntSetting amDelayBase;
	private final IntSetting amDelayPlus;

	public PnMain(EventMaster master, PersistenceProvider pers, PrimaryLogSource pls) {
		this.master = master;
		this.pls = pls;
		try {
			uriSetting = new HttpURISetting(pers, "pn-support.uri", new URI("http://localhost:2019/"));
			cmdDelayBase = new IntSetting(pers, "pn-support.base-cmd-delay", 150, 0, 5000);
			cmdDelayPlus = new IntSetting(pers, "pn-support.plus-cmd-delay", 150, 0, 5000);
			amDelayBase = new IntSetting(pers, "pn-support.base-am-delay", 150, 0, 5000);
			amDelayPlus = new IntSetting(pers, "pn-support.plus-am-delay", 150, 0, 5000);
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@HandleEvents
	public void pnGameCmd(EventContext context, PnGameCommand pgc) {
		context.accept(new PnOutgoingMessage("command", pgc.getCommand()));
	}

	@HandleEvents
	public void handleOutgoing(EventContext context, PnOutgoingMessage message) {
		Runnable task = () -> {
			try {
				HttpResponse<String> response = sendMessageDirectly(message.getCommand(), message.getPayload());
				updateStatus(PnStatus.GOOD);
			}
			catch (Throwable e) {
				updateStatus(PnStatus.BAD);
			}
		};
		if (message.getCommand().equals("command")) {
			cmdQueue.submit(() -> {
				try {
					// Insert delay to avoid spamming
					int delay = (int) (cmdDelayBase.get() + (Math.random() * cmdDelayPlus.get()));
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
		else if (message.getCommand().equals("mark")) {
			amQueue.submit(() -> {
				try {
					// Insert delay to avoid spamming
					int delay = (int) (amDelayBase.get() + (Math.random() * amDelayPlus.get()));
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
		else {
			exs.submit(task);
		}
	}

	private void updateStatus(PnStatus status) {
		this.status = status;
	}

	public HttpResponse<String> sendMessageDirectly(String command, Object payload) {
		String body;
		try {
			body = payload instanceof String sp ? sp : mapper.writeValueAsString(payload);
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
			return response;
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
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
}
