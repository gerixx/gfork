package org.gfork.internal.remote;

public class ReplyData {

	private boolean isExpectedToken;
	private boolean isTimedOut;
	private String replyToken;

	public ReplyData(boolean isExpectedToken, boolean isTimedOut, String replyToken) {
		this.setExpectedToken(isExpectedToken);
		this.setTimedOut(isTimedOut);
		this.setReplyToken(replyToken);
	}

	public boolean isExpectedToken() {
		return isExpectedToken;
	}

	public void setExpectedToken(boolean isExpectedToken) {
		this.isExpectedToken = isExpectedToken;
	}

	public boolean isTimedOut() {
		return isTimedOut;
	}

	public void setTimedOut(boolean isTimedOut) {
		this.isTimedOut = isTimedOut;
	}

	public String getReplyToken() {
		return replyToken;
	}

	public void setReplyToken(String replyToken) {
		this.replyToken = replyToken;
	}
}
