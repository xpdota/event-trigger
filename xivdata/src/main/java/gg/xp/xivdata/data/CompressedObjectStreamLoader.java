package gg.xp.xivdata.data;

import gg.xp.xivdata.util.ArrayBackedMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public final class CompressedObjectStreamLoader {
	private CompressedObjectStreamLoader() {
	}

	public static <X> Map<Integer, X> loadFrom(InputStream inputStream, Function<X, Integer> keyExtractor) {
		List<X> datum;
		try (ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(inputStream))) {
			datum = (List<X>) ois.readObject();
		}
		catch (ClassNotFoundException | IOException e) {
			throw new RuntimeException(e);
		}
		Map<Integer, X> tmp = datum.stream()
				.collect(Collectors.toMap(keyExtractor, Function.identity(), (a, b) -> b));
		return new ArrayBackedMap<>(tmp);
	}

}
