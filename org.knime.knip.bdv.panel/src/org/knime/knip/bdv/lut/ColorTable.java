package org.knime.knip.bdv.lut;

/**
 * A lookup table.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public interface ColorTable {

	public int[] getLut();
	
	public void setLut(final int[] lut);
	
	public void newColors();
}
