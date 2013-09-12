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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.meta.DefaultCalibratedSpace;
import net.imglib2.meta.DefaultNamed;
import net.imglib2.meta.DefaultSourced;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorHandler;
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
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.data.IntegerLabelGenerator;
import org.knime.knip.core.data.LabelGenerator;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.core.types.ConstantLabelingType;
import org.knime.knip.core.types.ImgFactoryTypes;
import org.knime.knip.core.types.NativeTypes;
import org.knime.knip.core.util.EnumListProvider;
import org.knime.knip.core.util.Triple;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LabelingComposeOperator<T extends IntegerType<T> & NativeType<T>> extends LabelingAggregrationOperation {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LabelingComposeOperator.class);

    private static SettingsModelString createAvoidOverlapOrderColModel() {
        return new SettingsModelString("avoid_overlap_column_order", "");
    }

    private static SettingsModelString createIntervalColumnModel() {
        return new SettingsModelString("interval_column", "");
    }

    private static SettingsModelString createLabelColumnModel() {
        return new SettingsModelString("label_column", "");
    }

    private static SettingsModelString createLabelingFactoryModel() {
        return new SettingsModelString("labeling_factory", ImgFactoryTypes.ARRAY_IMG_FACTORY.toString());
    }

    private static SettingsModelString createLabelingTypeModel() {
        return new SettingsModelString("labeling_type", NativeTypes.SHORTTYPE.toString());
    }

    // fields needed, if the result interval is not known in advance, i.e.
    // if the default settings are used
    private List<Triple<ImgPlusValue<BitType>, String, Double>> m_bitMaskList = null;

    private DialogComponent m_dcAvoidOverlapCol;

    // dialog components
    private DialogComponent m_dcIntervalCol;

    private DialogComponent m_dcLabelCol;

    private DialogComponentStringSelection m_dcLabelingFactory;

    private DialogComponentStringSelection m_dcLabelingType;

    private ImgFactory<T> m_factory = null;

    // the default settings, i.e. if the respective globalsettings are not
    // set
    private String m_intervalCol = "";

    private String m_labelCol = "";

    private final LabelGenerator<Integer> m_labelGenerator = new IntegerLabelGenerator();

    private long[] m_maxDims = null;

    private RandomAccess<LabelingType<String>> m_resAccess = null;

    private T m_resType;

    // field for the labeling generation
    private Labeling<String> m_resultLab = null;

    private LabelingMetadata m_resultMetadata = null;

    private final SettingsModelString m_smAvoidOverlapCol = createAvoidOverlapOrderColModel();

    // settings models
    private final SettingsModelString m_smIntervalCol = createIntervalColumnModel();

    private final SettingsModelString m_smLabelCol = createLabelColumnModel();

    private final SettingsModelString m_smLabelingFactory = createLabelingFactoryModel();

    private final SettingsModelString m_smLabelingType = createLabelingTypeModel();

    private HashSet<String> m_labelList = null;

    private int m_labelColIdx = -1;

    public LabelingComposeOperator() {
        super("compose_labeling_2", "Compose Labeling", "Compose Labeling");
    }

    /**
     * @param label
     * @param globalSettings
     */
    public LabelingComposeOperator(final GlobalSettings globalSettings) {
        this(globalSettings, null, null, null, null, null);
    }

    @SuppressWarnings("unchecked")
    protected LabelingComposeOperator(final GlobalSettings globalSettings, final String intervalColName,
                                      final String labelColName, final String labelingType,
                                      final String labelingFactory, final String overlapCol) {
        super("compose_labeling_2", "Compose Labeling", globalSettings);
        if (intervalColName != null) {
            m_smIntervalCol.setStringValue(intervalColName);
        }
        if (labelColName != null) {
            m_smLabelCol.setStringValue(labelColName);
        }
        if (labelingType != null) {
            m_smLabelingType.setStringValue(labelingType);
        }
        if (labelingFactory != null) {
            m_smLabelingFactory.setStringValue(labelingFactory);
        }
        if (overlapCol != null) {
            m_smAvoidOverlapCol.setStringValue(overlapCol);
        }

        m_intervalCol = m_smIntervalCol.getStringValue();
        m_labelCol = m_smLabelCol.getStringValue();
        m_factory = ImgFactoryTypes.getImgFactory(m_smLabelingFactory.getStringValue(), null);
        m_resType = (T)NativeTypes.getTypeInstance(NativeTypes.valueOf(m_smLabelingType.getStringValue()));
    }

    /*
     * adds the bitmask to the labeling and return true, if it was
     * successfull
     */
    private boolean addToLabeling(final ImgPlusValue<BitType> val, String label) {
        final Img<BitType> img = val.getImgPlus();
        final long[] min = val.getMinimum();

        if (img.numDimensions() != m_resultLab.numDimensions()) {
            return false;
        }
        if (label == null) {
            label = "" + m_labelGenerator.nextLabel();
        }

        // Set segmented pixels to label
        final Cursor<BitType> cur = img.localizingCursor();
        while (cur.hasNext()) {
            cur.fwd();
            if (!cur.get().get()) {
                continue;
            }
            for (int d = 0; d < min.length; d++) {
                if (img.numDimensions() > d) {
                    m_resAccess.setPosition(Math.min(m_resultLab.dimension(d),
                                                     Math.max(0, min[d] + cur.getLongPosition(d))), d);
                } else {
                    m_resAccess.setPosition(Math.min(m_resultLab.dimension(d), Math.max(0, min[d])), d);
                }

            }

            if (m_resAccess.get().getLabeling().isEmpty()) {
                m_resAccess.get().setLabel(label);
            } else {
                final ArrayList<String> tmp = new ArrayList<String>(m_resAccess.get().getLabeling());
                tmp.add(label);
                m_resAccess.get().setLabeling(tmp);
            }

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
    @Override
    protected boolean computeInternal(final DataRow row, final DataCell cell) {

        // check correct pixel type
        if (!(((ImgPlusValue)cell).getImgPlus().firstElement() instanceof BitType)) {
            setSkipMessage("The Bitmask in row " + row.getKey() + " is not of pixel type BitType!");
            return true;
        }
        final int intervalColIdx = getGlobalSettings().findColumnIndex(m_intervalCol);

        final int avoidOverlapColIdx = getGlobalSettings().findColumnIndex(m_smAvoidOverlapCol.getStringValue());

        // create result labeling, if the interval is known
        // if no column index is there, collect all tiles and put them
        // together to an image afterwards, if not, the result image can
        // be created in advance
        if ((intervalColIdx != -1) && (m_resultLab == null)) {
            final IntervalValue iv = (IntervalValue)row.getCell(intervalColIdx);
            // create labeling and metadata
            // todo make the generation of the result
            // labeling configurable
            final Interval i = new FinalInterval(iv.getMinimum(), iv.getMaximum());
            m_resultLab = new NativeImgLabeling<String, T>(m_factory.create(i, m_resType));
            m_resAccess =
                    Views.extendValue(m_resultLab, (LabelingType<String>)new ConstantLabelingType<String>(""))
                            .randomAccess();

            m_resultMetadata =
                    new DefaultLabelingMetadata(iv.getCalibratedSpace(), iv.getName(), iv.getSource(),
                            new DefaultLabelingColorTable());

            m_labelColIdx = getGlobalSettings().findColumnIndex(m_labelCol);

            if (m_labelColIdx > -1 && getGlobalSettings().getOriginalColumnSpec(m_labelCol).getColorHandler() != null) {
                m_labelList = new HashSet<String>();
            }

        }

        String label = null;
        if (m_labelColIdx != -1) {
            label = row.getCell(m_labelColIdx).toString();
            if (m_labelList != null) {
                m_labelList.add(label);
            }
        }

        // add the bitmask either to the the bitmask list (if the
        // interval is not known or overlapping segments should be
        // filtered or add them directly to the labeling
        if ((intervalColIdx == -1) || (avoidOverlapColIdx != -1)) {
            if (m_bitMaskList == null) {
                m_bitMaskList = new ArrayList<Triple<ImgPlusValue<BitType>, String, Double>>();
            }
            final ImgPlusValue<BitType> imgVal = (ImgPlusValue<BitType>)cell;
            final long[] dims = imgVal.getDimensions();
            if (m_maxDims == null) {
                m_maxDims = new long[dims.length];
            }
            if (m_maxDims.length != dims.length) {
                LOGGER.warn("Bitmask in row " + row.getKey() + " cannot be added to the result.");
                return true;
            } else {
                final long[] min = imgVal.getMinimum();
                for (int i = 0; i < m_maxDims.length; i++) {
                    m_maxDims[i] = Math.max(m_maxDims[i], min[i] + dims[i]);
                }
                Double order = null;
                if (avoidOverlapColIdx != -1) {
                    order = ((DoubleValue)row.getCell(avoidOverlapColIdx)).getDoubleValue();
                }
                m_bitMaskList.add(new Triple<ImgPlusValue<BitType>, String, Double>(imgVal, label, order));
            }

        } else {
            final boolean success = addToLabeling((ImgPlusValue<BitType>)cell, label);
            if (!success) {
                setSkipMessage("Bitmask in row " + row.getKey() + " cannot be added to the result.");
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
        if ((m_smIntervalCol.getStringValue().length() > 0)
                && (spec.findColumnIndex(m_smIntervalCol.getStringValue()) < 0)) {
            throw new InvalidSettingsException("Cannot find interval column.");
        }
        if ((m_smLabelCol.getStringValue().length() > 0) && (spec.findColumnIndex(m_smLabelCol.getStringValue()) < 0)) {
            throw new InvalidSettingsException("Cannot find label column.");
        }
        if ((m_smAvoidOverlapCol.getStringValue().length() > 0)
                && (spec.findColumnIndex(m_smAvoidOverlapCol.getStringValue()) < 0)) {
            throw new InvalidSettingsException("Cannot find overlap order column.");
        }
        if (m_smIntervalCol.getStringValue().length() == 0) {
            LOGGER.warn("Creating a composed labeling with no selected interval may be less efficient and may not meet the requirement.");
        }

    }

    private void createDCs() {
        if (m_dcIntervalCol == null) {
            m_dcIntervalCol =
                    new DialogComponentColumnNameSelection(createIntervalColumnModel(),
                            "Interval (the size of the result labeling)", 0, false, true, IntervalValue.class);
            m_dcLabelCol =
                    new DialogComponentColumnNameSelection(createLabelColumnModel(), "Label", 0, false, true,
                            DataValue.class);

            m_dcLabelingType =
                    new DialogComponentStringSelection(createLabelingTypeModel(), "Result labeling type",
                            EnumListProvider.getStringList(NativeTypes.SHORTTYPE, NativeTypes.BITTYPE,
                                                           NativeTypes.BYTETYPE, NativeTypes.INTTYPE,
                                                           NativeTypes.UNSIGNEDSHORTTYPE, NativeTypes.UNSIGNEDINTTYPE,
                                                           NativeTypes.UNSIGNEDBYTETYPE));

            m_dcLabelingFactory =
                    new DialogComponentStringSelection(createLabelingFactoryModel(), "Result labeling factory",
                            EnumListProvider.getStringList(ImgFactoryTypes.ARRAY_IMG_FACTORY,
                                                           ImgFactoryTypes.CELL_IMG_FACTORY,
                                                           ImgFactoryTypes.NTREE_IMG_FACTORY,
                                                           ImgFactoryTypes.PLANAR_IMG_FACTORY));
            m_dcAvoidOverlapCol =
                    new DialogComponentColumnNameSelection(createAvoidOverlapOrderColModel(),
                            "Order to avoid segment overlap", 0, false, true, DoubleValue.class);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
                                              final OperatorColumnSettings opColSettings) {
        return new LabelingComposeOperator(globalSettings, m_smIntervalCol.getStringValue(),
                m_smLabelCol.getStringValue(), m_smLabelingType.getStringValue(), m_smLabelingFactory.getStringValue(),
                m_smAvoidOverlapCol.getStringValue());
    }

    /*
     * filters overlapping segments
     */
    private void filterBitMaskList() {
        // sort bitmask list
        Collections.sort(m_bitMaskList, new Comparator<Triple<ImgPlusValue<BitType>, String, Double>>() {
            @Override
            public int compare(final Triple<ImgPlusValue<BitType>, String, Double> o1,
                               final Triple<ImgPlusValue<BitType>, String, Double> o2) {
                return o2.getThird().compareTo(o1.getThird());
            }
        });
        for (int i = 0; i < m_bitMaskList.size(); i++) {
            if (m_bitMaskList.get(i) == null) {
                continue;
            }
            for (int j = i + 1; j < m_bitMaskList.size(); j++) {
                if (m_bitMaskList.get(j) != null) {
                    if (overlap(m_bitMaskList.get(i).getFirst(), m_bitMaskList.get(j).getFirst())) {
                        m_bitMaskList.set(j, null);
                    }
                }
            }

        }
    }

    @Override
    public Collection<String> getAdditionalColumnNames() {
        final ArrayList<String> cols = new ArrayList<String>();
        if (m_smIntervalCol.getStringValue().length() > 0) {

            cols.add(m_smIntervalCol.getStringValue());
        }
        if (m_smLabelCol.getStringValue().length() > 0) {

            cols.add(m_smLabelCol.getStringValue());
        }
        if (m_smAvoidOverlapCol.getStringValue().length() > 0) {
            cols.add(m_smAvoidOverlapCol.getStringValue());
        }

        return cols;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return LabelingCell.TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Composes labelings from binary images.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        if (m_smAvoidOverlapCol.getStringValue().length() != 0) {
            filterBitMaskList();
        }

        if (m_intervalCol.length() == 0) {
            // compose result and return
            m_resultLab = new NativeImgLabeling<String, T>(m_factory.create(m_maxDims, m_resType));
            //TODO: Make Missing Color handler selectable
            m_resultMetadata =
                    new DefaultLabelingMetadata(new DefaultCalibratedSpace(m_maxDims.length), new DefaultNamed(
                            "Unknown"), new DefaultSourced("Unknown"), new DefaultLabelingColorTable());
            m_resAccess = m_resultLab.randomAccess();
            m_labelList = new HashSet<String>();
            m_labelGenerator.reset();
        }

        if (m_labelList != null) {
            for (String label : m_labelList) {
                ColorHandler colorHandler =
                        getGlobalSettings().getOriginalColumnSpec(m_smLabelCol.getStringValue()).getColorHandler();
                if (colorHandler != null) {
                    m_resultMetadata.getLabelingColorTable().setColor(label,
                                                                      colorHandler.getColorAttr(new StringCell(label))
                                                                              .getColor().getRGB());
                }
            }
        }

        if ((m_intervalCol.length() == 0) || (m_smAvoidOverlapCol.getStringValue().length() != 0)) {
            for (final Triple<ImgPlusValue<BitType>, String, Double> p : m_bitMaskList) {
                if (p != null) {
                    addToLabeling(p.getFirst(), p.getSecond());
                }
            }
        }

        // return the already composed result
        try {
            return getLabelingCellFactory().createCell(m_resultLab, m_resultMetadata);
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
        panel.add(m_dcLabelCol.getComponentPanel());
        panel.add(m_dcLabelingType.getComponentPanel());
        panel.add(m_dcLabelingFactory.getComponentPanel());
        panel.add(m_dcAvoidOverlapCol.getComponentPanel());
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
        m_dcLabelingFactory.loadSettingsFrom(settings, new DataTableSpec[]{spec});
        m_dcLabelingType.loadSettingsFrom(settings, new DataTableSpec[]{spec});
        m_dcLabelCol.loadSettingsFrom(settings, new DataTableSpec[]{spec});
        m_dcAvoidOverlapCol.loadSettingsFrom(settings, new DataTableSpec[]{spec});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_smIntervalCol.loadSettingsFrom(settings);
        m_smLabelCol.loadSettingsFrom(settings);
        if (m_smIntervalCol.getStringValue() == null) {
            m_smIntervalCol.setStringValue("");
        }
        if (m_smLabelCol.getStringValue() == null) {
            m_smLabelCol.setStringValue("");
        }
        m_smLabelingFactory.loadSettingsFrom(settings);
        m_smLabelingType.loadSettingsFrom(settings);
        try {
            m_smAvoidOverlapCol.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            // since knip 1.0.3
            m_smAvoidOverlapCol.setStringValue("");
        }
        if (m_smAvoidOverlapCol.getStringValue() == null) {
            m_smAvoidOverlapCol.setStringValue("");
        }
    }

    private boolean overlap(final ImgPlusValue<BitType> val1, final ImgPlusValue<BitType> val2) {
        final Img<BitType> img1 = val1.getImgPlus();
        final Img<BitType> img2 = val2.getImgPlus();

        final long[] min1 = val1.getMinimum();
        final long[] min2 = val2.getMinimum();

        final IntervalView<BitType> iv1 = Views.translate(img1, min1);
        final IntervalView<BitType> iv2 = Views.translate(img2, min2);

        final Interval intersect = Intervals.intersect(iv1, iv2);
        for (int i = 0; i < intersect.numDimensions(); i++) {
            if (intersect.dimension(i) <= 0) {
                return false;
            }
        }
        final RandomAccess<BitType> ra1 = iv1.randomAccess();
        final RandomAccess<BitType> ra2 = iv2.randomAccess();

        final IntervalIterator ii = new IntervalIterator(intersect);
        while (ii.hasNext()) {
            ii.fwd();
            ra1.setPosition(ii);
            if (ra1.get().get()) {
                ra2.setPosition(ii);
                if (ra2.get().get()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        // labeling creation fields
        m_resultLab = null;
        m_resultMetadata = null;
        m_resAccess = null;

        // "lazy" labeling creation
        if (m_bitMaskList != null) {
            m_bitMaskList.clear();
        }
        m_maxDims = null;

        // default settings
        m_intervalCol = "";
        m_labelCol = "";
        m_labelGenerator.reset();

    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_dcIntervalCol != null) {
            try {
                m_dcIntervalCol.saveSettingsTo(settings);
                m_dcLabelingFactory.saveSettingsTo(settings);
                m_dcLabelingType.saveSettingsTo(settings);
                m_dcLabelCol.saveSettingsTo(settings);
                m_dcAvoidOverlapCol.saveSettingsTo(settings);
            } catch (final InvalidSettingsException e) {
                throw new RuntimeException(e.getMessage());
            }
        } else {
            m_smIntervalCol.saveSettingsTo(settings);
            m_smLabelingFactory.saveSettingsTo(settings);
            m_smLabelingType.saveSettingsTo(settings);
            m_smLabelCol.saveSettingsTo(settings);
            m_smAvoidOverlapCol.saveSettingsTo(settings);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_smIntervalCol.validateSettings(settings);
        m_smLabelingFactory.validateSettings(settings);
        m_smLabelingType.validateSettings(settings);
        m_smLabelCol.validateSettings(settings);
        try {
            m_smAvoidOverlapCol.validateSettings(settings);
        } catch (final InvalidSettingsException e) {
        }
    }

}
