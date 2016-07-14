package org.knime.knip.io.nodes.imgreader2;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.URIUtil;

public class URLUtil {

	private static final String[] VALID_PROTOCOLS = new String[] { "ftp", "http", "https", "knime", "file" };

	/**
	 * String to URI if valid & possible.
	 * 
	 * @param in
	 * @return valid URI
	 */
	public static URI encode(String in) {
		try {
			return new URL(in).toURI();
		} catch (MalformedURLException | URISyntaxException e1) {
			try {
				final URI fromString = URIUtil.fromString(in);
				if (fromString.getScheme() == null) {
					final String fragment = fromString.getFragment();
					if (fragment != null) {
						return new URI("file", fromString.getSchemeSpecificPart() + "#" + fragment, null);
					} else {
						return new URI("file", fromString.getSchemeSpecificPart(), fragment);
					}
				} else if (isValidScheme(fromString.getScheme())) {
					return fromString;
				} else {
					return URIUtil.fromString("file://" + in);
				}

			} catch (final URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static boolean isValidScheme(String scheme) {
		for (final String protocol : VALID_PROTOCOLS) {
			if (scheme.equals(protocol)) {
				return true;
			}
		}
		return false;
	}
}
