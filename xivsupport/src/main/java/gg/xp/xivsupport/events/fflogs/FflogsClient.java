package gg.xp.xivsupport.events.fflogs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FflogsClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(FflogsClient.class);

	private static final String apiBaseUrlV2 = "https://www.fflogs.com/api/v2/client";
	private static final String apiBaseUrlV1 = "https://www.fflogs.com/v1";
	private static final String authUrl = "https://www.fflogs.com/oauth/token";
	private final String authLine;

	// TODO: find a way to make cache weakref-based
	private final ConcurrentCacheMap<String, JsonNode> v2cache = new ConcurrentCacheMap<>(d -> queryV2internal(d, Collections.emptyMap()));
	public static final ObjectMapper mapper = new ObjectMapper();
	private HttpClient hc;
	private String accessToken;

	private static final Object instanceLock = new Object();

	public FflogsClient(FflogsController controller) {
		String clid = controller.clientId().get();
		if (clid.isBlank()) {
			throw new IllegalArgumentException("Client ID must not be blank");
		}
		String secret = controller.clientSecret().get();
		if (clid.isEmpty()) {
			throw new IllegalArgumentException("Secret must not be blank");
		}
		authLine = "Basic " + Base64.encodeBase64String((clid + ":" + secret).getBytes());
	}

	public void clearCaches() {
		v2cache.clear();
	}

	public JsonNode queryV2(String queryBody) {
		return queryV2(queryBody, false);
	}

	public JsonNode queryV2(String queryBody, boolean bypassCache) {
		if (bypassCache) {
			return queryV2internal(queryBody, Collections.emptyMap());
		} else {
			return v2cache.get(queryBody);
		}
	}

	public JsonNode queryV2internal(String queryBody, Map<String, Object> vars) {
		Map<String, Object> queryMap = new HashMap<>();

		queryMap.put("query", queryBody);

		if (!vars.isEmpty()) {
			queryMap.put("variables", vars);
		}


		String actualBody;
		try {
			actualBody = mapper.writeValueAsString(queryMap);
		} catch (JsonProcessingException e) {
			throw new ApiException(e);
		}
		HttpPost req = new HttpPost(apiBaseUrlV2);
		req.addHeader("Authorization", "Bearer " + accessToken);
		req.addHeader("Accept", "application/json");
		req.addHeader("Content-Type", "application/json");

		HttpResponse response;
		try {
			req.setEntity(new StringEntity(actualBody));
			long before = System.currentTimeMillis();
			response = hc.execute(req);
			long after = System.currentTimeMillis();
			LOGGER.info("Query took {}ms", after - before);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new ApiException("Bad response: " + response.getStatusLine());
			}
			JsonNode rootNode = mapper.readTree(response.getEntity().getContent());
			if (rootNode.has("errors")) {
				throw new ApiException("There were errors in the returned GraphQL response:\n" + rootNode.get("errors").toPrettyString());
			}
			return rootNode.at("/data");
		} catch (IOException e) {
			throw new ApiException(e);
		}

	}

	public void init() {
		SSLContext sslContext;
		try {
			sslContext = SSLContextBuilder
					.create()
					.loadTrustMaterial(new TrustSelfSignedStrategy())
					.build();

			// we can optionally disable hostname verification.
			// if you don't want to further weaken the security, you don't have to include this.
			HostnameVerifier allowAllHosts = new NoopHostnameVerifier();

			// create an SSL Socket Factory to use the SSLContext with the trust self signed certificate strategy
			// and allow all hosts verifier.
			SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);

			hc = HttpClientBuilder.create()
//					.setProxy(HttpHost.create("http://localhost:8888"))
//					.setSSLSocketFactory(connectionFactory)
					.build();
			RequestConfig rc = RequestConfig.custom().setAuthenticationEnabled(true).build();
			HttpPost tokenRequest = new HttpPost(authUrl);
			tokenRequest.addHeader("Authorization", authLine);
			tokenRequest.setConfig(rc);
			HttpEntity authEntity = MultipartEntityBuilder.create()
					.addTextBody("grant_type", "client_credentials")
					.build();
			tokenRequest.setEntity(authEntity);

			HttpResponse authResponse = hc.execute(tokenRequest);

			StatusLine statusLine = authResponse.getStatusLine();
			if (statusLine.getStatusCode() != 200) {
				throw new ApiException("Got bad response: " + statusLine);
			}
			InputStream contentStream = authResponse.getEntity().getContent();

			String content = inputStreamToString(contentStream);
			JsonNode tree = mapper.readTree(content);
			Map<String, String> responseData = mapper.convertValue(tree, new TypeReference<>() {
			});
			accessToken = responseData.get("access_token");

		} catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException | IOException e) {
			throw new ApiException(e);
		}

	}

	private static String inputStreamToString(InputStream inputStream) {
		String text;
		try {
			text = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return text;
	}


}
