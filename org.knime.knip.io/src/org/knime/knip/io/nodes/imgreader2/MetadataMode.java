package org.knime.knip.io.nodes.imgreader2;
/**
 * Enum that stores the metadata modes.
 */
public enum MetadataMode {
	NO_METADATA("No metadata"), APPEND_METADATA("Append a metadata column"), METADATA_ONLY(
			"Only metadata");

	private final String name;

	MetadataMode(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}