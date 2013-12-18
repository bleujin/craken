package net.ion.craken.aradon.let;

import java.io.IOException;
import java.util.Map.Entry;

import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import net.ion.craken.aradon.ParameterMap;
import net.ion.craken.aradon.bean.RepositoryEntry;
import net.ion.craken.aradon.bean.RhinoEntry;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.nradon.let.IServiceLet;
import net.ion.radon.core.TreeContext;
import net.ion.radon.core.annotation.AnContext;
import net.ion.radon.core.annotation.AnRequest;
import net.ion.radon.core.annotation.FormParam;
import net.ion.radon.core.annotation.PathParam;
import net.ion.radon.core.let.InnerRequest;
import net.ion.radon.core.let.MultiValueMap;
import net.ion.script.rhino.ResponseHandler;
import net.ion.script.rhino.RhinoEngine;
import net.ion.script.rhino.RhinoScript;

public class ScriptLet implements IServiceLet {

	@Get
	public String helloWorld(@AnRequest InnerRequest request){
		StringBuilder result = new StringBuilder() ;
		MultiValueMap map = request.getFormParameter();
		
		for (Entry<String, Object> entry : map.entrySet()) {
			result.append(entry.getKey() + ":" + entry.getValue() + "\n") ;
		}

		return result.toString() ;
	}
	
	@Post
	public Representation execute(@AnContext TreeContext context, @AnRequest InnerRequest request, @PathParam("name") String sname, @FormParam("script") String script) throws IOException{
		
		RepositoryEntry r = context.getAttributeObject(RepositoryEntry.EntryName, RepositoryEntry.class);
		ReadSession rsession = r.login("test");
		RhinoEntry rengine = context.getAttributeObject(RhinoEntry.EntryName, RhinoEntry.class);
		RhinoScript rscript = rengine.newScript(sname).defineScript(script);
		
		rscript.bind("session", rsession).bind("params", ParameterMap.create(request.getFormParameter())) ;
		String scriptResult = rscript.exec(ResponseHandler.StringMessage) ;
		
		return new StringRepresentation(scriptResult) ;
	}

	
}
