package gg.xp.postnamazu;

import java.net.http.HttpResponse;

public record PnHttpResponse(HttpResponse<String> response) implements PnResponse {
	@Override
	public boolean isSuccess() {
		return response.statusCode() == 200;
	}
}
