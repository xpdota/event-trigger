package gg.xp.telestosupport;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;
import java.net.http.HttpResponse;

public class TelestoHttpError extends BaseEvent implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = 5675470435257672749L;
	private final HttpResponse<String> response;

	public TelestoHttpError(HttpResponse<String> response) {
		this.response = response;
	}

	public HttpResponse<String> getResponse() {
		return response;
	}

	@Override
	public String getPrimaryValue() {
		return response.statusCode() + ": " + response.body();
	}

}
