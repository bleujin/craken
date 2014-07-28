package net.ion.craken.aradon.bean;

import java.io.IOException;

import net.ion.nradon.Radon;
import net.ion.nradon.config.OnEventObject;
import net.ion.nradon.handler.event.ServerEvent.EventType;
import net.ion.script.rhino.Scripter;

public class RhinoEntry implements OnEventObject {

	private Scripter rhiner;
	public final static String EntryName = "rengine";

	private RhinoEntry(Scripter rengine) {
		this.rhiner = rengine;
	}

	public final static RhinoEntry test() throws IOException {
		return new RhinoEntry(Scripter.create());
	}

	@Override
	public void onEvent(EventType event, Radon service) {
		if (event == EventType.START) {
			rhiner.start();
		} else if (event == EventType.STOP) {
			rhiner.shutdown();
		}
	}

}
