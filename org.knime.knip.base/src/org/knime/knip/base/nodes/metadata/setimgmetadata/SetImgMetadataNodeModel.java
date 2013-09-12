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
package org.knime.knip.base.nodes.metadata.setimgmetadata;

import java.io.File;
import java.io.IOException;

import net.imglib2.meta.Axes;
import net.imglib2.meta.DefaultCalibratedAxis;
import net.imglib2.meta.DefaultCalibratedSpace;
import net.imglib2.meta.DefaultNamed;
import net.imglib2.meta.DefaultSourced;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.NodeTools;
import org.knime.knip.core.data.img.DefaultImgMetadata;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SetImgMetadataNodeModel<T extends RealType<T>> extends NodeModel implements BufferedDataTableHolder {

    static SettingsModelDouble[] createCalibrationModels() {
        final SettingsModelDouble[] models = new SettingsModelDouble[5];
        for (int i = 0; i < models.length; i++) {
            models[i] = new SettingsModelDouble(i + "calibration", 0);
        }
        return models;
    }

    static SettingsModelString[] createDimLabelModels() {
        final SettingsModelString[] models = new SettingsModelString[5];
        for (int i = 0; i < models.length; i++) {
            models[i] = new SettingsModelString(i + "dim_label", "");
        }
        return models;
    }

    static SettingsModelString createImgColModel() {
        return new SettingsModelString("img_col", "");
    }

    static SettingsModelString createNameColModel() {
        return new SettingsModelString("name_col", "");
    }

    static SettingsModelString[] createOffSetColModels() {
        final SettingsModelString[] models = new SettingsModelString[5];
        for (int i = 0; i < models.length; i++) {
            models[i] = new SettingsModelString(i + "offset", "");
        }
        return models;
    }

    static SettingsModelString createSourceColModel() {
        return new SettingsModelString("source_col", "");
    }

    private final SettingsModelDouble[] m_calibrations = createCalibrationModels();

    /* data table for the table cell view */
    private BufferedDataTable m_data;

    private final SettingsModelString[] m_dimLabels = createDimLabelModels();

    private final SettingsModelString m_imgCol = createImgColModel();

    private final SettingsModelString m_nameCol = createNameColModel();

    private final SettingsModelString[] m_offsets = createOffSetColModels();

    private final SettingsModelString m_sourceCol = createSourceColModel();

    /**
     * @param nrInDataPorts
     * @param nrOutDataPorts
     */
    protected SetImgMetadataNodeModel() {
        super(1, 1);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        NodeTools.autoColumnSelection(inSpecs[0], m_imgCol, ImgPlusValue.class, this.getClass());
        final ColumnRearranger rearranger = new ColumnRearranger(inSpecs[0]);
        rearranger.replace(createCellFactory(inSpecs[0], null, -1), m_imgCol.getStringValue());
        return new DataTableSpec[]{rearranger.createSpec()};
    }

    private CellFactory createCellFactory(final DataTableSpec inSpec, final ImgPlusCellFactory imgFactory,
                                          final int imgColIdx) {

        return new CellFactory() {

            private int m_nameColIdx = -1;

            private final int[] m_offsetColIndices = new int[m_offsets.length];

            private int m_sourceColIdx = -1;

            {
                m_nameColIdx = inSpec.findColumnIndex(m_nameCol.getStringValue());
                m_sourceColIdx = inSpec.findColumnIndex(m_sourceCol.getStringValue());
                for (int i = 0; i < m_offsetColIndices.length; i++) {
                    m_offsetColIndices[i] = inSpec.findColumnIndex(m_offsets[i].getStringValue());
                }
            }

            @Override
            public DataCell[] getCells(final DataRow row) {
                //check for missing cell
                if (row.getCell(imgColIdx).isMissing()) {
                    return new DataCell[]{DataType.getMissingCell()};
                }

                final ImgPlusValue<T> imgPlusVal = (ImgPlusValue<T>)row.getCell(imgColIdx);
                // No need to copy here. will be persisted after
                // it is read out and images from cells
                // shouldn't be manipulated anyway!
                final ImgPlus<T> imgPlus = imgPlusVal.getImgPlus();
                String name;
                if ((m_nameColIdx != -1) && !row.getCell(m_nameColIdx).isMissing()) {
                    name = ((StringValue)row.getCell(m_nameColIdx)).getStringValue();
                } else {
                    name = imgPlus.getName();
                }

                String source;
                if ((m_sourceColIdx != -1) && !row.getCell(m_sourceColIdx).isMissing()) {
                    source = ((StringValue)row.getCell(m_sourceColIdx)).getStringValue();
                } else {
                    source = imgPlus.getSource();
                }

                final String[] axisLabels = new String[imgPlus.numDimensions()];

                final double[] calibration = new double[axisLabels.length];
                for (int i = 0; i < imgPlus.numDimensions(); i++) {
                    if (m_dimLabels[i].getStringValue().trim().length() > 0) {
                        axisLabels[i] = m_dimLabels[i].getStringValue().trim();
                    } else {
                        axisLabels[i] = imgPlus.axis(i).type().getLabel();
                    }
                    if (m_calibrations[i].getDoubleValue() > 0) {
                        calibration[i] = m_calibrations[i].getDoubleValue();
                    } else {
                        calibration[i] = imgPlus.calibration(i);
                    }
                }

                final long[] min = imgPlusVal.getMinimum();
                for (int i = 0; i < Math.min(min.length, m_offsetColIndices.length); i++) {
                    if (m_offsetColIndices[i] != -1) {
                        min[i] = ((LongValue)row.getCell(m_offsetColIndices[i])).getLongValue();
                    }
                }

                DefaultCalibratedSpace defaultCalibratedSpace = new DefaultCalibratedSpace(axisLabels.length);
                for (int i = 0; i < axisLabels.length; i++) {
                    defaultCalibratedSpace.setAxis(new DefaultCalibratedAxis(Axes.get(axisLabels[i])), i);

                }

                defaultCalibratedSpace.setCalibration(calibration);

                final ImgPlusMetadata metadata =
                        new DefaultImgMetadata(defaultCalibratedSpace, new DefaultNamed(name), new DefaultSourced(
                                source), imgPlus);

                try {
                    return new DataCell[]{imgFactory.createCell(imgPlus.getImg(), metadata, min)};
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public DataColumnSpec[] getColumnSpecs() {
                return new DataColumnSpec[]{new DataColumnSpecCreator(m_imgCol.getStringValue(), ImgPlusCell.TYPE)
                        .createSpec()};
            }

            @Override
            public void setProgress(final int curRowNr, final int rowCount, final RowKey lastKey,
                                    final ExecutionMonitor exec) {
                exec.setProgress(curRowNr / (double)rowCount);

            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        final ColumnRearranger rearranger = new ColumnRearranger(inData[0].getDataTableSpec());
        final int idx =
                NodeTools.autoColumnSelection(inData[0].getDataTableSpec(), m_imgCol, ImgPlusValue.class,
                                              this.getClass());
        rearranger.replace(createCellFactory(inData[0].getDataTableSpec(), new ImgPlusCellFactory(exec),

        idx), m_imgCol.getStringValue());

        // data for the table cell view
        m_data = exec.createColumnRearrangeTable(inData[0], rearranger, exec);

        return new BufferedDataTable[]{m_data};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{m_data};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_imgCol.loadSettingsFrom(settings);
        m_nameCol.loadSettingsFrom(settings);
        m_sourceCol.loadSettingsFrom(settings);
        for (final SettingsModel sm : m_dimLabels) {
            sm.loadSettingsFrom(settings);

        }
        for (final SettingsModel sm : m_calibrations) {
            sm.loadSettingsFrom(settings);

        }
        try {
            for (final SettingsModel sm : m_offsets) {
                sm.loadSettingsFrom(settings);
            }
        } catch (final InvalidSettingsException e) {
            //
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_imgCol.saveSettingsTo(settings);
        m_nameCol.saveSettingsTo(settings);
        m_sourceCol.saveSettingsTo(settings);
        for (final SettingsModel sm : m_dimLabels) {
            sm.saveSettingsTo(settings);

        }
        for (final SettingsModel sm : m_calibrations) {
            sm.saveSettingsTo(settings);

        }
        for (final SettingsModel sm : m_offsets) {
            sm.saveSettingsTo(settings);

        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_data = tables[0];

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_imgCol.validateSettings(settings);
        m_nameCol.validateSettings(settings);
        m_sourceCol.validateSettings(settings);
        for (final SettingsModel sm : m_dimLabels) {
            sm.validateSettings(settings);

        }
        for (final SettingsModel sm : m_calibrations) {
            sm.validateSettings(settings);

        }
        try {
            for (final SettingsModel sm : m_offsets) {
                sm.validateSettings(settings);

            }
        } catch (final InvalidSettingsException e) {
            //
        }

    }

}
