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
package org.knime.knip.base.data.aggregation;

import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.DefaultCalibratedSpace;
import net.imglib2.meta.DefaultNamed;
import net.imglib2.meta.DefaultSourced;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.IntervalValue;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.core.data.img.DefaultImageMetadata;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.types.NativeTypes;
import org.knime.knip.core.util.EnumListProvider;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgComposeOperator<T1 extends RealType<T1>, T2 extends RealType<T2> & NativeType<T2>> extends
        ImgAggregrationOperation {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ImgComposeOperator.class);

    private static SettingsModelString createImgTypeModel() {
        return new SettingsModelString("img_output_type", NativeTypes.BYTETYPE.toString());
    }

    private static SettingsModelString createIntervalColumnModel() {
        return new SettingsModelString("interval_column", "");
    }

    private DialogComponentStringSelection m_dcImgType;

    // dialog components
    private DialogComponent m_dcIntervalCol;

    // the default settings, i.e. if the respective globalsettings are not
    // set
    private String m_intervalCol = "";

    private long[] m_maxDims = null;

    // fields needed, if the result interval is not known in advance, i.e.
    // if the default settings are used
    private List<ImgPlusValue<T1>> m_patchList = null;

    private RandomAccess<T2> m_resAccess = null;

    // field for the labeling generation
    private Img<T2> m_resultImg = null;

    private ImgPlusMetadata m_resultMetadata = null;

    private T2 m_resultType = null;

    private final SettingsModelString m_smImgType = createImgTypeModel();

    // settings models
    private final SettingsModelString m_smIntervalCol = createIntervalColumnModel();

    public ImgComposeOperator() {
        super("compose_image_2", "Compose Image", "Compose Image");
    }

    /**
     * @param label
     * @param globalSettings
     */
    public ImgComposeOperator(final GlobalSettings globalSettings) {
        this(globalSettings, null, null);
    }

    /**
     * @param label
     * @param globalSettings
     */
    @SuppressWarnings("unchecked")
    public ImgComposeOperator(final GlobalSettings globalSettings, final String colName, final String type) {
        super("compose_image_2", "Compose Image", globalSettings);

        if (colName != null) {
            m_smIntervalCol.setStringValue(colName);
        }
        if (type != null) {
            m_smImgType.setStringValue(type);
        }

        m_intervalCol = m_smIntervalCol.getStringValue();
        m_resultType = (T2)NativeTypes.getTypeInstance(NativeTypes.valueOf(m_smImgType.getStringValue()));

    }

    /*
     * adds the bitmask to the labeling and return true, if it was
     * successfull
     */
    private boolean addToImage(final ImgPlusValue<T1> val) {
        final Img<T1> patch = val.getImgPlus();
        final long[] min = val.getMinimum();

        if (patch.numDimensions() != m_resultImg.numDimensions()) {
            return false;
        }

        // Set segmented pixels to label
        final Cursor<T1> patchCursor = patch.localizingCursor();
        final T1 minVal = patch.firstElement().createVariable();
        minVal.setReal(minVal.getMinValue());
        while (patchCursor.hasNext()) {
            patchCursor.fwd();
            if (patchCursor.get().compareTo(minVal) == 0) {
                continue;
            }
            final double curr = patchCursor.get().getRealDouble();

            for (int d = 0; d < Math.min(patchCursor.numDimensions(), m_resAccess.numDimensions()); d++) {
                m_resAccess.setPosition(min[d] + patchCursor.getLongPosition(d), d);
            }

            m_resAccess.get().setReal(curr);
        }
        return true;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected boolean computeInternal(final DataRow row, final DataCell cell) {

        final int intervalColIdx = getGlobalSettings().findColumnIndex(m_intervalCol);

        // if no column index is there, collect all tiles and put them
        // together to an image afterwards, if not, the result image can
        // be created in advance
        if (intervalColIdx == -1) {
            if (m_patchList == null) {
                m_patchList = new ArrayList<ImgPlusValue<T1>>();
            }
            final ImgPlusValue<T1> imgVal = (ImgPlusValue<T1>)cell;
            final long[] dims = imgVal.getDimensions();
            if (m_maxDims == null) {
                m_maxDims = new long[dims.length];
            }
            if (m_maxDims.length != dims.length) {
                LOGGER.warn("Patch in row " + row.getKey() + " cannot be added to the result.");
                return true;
            } else {
                final long[] min = imgVal.getMinimum();
                for (int i = 0; i < m_maxDims.length; i++) {
                    m_maxDims[i] = Math.max(m_maxDims[i], min[i] + dims[i]);
                }

                m_patchList.add(imgVal);
            }

        } else {
            if (m_resultImg == null) {
                final IntervalValue iv = (IntervalValue)row.getCell(intervalColIdx);
                // create labeling and metadata
                // todo make the generation of the result
                // labeling configurable
                final Interval i = new FinalInterval(iv.getMinimum(), iv.getMaximum());
                m_resultImg = new ArrayImgFactory<T2>().create(i, m_resultType);
                m_resAccess = Views.extendValue(m_resultImg, m_resultType).randomAccess();
                m_resultMetadata =
                        new DefaultImgMetadata(iv.getCalibratedSpace(), iv.getName(), iv.getSource(),
                                new DefaultImageMetadata());

            }

            if (!addToImage((ImgPlusValue<T1>)cell)) {
                setSkipMessage("Patch in row " + row.getKey() + " cannot be added to the result.");
                return true;
            }

        }
        return false;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final DataTableSpec spec) throws InvalidSettingsException {
        if ((m_smIntervalCol.getStringValue().length() > 1)
                && (spec.findColumnIndex(m_smIntervalCol.getStringValue()) < 0)) {
            throw new InvalidSettingsException("Cannot find interval column.");
        }
        if (m_smIntervalCol.getStringValue().length() == 0) {
            LOGGER.warn("Creating a composed image with no selected interval may be less efficient and may not meet the requirement.");
        }
    }

    private void createDCs() {
        if (m_dcIntervalCol == null) {
            m_dcIntervalCol =
                    new DialogComponentColumnNameSelection(createIntervalColumnModel(),
                            "Interval (the size of the result image)", 0, false, true, IntervalValue.class);

            m_dcImgType =
                    new DialogComponentStringSelection(createImgTypeModel(), "Result image type",
                            EnumListProvider.getStringList(NativeTypes.SHORTTYPE, NativeTypes.BITTYPE,
                                                           NativeTypes.BYTETYPE, NativeTypes.INTTYPE,
                                                           NativeTypes.UNSIGNEDSHORTTYPE, NativeTypes.UNSIGNEDINTTYPE,
                                                           NativeTypes.UNSIGNEDBYTETYPE));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
                                              final OperatorColumnSettings opColSettings) {
        return new ImgComposeOperator<T1, T2>(globalSettings, m_smIntervalCol.getStringValue(),
                m_smImgType.getStringValue());
    }

    @Override
    public Collection<String> getAdditionalColumnNames() {
        if (m_smIntervalCol.getStringValue().length() > 0) {
            final ArrayList<String> cols = new ArrayList<String>();
            cols.add(m_smIntervalCol.getStringValue());
            return cols;
        } else {
            return super.getAdditionalColumnNames();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return ImgPlusCell.TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Composes images from image patches.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        if (m_intervalCol.length() == 0) {
            // compose result and return
            m_resultImg = new ArrayImgFactory<T2>().create(m_maxDims, m_resultType);
            m_resultMetadata =
                    new DefaultImgMetadata(new DefaultCalibratedSpace(m_maxDims.length), new DefaultNamed("Unknown"),
                            new DefaultSourced("Unknown"), new DefaultImageMetadata());
            m_resAccess = m_resultImg.randomAccess();
            for (final ImgPlusValue<T1> imgVal : m_patchList) {
                addToImage(imgVal);
            }
        }

        // return the already composed result
        try {
            return getImgPlusCellFactory().createCell(m_resultImg, m_resultMetadata);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Component getSettingsPanel() {
        createDCs();
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(m_dcIntervalCol.getComponentPanel());
        panel.add(m_dcImgType.getComponentPanel());
        return panel;
    }

    @Override
    public boolean hasOptionalSettings() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
            throws NotConfigurableException {
        createDCs();
        m_dcIntervalCol.loadSettingsFrom(settings, new DataTableSpec[]{spec});
        m_dcImgType.loadSettingsFrom(settings, new DataTableSpec[]{spec});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_smIntervalCol.loadSettingsFrom(settings);
        if (m_smIntervalCol.getStringValue() == null) {
            m_smIntervalCol.setStringValue("");
        }
        m_smImgType.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        // labeling creation fields
        m_resultImg = null;
        m_resultMetadata = null;
        m_resAccess = null;
        m_resultType = null;

        // "lazy" labeling creation
        if (m_patchList != null) {
            m_patchList.clear();
        }
        m_maxDims = null;

        // default settings
        m_intervalCol = "";

    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_dcIntervalCol != null) {
            try {
                m_dcIntervalCol.saveSettingsTo(settings);
                m_dcImgType.saveSettingsTo(settings);
            } catch (final InvalidSettingsException e) {
                throw new RuntimeException(e.getMessage());
            }
        } else {
            m_smIntervalCol.saveSettingsTo(settings);
            m_smImgType.saveSettingsTo(settings);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_smIntervalCol.validateSettings(settings);
        m_smImgType.validateSettings(settings);
    }
}
