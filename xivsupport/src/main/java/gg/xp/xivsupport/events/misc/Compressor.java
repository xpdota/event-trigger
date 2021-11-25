package gg.xp.xivsupport.events.misc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class Compressor {

	private Compressor() {
	}

	// TODO: on paper, lzma and xz can compress better than gzip, at the cost of more CPU time.
	// Do some more research into what the best compression algorithm would be.
	public static byte[] compressStringToBytes(String inStr) {
		byte[] inBytes = inStr.getBytes(StandardCharsets.UTF_8);
		byte[] outBytes;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (OutputStream gzip = new GZIPOutputStream(baos)) {
			gzip.write(inBytes);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		outBytes = baos.toByteArray();
		return outBytes;
	}

	public static String uncompressBytesToString(byte[] compressed) {
		ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
		try (InputStream gzip = new GZIPInputStream(bais)) {
			return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
