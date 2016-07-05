/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.cellviewer.interfaces;

import java.util.List;

import org.knime.core.data.DataValue;

/**
 * Implementing classes need to provide a no-args constructor!
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:Andreas.Burger@uni-konstanz.de">Andreas Burger</a>
 */
public interface CellViewFactory extends Comparable<CellViewFactory> {

	/**
	 * This method creates the {@link CellView} which instantiates and handles
	 * the actual view.
	 * 
	 * @return the possible views for the given data value
	 */
	public CellView createCellView();

	/**
	 * @return the (unique) name of the provided view, for caching and display
	 */

	public String getCellViewName();

	/**
	 * @return a description if the provided view
	 */
	public String getCellViewDescription();

	/**
	 * This method is used to signal the render capabilities of the provided
	 * views. If, for a given list, this method returns <i>true</i>, the view
	 * has to be able to handle (i.e. display) the list. <br>
	 * A default implementation which checks for a list containing a single
	 * value in accordance with {@link CellViewFactory#getDataValueClass()} is
	 * provided.
	 * 
	 * @param values
	 *            The prototype list whose displayability is to be checked.
	 * @return true if the contents of the list can be rendered, false otherwise
	 */
	public boolean isCompatible(final List<Class<? extends DataValue>> values);

	/**
	 * Get an associated priority-value for the CellView. This is used to
	 * determine the ordering of eligible views in the cell viewer. Views with
	 * larger priority are shown before views with small priority.
	 * 
	 * @return An integer describing this views priority
	 */
	default public int getPriority() {
		return 0;
	}

	@Override
	default int compareTo(CellViewFactory o) {
		if(getPriority() > o.getPriority())
			return 1;
		if(getPriority() < o.getPriority())
			return -1;
		return 0;
		
	}
	
	

}
