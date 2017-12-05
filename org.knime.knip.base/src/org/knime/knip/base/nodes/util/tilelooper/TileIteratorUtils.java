/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2017
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
 * ---------------------------------------------------------------------
 *
 * Created on 27 Sep 2017 by Benjamin Wilhelm
 */
package org.knime.knip.base.nodes.util.tilelooper;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.base.nodes.util.tilelooper.config.SettingsModelOptionalNumber;

import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.TypedAxis;
import net.imagej.space.AnnotatedSpace;

/**
 * Utilities for the tile loop.
 *
 * @author Benjamin Wilhelm, MPI-CBG, Dresden
 */
public class TileIteratorUtils {
    static final String ROW_KEY_DELIMITER = "#";

    private TileIteratorUtils() {
        // NB: Utility class
    }

    /**
     * Writes the values of integer fields in the settings into an array using the order of dimensions of the image.
     *
     * @param imgAxis The metadata of the image.
     * @param models The {@link SettingsModelOptionalNumber} which stores the settings.
     * @return An array containing the values of the settings.
     */
    static long[] modelToArray(final AnnotatedSpace<CalibratedAxis> imgAxis,
                               final SettingsModelOptionalNumber[] models) {
        TypedAxis[] axis = KNIMEKNIPPlugin.parseDimensionLabelsAsAxis();
        long[] array = new long[imgAxis.numDimensions()];
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < axis.length; j++) {
                // TODO think about a more elegant solution
                if (axis[j].type().getLabel().equals(imgAxis.axis(i).type().getLabel())) {
                    array[i] = models[j].hasNumber() ? models[j].getIntValue() : -1;
                    break;
                }
            }
        }
        return array;
    }

    /**
     * Finds the index of the chosen column.
     *
     * @param inSpec The data table spec of the table.
     * @param columnSelection Settings model for the column selection.
     * @return The index.
     * @throws InvalidSettingsException If no column is selected.
     */
    static int getSelectedColumnIndex(final DataTableSpec inSpec, final SettingsModelString columnSelection,
                                      final Class<? extends NodeModel> nodeModelClass)
            throws InvalidSettingsException {
        int columnIndex = inSpec.findColumnIndex(columnSelection.getStringValue());
        if (columnIndex == -1) {
            try {
                return NodeUtils.autoColumnSelection(inSpec, columnSelection, ImgPlusValue.class, nodeModelClass, -1);
            } catch (InvalidSettingsException e) {
                return NodeUtils.autoColumnSelection(inSpec, columnSelection, LabelingValue.class, nodeModelClass, -1);
            }
        }
        return columnIndex;
    }

    /**
     * Creates the output table spec of the node.
     *
     * @param inSpec The input table spec.
     * @param columnSelection Settings model for the column selection.
     * @return The output table spec.
     * @throws InvalidSettingsException If no column is selected.
     */
    static DataTableSpec createOutSpecs(final DataTableSpec inSpec, final SettingsModelString columnSelection,
                                        final Class<? extends NodeModel> nodeModelClass)
            throws InvalidSettingsException {
        int i = getSelectedColumnIndex(inSpec, columnSelection, nodeModelClass);
        DataColumnSpec columnSpec = inSpec.getColumnSpec(i);
        return new DataTableSpec(columnSpec);
    }
}
