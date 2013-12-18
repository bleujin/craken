package net.ion.craken.aradon.let;

import net.ion.craken.aradon.ParameterMap;
import net.ion.craken.aradon.TalkEngine;
import net.ion.craken.aradon.TalkHandler;
import net.ion.craken.aradon.TalkMessage;
import net.ion.craken.aradon.UserConnection;
import net.ion.craken.aradon.bean.RhinoEntry;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.script.rhino.ResponseHandler;
import net.ion.script.rhino.RhinoEngine;
import net.ion.script.rhino.RhinoScript;

public class ScriptTalkHandler implements TalkHandler {

	@Override
	public void onClose(TalkEngine tengine, UserConnection uconn) {
		
	}

	@Override
	public void onConnected(TalkEngine tengine, UserConnection uconn) {
		
	}

	@Override
	public void onEngineStart(TalkEngine tengine) {
		
	}

	@Override
	public void onEngineStop(TalkEngine tengine) {
		
	}

	@Override
	public void onMessage(TalkEngine tengine, UserConnection sender, ReadSession rsession, TalkMessage tmessage) {
		
		RhinoEntry rengine = tengine.context().getAttributeObject(RhinoEntry.EntryName, RhinoEntry.class);
		RhinoScript rscript = rengine.newScript(tmessage.id()).defineScript(tmessage.script());
		
		rscript.bind("session", rsession).bind("params", ParameterMap.create(tmessage.params())) ;
		String scriptResult = rscript.exec(ResponseHandler.StringMessage) ;
		sender.sendMessage(scriptResult) ;
	}

}
