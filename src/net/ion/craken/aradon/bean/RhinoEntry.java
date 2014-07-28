package net.ion.craken.aradon.bean;

import java.io.IOException;

import net.ion.nradon.Radon;
import net.ion.nradon.config.OnEventObject;
import net.ion.nradon.handler.event.ServerEvent.EventType;
import net.ion.script.rhino.RhinoEngine;
import net.ion.script.rhino.RhinoScript;

public class RhinoEntry implements OnEventObject {

	private RhinoEngine rengine;
	public final static String EntryName = "rengine";

	private RhinoEntry(RhinoEngine rengine) {
		this.rengine = rengine;
	}

	public final static RhinoEntry test() throws IOException {
		return new RhinoEntry(RhinoEngine.create());
	}

	@Override
	public void onEvent(EventType event, Radon service) {
		if (event == EventType.START) {
			rengine.start();
		} else if (event == EventType.STOP) {
			rengine.shutdown();
		}
	}

	public RhinoScript newScript(String sname) {
		return rengine.newScript(sname);
	}

}
