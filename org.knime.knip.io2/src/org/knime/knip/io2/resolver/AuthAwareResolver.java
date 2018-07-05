package org.knime.knip.io2.resolver;

import java.net.URI;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.scijava.io.location.Location;

public interface AuthAwareResolver {

	public Location resolveWithAuth(URI uri, ConnectionInformation conenctionInfo);

}
