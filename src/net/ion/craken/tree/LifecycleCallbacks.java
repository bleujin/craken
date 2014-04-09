
package net.ion.craken.tree;

import java.util.Map;

import net.ion.craken.node.crud.TreeNodeKey;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.lifecycle.AbstractModuleLifecycle;
import org.infinispan.marshall.AdvancedExternalizer;

public class LifecycleCallbacks extends AbstractModuleLifecycle {

	@Override
	public void cacheManagerStarting(GlobalComponentRegistry gcr, GlobalConfiguration globalCfg) {
		Map<Integer, AdvancedExternalizer<?>> externalizerMap = globalCfg.serialization().advancedExternalizers();
		externalizerMap.put(1000, new TreeNodeKey.Externalizer());
		externalizerMap.put(1001, new Fqn.Externalizer());
	}

}
