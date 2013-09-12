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

import java.awt.Color;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.meta.Axes;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.DefaultCalibratedAxis;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.exceptions.KNIPException;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.core.awt.converter.RealGreyARGBConverter;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTable;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableUtils;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LabelingToRGBImgNodeFactory<T extends RealType<T>, L extends Comparable<L>> extends
        ValueToCellNodeFactory<LabelingValue<L>> {

    private static SettingsModelString createImgColumnSM() {
        return new SettingsModelString("useImgColumn", "");
    }

    private static SettingsModelIntegerBounded createTransparencySM() {
        return new SettingsModelIntegerBounded("transparency", 128, 0, 255);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<LabelingValue<L>, ImgPlusCell<UnsignedByteType>> createNodeModel() {
        return new ValueToCellNodeModel<LabelingValue<L>, ImgPlusCell<UnsignedByteType>>() {

            private ImgPlusCellFactory m_imgCellFactory;

            private SettingsModelString m_imgCol = createImgColumnSM();

            private SettingsModelIntegerBounded m_transparency = createTransparencySM();

            private int m_imgColNr;

            private ImgPlus<T> m_img;

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_imgCol);
                settingsModels.add(m_transparency);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

                String colName = m_imgCol.getStringValue();
                if (colName != null && !colName.isEmpty()) {
                    DataTableSpec spec = (DataTableSpec)inSpecs[0];
                    m_imgColNr = spec.findColumnIndex(colName);
                } else {
                    m_imgColNr = -1;
                }

                return super.configure(inSpecs);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void computeDataRow(final DataRow row) {
                if (m_imgColNr != -1) {
                    m_img = ((ImgPlusCell<T>)row.getCell(m_imgColNr)).getImgPlus();
                }

                super.computeDataRow(row);
            }

            @Override
            protected ImgPlusCell<UnsignedByteType> compute(final LabelingValue<L> cellValue) throws Exception {
                final Labeling<L> lab = cellValue.getLabeling();
                //TODO: Make MissingColorHandler selectable
                final LabelingColorTable colorMapping = new DefaultLabelingColorTable();

                if (m_imgColNr != -1) {
                    //render with image first check dimensionality
                    if (m_img.numDimensions() == lab.numDimensions()) {
                        for (int i = 0; i < m_img.numDimensions(); i++) {
                            if (m_img.dimension(i) != lab.dimension(i)) {
                                throw new KNIPException(
                                        "Incompatible dimension sizes: label dimension size != image dimension size for image axis "
                                                + m_img.axis(i).type().getLabel());
                            }
                        }
                    } else {
                        throw new KNIPException("Labeling and image are incompatible, different dimension count!");
                    }

                    if (m_img.firstElement().getClass() == DoubleType.class) {
                        throw new KNIPException(
                                "double type is currently not supported please convert the images first");
                    }
                }

                final Cursor<LabelingType<L>> labCur = lab.localizingCursor();

                //load the dims from labeling and append one 0
                final long[] tmp = new long[lab.numDimensions()];
                final long[] dims = new long[tmp.length + 1];
                lab.dimensions(tmp);
                System.arraycopy(tmp, 0, dims, 0, tmp.length);

                dims[dims.length - 1] = 3;

                final Img<UnsignedByteType> res =
                        new ArrayImgFactory<UnsignedByteType>().create(dims, new UnsignedByteType());
                final RandomAccess<UnsignedByteType> raOut = res.randomAccess();

                // new UnaryOperationAssignment(new
                // RealConstant(
                // Byte.MIN_VALUE)).compute(res,
                // res);

                List<L> labels;

                if (m_imgColNr != -1) {
                    //render with img overlay
                    RandomAccess<T> raIn = m_img.randomAccess();
                    //just normalize using the converter that is also used to render images
                    RealGreyARGBConverter<T> imgConverter = new RealGreyARGBConverter<T>(1, 0);
                    ARGBType converterOut = new ARGBType();

                    while (labCur.hasNext()) {
                        labCur.fwd();
                        for (int d = 0; d < lab.numDimensions(); d++) {
                            raOut.setPosition(labCur.getLongPosition(d), d);
                            raIn.setPosition(labCur.getLongPosition(d), d);
                        }
                        labels = labCur.get().getLabeling();
                        imgConverter.convert(raIn.get(), converterOut);
                        int normedImgValue = 0x000000FF & converterOut.get();

                        //if no label just use the image value
                        Color c = Color.WHITE;
                        double transFactor = 0.0;

                        if (!labels.isEmpty()) {
                            c = new Color(LabelingColorTableUtils.getAverageColor(colorMapping, labels));
                            transFactor = m_transparency.getIntValue() / 255.0;
                        }

                        raOut.setPosition(0, dims.length - 1);
                        raOut.get().set(alphaBlended(c.getRed(), normedImgValue, transFactor));
                        raOut.setPosition(1, dims.length - 1);
                        raOut.get().set(alphaBlended(c.getGreen(), normedImgValue, transFactor));
                        raOut.setPosition(2, dims.length - 1);
                        raOut.get().set(alphaBlended(c.getBlue(), normedImgValue, transFactor));
                    }

                } else {
                    //render only the label
                    while (labCur.hasNext()) {
                        labCur.fwd();
                        for (int d = 0; d < lab.numDimensions(); d++) {
                            raOut.setPosition(labCur.getLongPosition(d), d);
                        }
                        labels = labCur.get().getLabeling();
                        if (!labels.isEmpty()) {

                            final Color c = new Color(LabelingColorTableUtils.getAverageColor(colorMapping, labels));
                            raOut.setPosition(0, dims.length - 1);
                            raOut.get().set((byte)c.getRed());
                            raOut.setPosition(1, dims.length - 1);
                            raOut.get().set((byte)c.getGreen());
                            raOut.setPosition(2, dims.length - 1);
                            raOut.get().set((byte)c.getBlue());
                        }
                    }
                }

                final CalibratedAxis[] axes = new CalibratedAxis[res.numDimensions()];
                cellValue.getLabelingMetadata().axes(axes);
                axes[axes.length - 1] = new DefaultCalibratedAxis(Axes.get("Channel"));

                ImgPlus<UnsignedByteType> resImgPlus = new ImgPlus<UnsignedByteType>(res);
                for (int d = 0; d < resImgPlus.numDimensions(); d++) {
                    resImgPlus.setAxis(axes[d], d);
                }
                resImgPlus.setName(cellValue.getLabelingMetadata().getName());
                return m_imgCellFactory.createCell(resImgPlus);
            }

            /**
             * 
             * @param labelRed 0..255
             * @param imgValue 0..255
             * @param transFactor label value * trans + imgValue * (1-trans)
             * @return
             */
            private int alphaBlended(final int labelRed, final int imgValue, final double transFactor) {
                int res = (int)Math.round(labelRed * transFactor + imgValue * (1.0 - transFactor));

                if (res < 0) {
                    res = 0;
                } else if (res > 255) {
                    res = 255;
                }
                return res;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_imgCellFactory = new ImgPlusCellFactory(exec);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<LabelingValue<L>> createNodeDialog() {
        return new ValueToCellNodeDialog<LabelingValue<L>>() {

            @Override
            public void addDialogComponents() {
                @SuppressWarnings("unchecked")
                DialogComponentColumnNameSelection imgCol =
                        new DialogComponentColumnNameSelection(createImgColumnSM(), "Background image", 0, false, true,
                                ImgPlusValue.class);
                DialogComponentNumber transparency =
                        new DialogComponentNumber(createTransparencySM(), "Transparency of labels", 8);

                addDialogComponent("Background Image", "", imgCol);
                addDialogComponent("Background Image", "", transparency);
            }

        };
    }
}
