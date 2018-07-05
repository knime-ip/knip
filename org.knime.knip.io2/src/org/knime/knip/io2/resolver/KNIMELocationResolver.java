package org.knime.knip.io2.resolver;

import java.net.URI;
import java.net.URISyntaxException;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.scijava.io.location.AbstractLocationResolver;
import org.scijava.io.location.Location;
import org.scijava.io.location.LocationResolver;
import org.scijava.plugin.Plugin;

/**
 * {@link LocationResolver} for the <code>knime://</code> uri scheme
 *
 */
@Plugin(type = LocationResolver.class)
public class KNIMELocationResolver extends AbstractLocationResolver implements AuthAwareResolver {

	public KNIMELocationResolver() {
		super("knime");
	}

	@Override
	public Location resolve(final URI uri) throws URISyntaxException {
		final String host = uri.getHost();
		switch (host) {
		case "knime.workflow":
			return translateWorkflowLocation();
		case "knime.mountpoint":
			return translateMountpointLocation();
		case "LOCAL":
			return translateWorkSpaceLocation();
		default:
			throw new UnsupportedOperationException("NYI");
		}
	}

	private Location translateWorkSpaceLocation() {
		// TODO
		throw new UnsupportedOperationException("NYI");
	}

	private Location translateMountpointLocation() {
		// TODO
		throw new UnsupportedOperationException("NYI");
	}

	private Location translateWorkflowLocation() {
		// TODO
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public Location resolveWithAuth(URI uri, ConnectionInformation conenctionInfo) {
		// TODO Implement with the server connector node
		throw new UnsupportedOperationException("NYI");
	}

}
