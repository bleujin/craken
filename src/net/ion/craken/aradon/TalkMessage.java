package net.ion.craken.aradon;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.ObjectId;

public class TalkMessage {

	private JsonObject json;
	private static TalkMessage PLAIN_MSG = new TalkMessage(JsonObject.fromString("{script:\"print('not supported plain message')\", id='0',params:{}}")) ;
	
	private TalkMessage(JsonObject json) {
		this.json = json;
	}

	public static TalkMessage fromJsonString(String json) {
		try {
			return new TalkMessage(JsonObject.fromString(json));
		} catch (IllegalStateException notjson) {
			return PLAIN_MSG ;
		}
	}

	public static TalkMessage fromStript(String script) {
		return new TalkMessage(new JsonObject().put("script", script).put("id", new ObjectId().toString()).put("params", new JsonObject())) ;
	}

	public String script() {
		return json.asString("script");
	}

	public String id() {
		return json.asString("id");
	}

	public JsonObject params() {
		return json.asJsonObject("params");
	}

	public String toPlainMessage() {
		return json.toString() ;
	}


}
