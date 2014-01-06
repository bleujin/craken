package net.ion.craken.aradon.bean;

import java.io.IOException;

import net.ion.radon.core.IService;
import net.ion.radon.core.context.OnEventObject;
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
	public void onEvent(AradonEvent event, IService service) {
		if (event == AradonEvent.START) {
			rengine.start();
		} else if (event == AradonEvent.STOP) {
			rengine.shutdown();
		}
	}

	public RhinoScript newScript(String sname) {
		return rengine.newScript(sname);
	}

}
