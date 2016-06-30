package org.knime.knip.io.nodes.imgreader2;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.URIUtil;

public class URLUtil {
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
				return URIUtil.fromString(in);
			} catch (final URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
