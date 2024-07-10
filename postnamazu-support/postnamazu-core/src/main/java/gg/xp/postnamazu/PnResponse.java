package gg.xp.postnamazu;

public sealed interface PnResponse permits PnHttpResponse, PnOpResponse {
	boolean isSuccess();
}
