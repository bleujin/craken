package net.ion.craken.loaders;

import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;

public class GridLoaderConfigurationBuilder  extends AbstractStoreConfigurationBuilder<GridLoaderConfiguration, GridLoaderConfigurationBuilder> {

	private String location = "";
	private int maxEntries = -1;

	public GridLoaderConfigurationBuilder(PersistenceConfigurationBuilder builder) {
		super(builder);
	}
	
	public GridLoaderConfigurationBuilder location(String location) {
		this.location = location;
		return this;
	}

	public GridLoaderConfigurationBuilder maxEntries(int maxEntries) {
		this.maxEntries = maxEntries;
		return this;
	}

	@Override
	public GridLoaderConfiguration create() {
		return new GridLoaderConfiguration(purgeOnStartup, fetchPersistentState, ignoreModifications, async.create(), singletonStore.create(), preload, shared, properties, location, maxEntries);
	}

	@Override
	public GridLoaderConfigurationBuilder self() {
		return this;
	}

}
