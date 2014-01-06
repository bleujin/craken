package org.infinispan.query;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.module.ExtendedModuleCommandFactory;
import org.infinispan.commands.remote.CacheRpcCommand;

/**
 * Remote commands factory implementation
 * 
 * @author Israel Lacerra <israeldl@gmail.com>
 * @since 5.1
 */
public class CommandFactory implements ExtendedModuleCommandFactory {

	@Override
	public Map<Byte, Class<? extends ReplicableCommand>> getModuleCommands() {
		Map<Byte, Class<? extends ReplicableCommand>> map = new HashMap<Byte, Class<? extends ReplicableCommand>>(1);
		return map;
	}

	@Override
	public ReplicableCommand fromStream(byte commandId, Object[] args) {
		// Should not be called while this factory only
		// provides cache specific replicable commands.
		return null;
	}

	@Override
	public CacheRpcCommand fromStream(byte byte0, Object[] aobj, String s) {
		// TODO Auto-generated method stub
		return null;
	}


}
