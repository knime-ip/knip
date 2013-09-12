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
package org.knime.knip.base.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class NodeTools {

    /**
     * If setting holds a valid column name returns the column index. If not search the first compatible column and
     * return the index. A logger warning is given to indicate the automatic selection in contrast to
     * {@link NodeTools#silentOptionalAutoColumnSelection}.
     * 
     * @param inSpec
     * @param model
     * @param valueClass
     * @param nodeModelClass node model class to be able to set right logger message
     * @param except columns that should not be chosen e.g. because they are already in use
     * @return the column index, never -1, a {@link InvalidSettingsException} is thrown instead
     * @throws InvalidSettingsException
     */
    public final static int autoColumnSelection(final DataTableSpec inSpec, final SettingsModelString model,
                                                final Class<? extends DataValue> valueClass,
                                                final Class<? extends NodeModel> nodeModelClass,
                                                final Integer... except) throws InvalidSettingsException {

        int i = inSpec.findColumnIndex(model.getStringValue());
        if ((i > -1) && inSpec.getColumnSpec(i).getType().isCompatible(valueClass)) {
            return i;
        } else {
            i = autoOptionalColumnSelection(inSpec, model, valueClass, except);
            if (i > -1) {
                NodeLogger.getLogger(nodeModelClass).warn("No column specified as " + valueClass.getSimpleName()
                                                                  + ": auto detection suggested column "
                                                                  + inSpec.getColumnSpec(i).getName());
            } else {
                String errorMessage = "";
                StringBuffer sb = new StringBuffer();
                if (except.length > 0) {
                    if (except.length > 1) {
                        sb.append(" (columns: ");
                        for (int j = 0; j < except.length; j++) {
                            sb.append(except[j]);
                            sb.append(", ");
                        }
                        errorMessage = sb.toString().substring(0, sb.length() - 2);
                        errorMessage +=
                                " have already been chosen or have been excluded from automatic selection for some other reason)";
                    } else {
                        errorMessage +=
                                " (column: "
                                        + except[0]
                                        + " has already been chosen or has been excluded from automatic selection for some other reason)";
                    }
                }

                throw new InvalidSettingsException(model.getKey() + ": No column of type " + valueClass.getSimpleName()
                        + " available!" + errorMessage);
            }

            return i;
        }

    }

    /**
     * Selects the first compatible column from the table spec. If a compatible column is found sets the model to this
     * column and returns the column index.
     * 
     * @param inSpec
     * @param model
     * @param value
     * @param except columns that should not be chosen e.g. because they are already in use
     * @return the column index if found, else -1
     */
    public final static int
            autoOptionalColumnSelection(final DataTableSpec inSpec, final SettingsModelString model,
                                        final Class<? extends DataValue> value, final Integer... except) {

        final int i = firstCompatibleColumn(inSpec, value, except);
        if (i > -1) {
            model.setStringValue(inSpec.getColumnSpec(i).getName());
            return i;
        }
        return -1;
    }

    /**
     * @param buf
     * @return a ListCell created from the given double array
     */
    public static final ListCell createListCell(final double[] buf) {
        final List<DoubleCell> res = new ArrayList<DoubleCell>(buf.length);
        for (int i = 0; i < buf.length; i++) {
            res.add(new DoubleCell(buf[i]));
        }
        return CollectionCellFactory.createListCell(res);
    }

    /**
     * 
     * @param buf
     * @return a ListCell created from the given int array
     */
    public static final ListCell createListCell(final int[] buf) {
        final List<IntCell> res = new ArrayList<IntCell>(buf.length);
        for (int i = 0; i < buf.length; i++) {
            res.add(new IntCell(buf[i]));
        }
        return CollectionCellFactory.createListCell(res);

    }

    /**
     * Searches the first compatible column from the table specs
     * 
     * @param inSpec
     * @param valueClass
     * @param except columns that should not be chosen e.g. because they are already in use
     * @return the index of the first compatible column or -1 if no column in the table specs is compatible
     */
    public final static int firstCompatibleColumn(final DataTableSpec inSpec,
                                                  final Class<? extends DataValue> valueClass, final Integer... except) {

        @SuppressWarnings("unchecked")
        final List<Integer> exceptList = (except == null ? new LinkedList<Integer>() : Arrays.asList(except));

        int i = 0;
        for (final DataColumnSpec colspec : inSpec) {
            if (colspec.getType().isCompatible(valueClass) && !exceptList.contains(i)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /**
     * If setting holds a valid column name returns the column index. If not search the first compatible column and
     * return the index.
     * 
     * @param inSpec
     * @param model
     * @param valueClass
     * @param except columns that should not be chosen e.g. because they are already in use
     * @return The column index, maybe -1, if not found
     */
    public final static int silentOptionalAutoColumnSelection(final DataTableSpec inSpec,
                                                              final SettingsModelString model,
                                                              final Class<? extends DataValue> valueClass,
                                                              final Integer... except) {

        int i = inSpec.findColumnIndex(model.getStringValue());
        if ((i > -1) && inSpec.getColumnSpec(i).getType().isCompatible(valueClass)) {
            return i;
        }
        i = autoOptionalColumnSelection(inSpec, model, valueClass, except);

        return i;

    }

    private NodeTools() {
        // utility class
    }
}
