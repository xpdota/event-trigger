package gg.xp.xivdata.builders;

import gg.xp.xivapi.XivApiClient;
import gg.xp.xivapi.assets.ImageFormat;
import gg.xp.xivapi.clienttypes.XivApiObject;
import gg.xp.xivapi.pagination.XivApiPaginator;
import gg.xp.xivdata.builders.models.Action;
import gg.xp.xivdata.builders.models.ContentFinderCondition;
import gg.xp.xivdata.builders.models.Map;
import gg.xp.xivdata.builders.models.NpcYell;
import gg.xp.xivdata.builders.models.PlaceName;
import gg.xp.xivdata.builders.models.StatusEffect;
import gg.xp.xivdata.builders.models.TerritoryType;
import gg.xp.xivdata.data.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.LongStream;
import java.util.zip.GZIPOutputStream;

/*
TODO:
- Pipeline for this
- Migrate the rest of stuff from SC to this
 */
public class MakeEverythingXivapi {
	private static final Logger log = LoggerFactory.getLogger(MakeEverythingXivapi.class);
	private static final int DUMMY_ACTION_ICON = 405;

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

		ConcurrentLinkedQueue<Long> iconIds = new ConcurrentLinkedQueue<>();

		maker.writeList(TerritoryType.class, entry -> {
			PlaceName place = entry.getPlaceName();
			ContentFinderCondition cfc = entry.getContentFinderCondition();

			return new ZoneInfo(entry.getRowId(), cfc == null ? null : cfc.getName(), place == null ? null : place.getName());
		}, List.of("territory", "TerritoryType.oos.gz"));

		maker.writeList(NpcYell.class, entry -> new NpcYellInfo(entry.getRowId(), entry.getText()), List.of("npcyell", "NpcYell.oos.gz"));

		maker.writeList(StatusEffect.class, entry -> {
			int baseIconId = entry.getIcon().getId();
			StatusEffectInfo statusEffectInfo = new StatusEffectInfo(entry.getRowId(), baseIconId, entry.getMaxStacks(), entry.getName(), entry.getDescription(), entry.canDispel(), entry.isPermanent(), entry.getPartyListPriority(), entry.isFcBuff());
			if (baseIconId > 0) {
				List<Long> allIconIds = statusEffectInfo.getAllIconIds();
				log.trace("Status id {} - icons {}", statusEffectInfo.statusEffectId(), allIconIds);
				iconIds.addAll(allIconIds);
			}
			return statusEffectInfo;
		}, List.of("statuseffect", "StatusEffect.oos.gz"));

		Pattern omenConePattern = Pattern.compile("_fan(\\d+)_");
		iconIds.add((long) DUMMY_ACTION_ICON);
		maker.writeList(Action.class, entry -> {
			String omenPath = entry.getOmen().getPath();
			boolean isConeAngleKnown = false;
			int coneAngle = 30; // TODO
			if (!StringUtils.isBlank(omenPath)) {
				Matcher matcher = omenConePattern.matcher(omenPath);
				if (matcher.matches()) {
					coneAngle = Integer.parseInt(matcher.group(1));
					isConeAngleKnown = true;
				}
			}
			ActionInfo ai = new ActionInfo(entry.getRowId(), entry.getName(), entry.getIcon().getId(), entry.getRecastRaw(), entry.getMaxCharges(), entry.getCategoryRaw(), entry.isPlayerAbility(), entry.getCastRaw(), entry.getCastType(), entry.getEffectRange(), entry.getXAxisModifier(), coneAngle, isConeAngleKnown, entry.getDescription());
			long id = entry.getIcon().getId();
			if (id > 0 && id != DUMMY_ACTION_ICON) {
				log.info("Action id {} - icon {}", ai.actionid(), id);
				iconIds.add(id);
			}
			return ai;
		}, List.of("actions", "Action.oos.gz"));

		maker.writeList(Map.class, entry -> {
			String subName = entry.getPlaceNameSub().getName();
			return new XivMap(entry.getRowId(), entry.getOffsetX(), entry.getOffsetY(), entry.getSizeFactor(), blankToNull(entry.mapPath()), entry.getPlaceNameRegion().getName(), entry.getPlaceName().getName(), blankToNull(subName));
		}, List.of("maps", "Map.oos.gz"));

		// Assorted Icons
		// Damage types
		LongStream.rangeClosed(60011, 60013).forEach(iconIds::add);
		// Floor Markers
		LongStream.rangeClosed(61241, 61248).forEach(iconIds::add);
		// Head Markers
		LongStream.rangeClosed(61201, 61208).forEach(iconIds::add);
		LongStream.rangeClosed(61211, 61213).forEach(iconIds::add);
		LongStream.rangeClosed(61221, 61222).forEach(iconIds::add);


		log.info("Need to download {} icons", iconIds.size());
		// TODO: these icons are larger than they should be. Look into pngtastic to recompress them.
		Path iconDir = outputPathBase.resolve("icon");
		iconDir.toFile().mkdirs();
		iconIds.stream()
				.distinct()
				.parallel()
				.forEach(iconId -> {
					log.trace("Icon ID: {}", iconId);
					URI downloadPath = client.getAssetUri("ui/icon/%06d/%06d_hr1.tex".formatted((iconId / 1000) * 1000, iconId), ImageFormat.PNG);
					File out = iconDir.resolve("%06d_hr1.png".formatted(iconId)).toFile();
					log.trace("Downloading icon {} to {}", downloadPath, out);
					try (BufferedInputStream in = new BufferedInputStream(downloadPath.toURL().openStream());
					     FileOutputStream fileOutputStream = new FileOutputStream(out)) {

						byte[] dataBuffer = new byte[1024];
						int bytesRead;
						while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
							fileOutputStream.write(dataBuffer, 0, bytesRead);
						}
					}
					catch (FileNotFoundException e) {
						if (e.getMessage().contains("http")) {
							log.warn("Icon {} not found, ignoring ({})", iconId, e.toString());
						}
						else {
							throw new RuntimeException(e);
						}
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
	}

	private static @Nullable String blankToNull(String str) {
		if (StringUtils.isBlank(str)) {
			return null;
		}
		return str;
	}

	private <In extends XivApiObject, Out extends Serializable> void writeList(Class<In> xivApiClass, Function<In, @Nullable Out> mapper, List<String> path) {
		log.info("Loading {}...", xivApiClass.getSimpleName());
		XivApiPaginator<In> pager = client.getListIterator(xivApiClass);
		List<Out> out = pager.toBufferedStream(10).parallel().map(mapper).filter(Objects::nonNull).toList();
		log.info("Found {} entries of {}", out.size(), xivApiClass.getSimpleName());
		Path outputPath = outputPathBase;
		for (String pathPart : path) {
			outputPath = outputPath.resolve(pathPart);
		}
		outputPath.getParent().toFile().mkdirs();
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
