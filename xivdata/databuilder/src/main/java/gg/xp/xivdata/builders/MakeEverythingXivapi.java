package gg.xp.xivdata.builders;

import gg.xp.xivapi.XivApiClient;
import gg.xp.xivapi.clienttypes.XivApiObject;
import gg.xp.xivapi.pagination.XivApiPaginator;
import gg.xp.xivdata.builders.models.ContentFinderCondition;
import gg.xp.xivdata.builders.models.NpcYell;
import gg.xp.xivdata.builders.models.PlaceName;
import gg.xp.xivdata.builders.models.TerritoryType;
import gg.xp.xivdata.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.zip.GZIPOutputStream;

/*
TODO:
- Pipeline for this
- Migrate the rest of stuff from SC to this
 */
public class MakeEverythingXivapi {
	private static final Logger log = LoggerFactory.getLogger(MakeEverythingXivapi.class);

	private final Path outputPathBase;
	private final XivApiClient client;

	public MakeEverythingXivapi(Path outputPathBase, XivApiClient client) {
		this.outputPathBase = outputPathBase;
		this.client = client;
	}

	public static void main(String[] args) throws Throwable {
		Path outputPathBase = Path.of("src", "main", "resources", "xiv");
		// Strongly recommended to use a local BM install rather than the live server
		String server = System.getProperty("xivapi-server", "https://bm.xivgear.app/api/1");
		XivApiClient client = new XivApiClient(builder -> {
			try {
				builder.setBaseUri(new URI(server));
			}
			catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		});
		MakeEverythingXivapi maker = new MakeEverythingXivapi(outputPathBase, client);

		maker.writeList(TerritoryType.class, entry -> {
			PlaceName place = entry.getPlaceName();
			ContentFinderCondition cfc = entry.getContentFinderCondition();

			return new ZoneInfo(entry.getRowId(), cfc == null ? null : cfc.getName(), place == null ? null : place.getName());
		}, List.of("territory", "TerritoryType.oos.gz"));
		maker.writeList(NpcYell.class, entry -> {
			return new NpcYellInfo(entry.getRowId(), entry.getText());
		}, List.of("npcyell", "NpcYell.oos.gz"));
	}

	private <In extends XivApiObject, Out extends Serializable> void writeList(Class<In> xivApiClass, Function<In, Out> mapper, List<String> path) {
		log.info("Loading {}...", xivApiClass.getSimpleName());
		XivApiPaginator<In> pager = client.getListIterator(xivApiClass);
		List<Out> out = pager.toBufferedStream(10).parallel().map(mapper).toList();
		log.info("Found {} entries of {}", out.size(), xivApiClass.getSimpleName());
		Path outputPath = outputPathBase;
		for (String pathPart : path) {
			outputPath = outputPath.resolve(pathPart);
		}
		try (var faos = new FileOutputStream(outputPath.toFile());
		     var gzos = new GZIPOutputStream(faos);
		     var oos = new ObjectOutputStream(gzos)) {
			oos.writeObject(out);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.info("Wrote {} entries of {}", out.size(), xivApiClass.getSimpleName());

	}
}
