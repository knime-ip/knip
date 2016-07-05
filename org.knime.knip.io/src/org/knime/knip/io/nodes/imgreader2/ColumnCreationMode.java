package org.knime.knip.io.nodes.imgreader2;

public enum ColumnCreationMode {
	NEW_TABLE("New Table"), APPEND("Append"), REPLACE("Replace");

	private static String[] modes = { NEW_TABLE.toString(), APPEND.toString(), REPLACE.toString() };
	private String name;

	ColumnCreationMode(final String name) {
		this.name = name;
	}

	/**
	 * @param name
	 *            the name of the column creation mode
	 * @return the ColumnCreationMode corresponding to that name
	 * @throws IllegalArgumentException
	 *             when the name is not associated with any ColumnCreationMode.
	 */
	public static ColumnCreationMode fromString(final String name) {
		if (NEW_TABLE.toString().equals(name)) {
			return NEW_TABLE;
		}
		if (APPEND.toString().equals(name)) {
			return APPEND;
		}
		if (REPLACE.toString().equals(name)) {
			return REPLACE;
		}
		throw new IllegalArgumentException(
				"ColumnCreationMode enum does not contain a value with name \"" + name + "\"");
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * @return an array containing the names of all available modes.
	 */
	public static String[] getModes() {
		return modes;
	}

}
