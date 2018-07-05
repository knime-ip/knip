package org.knime.knip.io2.resolver;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.scijava.Priority;
import org.scijava.io.location.LocationResolver;
import org.scijava.plugin.AbstractHandlerService;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;

@Plugin(type = Service.class, priority = Priority.VERY_HIGH)
public class DefaultKNIMELocationService extends AbstractHandlerService<URI, LocationResolver>
		implements KNIMELocationService {

	private final Map<String, LocationResolver> resolvers = new HashMap<>();

	@Override
	public LocationResolver getResolver(URI uri) {
		return resolvers.computeIfAbsent(uri.getScheme(), u -> getHandler(uri));
	}

}
