package org.knime.knip.io2.nodes.imgreader3;

/**
 * Enum that stores the metadata modes.
 * 
 * @author <a href="mailto:danielseebacher@t-online.de">Daniel Seebacher,
 *         University of Konstanz.</a>
 */
public enum MetadataMode {
	NO_METADATA("No metadata"), APPEND_METADATA("Append a metadata column"), METADATA_ONLY("Only metadata");

	private final String name;

	MetadataMode(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}