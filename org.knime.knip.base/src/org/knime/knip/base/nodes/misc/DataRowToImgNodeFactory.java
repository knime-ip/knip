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
package org.knime.knip.base.nodes.misc;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.exceptions.KNIPException;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.core.types.NativeTypes;
import org.knime.knip.core.util.EnumUtils;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Converts a Collection of {@link DoubleCell}s to {@link Img}
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @param <T>
 */
public class DataRowToImgNodeFactory<T extends RealType<T> & NativeType<T>> extends
        ValueToCellNodeFactory<CollectionDataValue> {

    private static final SettingsModelString createDimenionsModel() {
        return new SettingsModelString("dimensions", "100,100");
    }

    private static final SettingsModelString createDimensionLabelsModel() {
        return new SettingsModelString("dimension_labels", "X,Y");
    }

    private static SettingsModelString createPixelTypeModel() {
        return new SettingsModelString("pixel_type", NativeTypes.BYTETYPE.toString());
    }

    @Override
    public ValueToCellNodeModel<CollectionDataValue, ImgPlusCell<T>> createNodeModel() {
        return new ValueToCellNodeModel<CollectionDataValue, ImgPlusCell<T>>() {

            private SettingsModelString m_smDims = createDimenionsModel();

            private SettingsModelString m_smDimLabels = createDimensionLabelsModel();

            private SettingsModelString m_smPixType = createPixelTypeModel();

            private ImgPlusCellFactory m_cellFactory;

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_cellFactory = new ImgPlusCellFactory(exec);
            }

            @Override
            protected ImgPlusCell<T> compute(final CollectionDataValue cellValue) throws Exception {

                if (!cellValue.getElementType().isCompatible(DoubleValue.class)) {
                    throw new KNIPException(
                            "Collection of cells does not contain exclusively numbers! According row is skipped.");
                }

                //get dimensions and labels
                long[] dims = parseDimensions(m_smDims.getStringValue());
                String[] dimLabels = parseDimensionLabels(m_smDimLabels.getStringValue());

                //if number of dimension are one less than the number of labels
                //then determine the size of the last dimension
                if (dims.length == dimLabels.length - 1) {
                    long[] tmpA = new long[dims.length + 1];
                    double tmp = 1;
                    for (int i = 0; i < dims.length; i++) {
                        tmp *= dims[i];
                        tmpA[i] = dims[i];
                    }
                    tmpA[tmpA.length - 1] = (long)Math.ceil(cellValue.size() / tmp);
                    dims = tmpA;
                }

                //pixel type
                T type = (T)NativeTypes.valueOf(m_smPixType.getStringValue()).getTypeInstance();

                //create image
                Img<T> img = new ArrayImgFactory<T>().create(dims, type);

                if (img.size() != cellValue.size()) {
                    //if the number pixels of the result image and the number of provided double values don't fit
                    getLogger().warn("The number of provided numbers (" + cellValue.size()
                                             + ") doesn't comply with the number of pixels of the result image ("
                                             + img.size() + ")");
                    setWarningMessage("The collection of values doesn't fit into the result image for some rows. See console log for details.");
                }

                //transfer data to the image
                Cursor<T> cur = img.cursor();
                Iterator<DataCell> it = cellValue.iterator();
                while (cur.hasNext() && it.hasNext()) {
                    cur.fwd();
                    double val = ((DoubleValue)it.next()).getDoubleValue();
                    cur.get().setReal(val);
                }

                //set meta data
                AxisType[] axes = new AxisType[dimLabels.length];
                for (int i = 0; i < axes.length; i++) {
                    axes[i] = Axes.get(dimLabels[i]);
                }
                ImgPlus<T> imgPlus = new ImgPlus<T>(img, "", axes);

                return m_cellFactory.createCell(imgPlus);
            }

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_smDims);
                settingsModels.add(m_smDimLabels);
                settingsModels.add(m_smPixType);
            }

        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<CollectionDataValue> createNodeDialog() {
        return new ValueToCellNodeDialog<CollectionDataValue>() {

            private SettingsModelString m_smDims;

            private SettingsModelString m_smDimLabels;

            @Override
            public void addDialogComponents() {

                m_smDims = createDimenionsModel();
                m_smDimLabels = createDimensionLabelsModel();

                addDialogComponent("Options", "Image Dimensions", new DialogComponentString(m_smDims, "Dimensions"));
                addDialogComponent("Options", "Image Dimensions", new DialogComponentString(m_smDimLabels,
                        "Dimension labels"));
                addDialogComponent("Options", "Image Dimensions", new DialogComponentLabel(
                        "(Dimensions and labels are specified ','-separated)"));
                addDialogComponent("Options", "Image Pixel Type", new DialogComponentStringSelection(
                        createPixelTypeModel(), "", EnumUtils.getStringListFromToString(NativeTypes.values())));

            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
                super.saveAdditionalSettingsTo(settings);

                //check configuration
                long[] dims = parseDimensions(m_smDims.getStringValue());
                String[] dimLabels = parseDimensionLabels(m_smDimLabels.getStringValue());
                if (!(dims.length == dimLabels.length || dims.length == dimLabels.length - 1)) {
                    throw new InvalidSettingsException(
                            "Number of dimensions must be equal to the number of labels (or one less).");
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected String getDefaultSuffixForAppend() {
                return "_drti";
            }

        };
    }

    private long[] parseDimensions(final String s) {
        StringTokenizer st = new StringTokenizer(s, ",");
        long[] dims = new long[st.countTokens()];
        for (int i = 0; i < dims.length; i++) {
            dims[i] = Long.parseLong(st.nextToken().trim());
        }
        return dims;
    }

    private String[] parseDimensionLabels(final String s) {
        StringTokenizer st = new StringTokenizer(s, ",");
        String[] dimLabels = new String[st.countTokens()];
        for (int i = 0; i < dimLabels.length; i++) {
            dimLabels[i] = st.nextToken().trim();
        }
        return dimLabels;
    }
}
