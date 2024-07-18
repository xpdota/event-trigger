package gg.xp.postnamazu;

public record PnOpResponse() implements PnResponse {
	@Override
	public boolean isSuccess() {
		return true;
	}
}
