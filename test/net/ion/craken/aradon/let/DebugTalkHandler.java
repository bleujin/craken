package net.ion.craken.aradon.let;

import net.ion.craken.aradon.TalkEngine;
import net.ion.craken.aradon.TalkHandler;
import net.ion.craken.aradon.TalkMessage;
import net.ion.craken.aradon.UserConnection;
import net.ion.craken.node.ReadSession;
import net.ion.framework.util.Debug;

public class DebugTalkHandler implements TalkHandler{

	@Override
	public void onClose(TalkEngine tengine, UserConnection uconn) {
		Debug.line(uconn.id() + " closed") ;
	}

	@Override
	public void onConnected(TalkEngine tengine, UserConnection uconn) {
		Debug.line(uconn.id() + " connected") ;
	}

	@Override
	public void onEngineStart(TalkEngine tengine) {
		Debug.line("engine started") ;
	}

	@Override
	public void onEngineStop(TalkEngine tengine) {
		Debug.line("engine stopped") ;
	}

	@Override
	public void onMessage(TalkEngine tengine, UserConnection uconn, ReadSession rsession, TalkMessage tmsg) {
		Debug.line(tmsg + " received") ;
	}

}
