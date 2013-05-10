
package net.ion.craken.tree;

import java.util.Map;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.lifecycle.AbstractModuleLifecycle;
import org.infinispan.marshall.AdvancedExternalizer;

/**
 * Module lifecycle callbacks implementation that enables module specific {@link org.infinispan.marshall.AdvancedExternalizer} implementations to be registered.
 * 
 * Information about the valid id range can be found <a href="http://community.jboss.org/docs/DOC-16198">here</a>
 * 
 * @author Galder Zamarreño
 * @since 5.0
 */
public class LifecycleCallbacks extends AbstractModuleLifecycle {

	@Override
	public void cacheManagerStarting(GlobalComponentRegistry gcr, GlobalConfiguration globalCfg) {
		Map<Integer, AdvancedExternalizer<?>> externalizerMap = globalCfg.serialization().advancedExternalizers();
		externalizerMap.put(1000, new TreeNodeKey.Externalizer());
		externalizerMap.put(1001, new Fqn.Externalizer());
	}

}