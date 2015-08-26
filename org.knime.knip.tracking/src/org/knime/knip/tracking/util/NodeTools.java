/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   4 Jun 2010 (hornm): created
 */
package org.knime.knip.tracking.util;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public final class NodeTools {

    private NodeTools() {
        // utility class
    }

    /**
     * Collects all columns of a given Type and returns a list of their names.
     *
     * @param type
     *            A Subclass of {@link DataValue}
     * @param spec
     *            the datatable spec that is searched through
     * @return A list of column names that match the type
     */
    public static final List<String> collectAllColumnNamesOfType(
            final Class<? extends DataValue> type, final DataTableSpec spec) {
        final List<String> colNames = new ArrayList<>();
        for (final DataColumnSpec c : spec) {
            if (c.getType().isCompatible(type)) {
                colNames.add(c.getName());
            }
        }
        return colNames;
    }

    /**
     * Collects all columns of a given Type and returns a list of their indices.
     *
     * @param type
     *            A Subclass of {@link DataValue}
     * @param spec
     *            the Datatable spec that is searched through
     * @return A list of column indices that match the type
     */
    public static final List<Integer> collectAllColumnIndicesOfType(
            final Class<? extends DataValue> type, final DataTableSpec spec) {
        final List<Integer> colIndices = new ArrayList<>();
        for (final DataColumnSpec c : spec) {
            if (c.getType().isCompatible(type)) {
                colIndices.add(spec.findColumnIndex(c.getName()));
            }
        }
        return colIndices;
    }

    /**
     * Collects all indices for the given list of column names.
     *
     * @param names
     *            the list of column names to be converted
     * @param spec
     *            the DataTableSpec of the table
     * @return list of indices
     */
    public static final List<Integer> columnNamesToIndices(
            final List<String> names, final DataTableSpec spec) {
        final List<Integer> incices = new ArrayList<>();
        for (final String name : names) {
            incices.add(spec.findColumnIndex(name));
        }
        return incices;
    }

    /**
     * Collects all indices for the given array of column names.
     *
     * @param names
     *            the array of column names to be converted
     * @param spec
     *            the DataTableSpec of the table
     * @return list of indices
     */
    public static final List<Integer> columnNamesToIndices(
            final String[] names, final DataTableSpec spec) {
        final List<Integer> incices = new ArrayList<>();
        for (final String name : names) {
            incices.add(spec.findColumnIndex(name));
        }
        return incices;
    }

    /**
     * Transforms a row into a double array. Use the exclude array to specify
     * columns that are included.
     *
     * @param row
     *            The data row
     * @param include
     *            The indices of columns that are included in the array
     * @return array containing the double values from the included columns
     * @throws ClassCastException when there are missing values
     */
    public static double[] toDoubleArray(final DataRow row,
            final List<Integer> include) {
        final double[] res = new double[include.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = ((DoubleValue) row.getCell(include.get(i)))
                    .getDoubleValue();
        }
        return res;
    }

    /**
     * Transforms a row into a double array using all compatible columns.
     * Convenience overload, if all columns are needed.
     *
     * @param row
     *            The data row
     * @param spec
     *            The spec of the table the row is part of.
     * @return array containing the double values from the included columns
     */
    public static double[] toDoubleArray(final DataRow row,
            final DataTableSpec spec) {
        final List<Integer> include = collectAllColumnIndicesOfType(
                DoubleValue.class, spec);
        return toDoubleArray(row, include);
    }

    /**
     * Provides a safe method to get the indices of the selected columns from a
     * Filter Model, falls back on selecting all compatible types in case the
     * selection is invalid.
     *
     * @param spec
     *            the spec of the table.
     * @param filterModel
     *            the filter model that specifies the selection
     * @param valueClass
     *            the value class selected in case the selection is invalid
     * @param nodeModelClass
     *            node model class to be able to set right logger message
     *
     * @return the indices of the selected columns
     */
    public static List<Integer> getIndicesFromFilter(final DataTableSpec spec,
            final SettingsModelColumnFilter2 filterModel,
            final Class<? extends DataValue> valueClass,
            final Class<? extends NodeModel> nodeModelClass) {

        final FilterResult result = filterModel.applyTo(spec);

        // in case of an invalid selection, select all
        if (result.getIncludes().length == 0
                && result.getExcludes().length == 0) {
            NodeLogger.getLogger(nodeModelClass).warn(
                    "Invalid column selection, automatically selecting "
                            + "all columns of the following type: "
                            + valueClass.getSimpleName());
            return collectAllColumnIndicesOfType(valueClass, spec);
        }
        return columnNamesToIndices(result.getIncludes(), spec);
    }

    /**
     * Creates an List filled by the values from the given row
     *
     * @param row
     * @return the row as list.
     */
    public static List<DataCell> makeListFromRow(final DataRow row) {
        final List<DataCell> list = new ArrayList<DataCell>(row.getNumCells());
        for (final DataCell cell : row) {
            list.add(cell);
        }
        return list;
    }
}
