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
package org.knime.knip.base.nodes.io.imggenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.core.io.ImgGenerator;
import org.knime.knip.core.types.ImgFactoryTypes;
import org.knime.knip.core.types.NativeTypes;

/**
 * Generates images.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgGeneratorNodeModel<T extends NativeType<T> & RealType<T>> extends NodeModel implements
        BufferedDataTableHolder {

    static SettingsModelBoolean createCFGFillRandom() {
        return new SettingsModelBoolean("cfg_fill_random", false);
    }

    static SettingsModelBoolean createCFGRandomSize() {
        return new SettingsModelBoolean("cfg_random_size", false);
    }

    static SettingsModelString createCFGFactory() {
        return new SettingsModelString("cfg_factory", "Random");
    }

    static SettingsModelIntegerBounded createCFGSizeX() {
        return new SettingsModelIntegerBounded("cfg_size_x", 10, 0, Integer.MAX_VALUE);
    }

    static SettingsModelIntegerBounded createCFGSizeY() {
        return new SettingsModelIntegerBounded("cfg_size_y", 10, 0, Integer.MAX_VALUE);
    }

    static SettingsModelIntegerBounded createCFGSizeZ() {
        return new SettingsModelIntegerBounded("cfg_size_z", 0, 0, Integer.MAX_VALUE);
    }

    static SettingsModelIntegerBounded createCFGSizeChannel() {
        return new SettingsModelIntegerBounded("cfg_size_c", 0, 0, Integer.MAX_VALUE);
    }

    static SettingsModelIntegerBounded createCFGSizeT() {
        return new SettingsModelIntegerBounded("cfg_size_t", 0, 0, Integer.MAX_VALUE);
    }

    static SettingsModelString createCFGType() {
        return new SettingsModelString("cfg_type", "Random");
    }

    static SettingsModelIntegerBounded createCFGNumImg() {
        return new SettingsModelIntegerBounded("cfg_num_images", 1, 0, Integer.MAX_VALUE);
    }

    static SettingsModelDouble createCFGValue() {
        return new SettingsModelDouble("cfg_fill_value", 0.0);
    }

    //min values
    static SettingsModelIntegerBounded createCFGSizeXMin() {
        return new SettingsModelIntegerBounded("cfg_size_x_min", 1, 0, Integer.MAX_VALUE);
    }

    static SettingsModelIntegerBounded createCFGSizeYMin() {
        return new SettingsModelIntegerBounded("cfg_size_y_min", 1, 0, Integer.MAX_VALUE);
    }

    static SettingsModelIntegerBounded createCFGSizeZMin() {
        return new SettingsModelIntegerBounded("cfg_size_z_min", 0, 0, Integer.MAX_VALUE);
    }

    static SettingsModelIntegerBounded createCFGSizeChannelMin() {
        return new SettingsModelIntegerBounded("cfg_size_channel_min", 0, 0, Integer.MAX_VALUE);
    }

    static SettingsModelIntegerBounded createCFGSizeTMin() {
        return new SettingsModelIntegerBounded("cfg_size_t_min", 0, 0, Integer.MAX_VALUE);
    }

    private static NodeLogger LOGGER = NodeLogger.getLogger(ImgGeneratorNodeModel.class);

    public static String[] SUPPORTED_FACTORIES = {ImgFactoryTypes.ARRAY_IMG_FACTORY.toString(),
            ImgFactoryTypes.CELL_IMG_FACTORY.toString(), ImgFactoryTypes.NTREE_IMG_FACTORY.toString(),
            // ImgFactoryTypes.LIST_IMG_FACTORY.toString(),
            ImgFactoryTypes.PLANAR_IMG_FACTORY.toString(), "Random"};

    public static String[] SUPPORTED_TYPES = {NativeTypes.BITTYPE.toString(), NativeTypes.BYTETYPE.toString(),
            NativeTypes.DOUBLETYPE.toString(), NativeTypes.FLOATTYPE.toString(), NativeTypes.INTTYPE.toString(),
            NativeTypes.LONGTYPE.toString(), NativeTypes.SHORTTYPE.toString(),
            NativeTypes.UNSIGNEDSHORTTYPE.toString(), NativeTypes.UNSIGNED12BITTYPE.toString(),
            NativeTypes.UNSIGNEDINTTYPE.toString(), NativeTypes.UNSIGNEDBYTETYPE.toString(), "Random"};

    private BufferedDataTable m_data;

    private final ImgGenerator m_gen;

    private final SettingsModelIntegerBounded m_numImgs = createCFGNumImg();

    /*
     * The image-output-table DataSpec
     */
    private DataTableSpec m_outspec;

    private final ArrayList<SettingsModel> m_settings;

    private final SettingsModelString m_smFactory = createCFGFactory();

    private final SettingsModelBoolean m_smRandomFill = createCFGFillRandom();

    private final SettingsModelBoolean m_smRandomSize = createCFGRandomSize();

    private final SettingsModelIntegerBounded m_smSizeChannel = createCFGSizeChannel();

    private final SettingsModelIntegerBounded m_smSizeT = createCFGSizeT();

    private final SettingsModelIntegerBounded m_smSizeX = createCFGSizeX();

    private final SettingsModelIntegerBounded m_smSizeY = createCFGSizeY();

    private final SettingsModelIntegerBounded m_smSizeZ = createCFGSizeZ();

    private final SettingsModelString m_smType = createCFGType();

    private final SettingsModelDouble m_smValue = createCFGValue();

    private final SettingsModelIntegerBounded m_smSizeX_Min = createCFGSizeXMin();

    private final SettingsModelIntegerBounded m_smSizeY_Min = createCFGSizeYMin();

    private final SettingsModelIntegerBounded m_smSizeZ_Min = createCFGSizeZMin();

    private final SettingsModelIntegerBounded m_smSizeChannel_Min = createCFGSizeChannelMin();

    private final SettingsModelIntegerBounded m_smSizeT_Min = createCFGSizeTMin();

    /**
     * Initializes the ImageReader
     */
    public ImgGeneratorNodeModel() {
        super(0, 1);
        m_settings = new ArrayList<SettingsModel>();

        m_settings.add(m_numImgs);
        m_settings.add(m_smFactory);
        m_settings.add(m_smRandomFill);
        m_settings.add(m_smRandomSize);
        m_settings.add(m_smSizeChannel);
        m_settings.add(m_smSizeX);
        m_settings.add(m_smSizeY);
        m_settings.add(m_smSizeZ);
        m_settings.add(m_smSizeT);
        m_settings.add(m_smType);
        m_settings.add(m_smValue);

        m_settings.add(m_smSizeX_Min);
        m_settings.add(m_smSizeY_Min);
        m_settings.add(m_smSizeZ_Min);
        m_settings.add(m_smSizeT_Min);
        m_settings.add(m_smSizeChannel_Min);

        m_gen = new ImgGenerator();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        final ArrayList<DataColumnSpec> specs = new ArrayList<DataColumnSpec>();
        specs.add(new DataColumnSpecCreator("Img", ImgPlusCell.TYPE).createSpec());
        m_outspec = new DataTableSpec(specs.toArray(new DataColumnSpec[specs.size()]));

        if (m_smSizeX_Min.getIntValue() > m_smSizeX.getIntValue()) {
            throw new InvalidSettingsException("X: min size can't be bigger than max size");
        }
        if (m_smSizeY_Min.getIntValue() > m_smSizeY.getIntValue()) {
            throw new InvalidSettingsException("Y: min size can't be bigger than max size");
        }
        if (m_smSizeZ_Min.getIntValue() > m_smSizeZ.getIntValue()) {
            throw new InvalidSettingsException("Z: min size can't be bigger than max size");
        }
        if (m_smSizeChannel_Min.getIntValue() > m_smSizeChannel.getIntValue()) {
            throw new InvalidSettingsException("Channel: min size can't be bigger than max size");
        }
        if (m_smSizeT_Min.getIntValue() > m_smSizeT.getIntValue()) {
            throw new InvalidSettingsException("T: min size can't be bigger than max size");
        }

        return new DataTableSpec[]{m_outspec};

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        final ImgPlusCellFactory imgCellFactory = new ImgPlusCellFactory(exec);

        // set all values for the creation
        m_gen.setRandomSize(m_smRandomSize.getBooleanValue());
        m_gen.setRandomFill(m_smRandomFill.getBooleanValue());

        m_gen.setValue(m_smValue.getDoubleValue());

        m_gen.setSizeX(m_smSizeX_Min.getIntValue(), m_smSizeX.getIntValue());
        m_gen.setSizeY(m_smSizeY_Min.getIntValue(), m_smSizeY.getIntValue());
        m_gen.setSizeZ(m_smSizeZ_Min.getIntValue(), m_smSizeZ.getIntValue());
        m_gen.setSizeChannel(m_smSizeChannel_Min.getIntValue(), m_smSizeChannel.getIntValue());
        m_gen.setSizeT(m_smSizeT_Min.getIntValue(), m_smSizeT.getIntValue());

        // Set Type and Factory
        if (m_smType.getStringValue().equalsIgnoreCase("Random")) {
            m_gen.setType(null);
            m_gen.setRandomType(true);
        } else {
            m_gen.setType(NativeTypes.valueOf(m_smType.getStringValue().toUpperCase()));
            m_gen.setRandomType(false);
        }

        if (m_smFactory.getStringValue().equalsIgnoreCase("Random")) {
            m_gen.setFactory(null);
            m_gen.setRandomFactory(true);
        } else {
            m_gen.setFactory(ImgFactoryTypes.valueOf(m_smFactory.getStringValue()));
            m_gen.setRandomFactory(false);
        }

        final BufferedDataContainer con = exec.createDataContainer(m_outspec);
        for (int k = 0; k < m_numImgs.getIntValue(); k++) {
            String fac = "?";
            String type = "?";
            try {
                final ImgPlus<T> img = m_gen.nextImage();

                fac = img.factory().toString();
                type = NativeTypes.getPixelType(img.firstElement()).toString();
                con.addRowToTable(new DefaultRow(new RowKey("img_" + type + "_" + fac + "_" + k), imgCellFactory
                        .createCell(img)));

            } catch (final Exception e) {
                // catch exception, if some combinations of
                // types are not supported yet
                LOGGER.warn("Image can't be generated.", e);
                setWarningMessage("Some erros occured!");

                con.addRowToTable(new DefaultRow(new RowKey("img_missing_" + type + "_" + fac + "_" + k), DataType
                        .getMissingCell()));
            }

            exec.setProgress((double)k / m_numImgs.getIntValue());
            exec.checkCanceled();

        }
        con.close();
        m_data = con.getTable();
        return new BufferedDataTable[]{m_data};
    }

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
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        //TODO test all settings models
        for (final SettingsModel sm : m_settings) {
            //temporary remove this later
            if (sm.equals(m_smSizeX_Min) || sm.equals(m_smSizeY_Min) || sm.equals(m_smSizeZ_Min)
                    || sm.equals(m_smSizeChannel_Min) || sm.equals(m_smSizeT_Min)) {
                try {
                    sm.loadSettingsFrom(settings);
                } catch (InvalidSettingsException e) {
                    ((SettingsModelIntegerBounded)sm).setIntValue(0);
                }

            } else {
                sm.loadSettingsFrom(settings);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        for (final SettingsModel sm : m_settings) {
            sm.saveSettingsTo(settings);
        }
    }

    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_data = tables[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        //TODO test all models not only the old ones

        for (final SettingsModel sm : m_settings) {
            if (sm.equals(m_smSizeX_Min) || sm.equals(m_smSizeY_Min) || sm.equals(m_smSizeZ_Min)
                    || sm.equals(m_smSizeChannel_Min) || sm.equals(m_smSizeT_Min)) {
                //do nothing these models are new and might not be there
            } else {
                sm.validateSettings(settings);
            }
        }
    }
}
