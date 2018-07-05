package org.knime.knip.io2.resolver;

import java.net.URI;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.scijava.io.location.Location;
import org.scijava.io.location.LocationResolver;
import org.scijava.io.location.LocationService;

/**
 * Adapted from Scijava-common location service
 *
 * @author gabriel
 */
public interface KNIMELocationService extends LocationService {

	/**
	 * Resolve using the provided connection info
	 *
	 * @param uri
	 * @param conenctionInfo
	 */
	public default Location resolveWithAuth(final URI uri, final ConnectionInformation conenctionInfo) {
		final LocationResolver res = getResolver(uri);
		if (res instanceof AuthAwareResolver) {
			final AuthAwareResolver authResolver = (AuthAwareResolver) res;
			return authResolver.resolveWithAuth(uri, conenctionInfo);
		}
		return null;
	}
}
