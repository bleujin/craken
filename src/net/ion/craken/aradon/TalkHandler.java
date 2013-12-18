package net.ion.craken.aradon;

import net.ion.craken.node.ReadSession;

public interface TalkHandler {

	void onConnected(TalkEngine tengine, UserConnection uconn);

	void onClose(TalkEngine tengine, UserConnection uconn);

	void onMessage(TalkEngine tengine, UserConnection uconn, ReadSession rsession, TalkMessage tmsg);

	void onEngineStart(TalkEngine tengine);

	void onEngineStop(TalkEngine tengine);

}
