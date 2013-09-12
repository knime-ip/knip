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
package org.knime.knip.base.nodes.seg.cropper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.iterable.unary.Fill;
import net.imglib2.ops.operation.subset.views.LabelingView;
import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.sampler.special.ConstantRandomAccessible;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.KNIPConstants;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeTools;
import org.knime.knip.base.node.nodesettings.SettingsModelFilterSelection;
import org.knime.knip.core.data.img.DefaultImageMetadata;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.ops.misc.LabelingDependency;
import org.knime.knip.core.types.ImgFactoryTypes;
import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter;
import org.knime.knip.core.util.MiscViews;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SegmentCropperNodeModel<L extends Comparable<L>, T extends RealType<T>, O extends RealType<O>> extends
        NodeModel implements BufferedDataTableHolder {

    private static enum BACKGROUND {
        MIN, MAX;
    }

    static final String[] BACKGROUND_OPTIONS = new String[]{"min value", "max value"};

    /**
     * Helper
     * 
     * @return SettingsModel to store img column
     */
    public static SettingsModelBoolean createSMAddDependency() {
        return new SettingsModelBoolean("cfg_add_dependendcy", false);
    }

    /**
     * Helper
     * 
     * @return SettingsModel to store factory selection
     */
    public static SettingsModelString createSMFactorySelection() {
        return new SettingsModelString("cfg_factory_selection", "");
    }

    /**
     * Helper
     * 
     * @return SettingsModel to store img column
     */
    public static SettingsModelString createSMImgColumnSelection() {
        return new SettingsModelString("cfg_img_col", "");
    }

    /**
     * @return selected value for the background (parts of a bounding box that do not belong to the label.
     */
    static SettingsModelString createSMBackgroundSelection() {
        return new SettingsModelString("backgroundOptions", BACKGROUND_OPTIONS[BACKGROUND.MIN.ordinal()]);
    }

    /**
     * Helper
     * 
     * @return SettingsModelFilterSelection to store left filter selection
     */
    public static <LL extends Comparable<LL>> SettingsModelFilterSelection<LL> createSMLabelFilterLeft() {
        return new SettingsModelFilterSelection<LL>("cfg_label_filter_left");
    }

    /**
     * Helper
     * 
     * @return SettingsModelFilterSelection to store right filter selection
     */
    public static <LL extends Comparable<LL>> SettingsModelFilterSelection<LL>
            createSMLabelFilterRight(final boolean isEnabled) {
        final SettingsModelFilterSelection<LL> sm = new SettingsModelFilterSelection<LL>("cfg_label_filter_right");
        sm.setEnabled(isEnabled);
        return sm;
    }

    /**
     * Helper
     * 
     * @return SettingsModel to store labeling column
     */
    public static SettingsModelString createSMLabelingColumnSelection() {
        return new SettingsModelString("cfg_labeling_column", "");
    }

    // SM addDependencies
    private final SettingsModelBoolean m_addDependencies = createSMAddDependency();

    /* Resulting BufferedDataTable */
    private BufferedDataTable m_data;

    // SettingsModel to store factory type of resulting img snippets
    private final SettingsModelString m_factorySelection = createSMFactorySelection();

    // Index of img column
    private int m_imgColIndex;

    // SettingsModel to store Img column
    private final SettingsModelString m_imgColumn = createSMImgColumnSelection();

    // Index of labeling column
    private int m_labColIndex;

    // SettingsModel to store Labeling column
    private final SettingsModelString m_labelingColumn = createSMLabelingColumnSelection();

    // SM left filter
    private final SettingsModelFilterSelection<L> m_leftFilter = createSMLabelFilterLeft();

    /* Specification of the resulting table */
    private DataTableSpec m_outSpec;

    // SM right filter
    private final SettingsModelFilterSelection<L> m_rightFilter = createSMLabelFilterRight(false);

    //value for the label background
    private final SettingsModelString m_backgroundSelection = createSMBackgroundSelection();

    // List of all SettingsModels
    private final ArrayList<SettingsModel> m_settings;

    /**
     * Constructor SegementCropperNodeModel
     */
    public SegmentCropperNodeModel() {
        super(1, 1);
        m_settings = new ArrayList<SettingsModel>();
        m_settings.add(m_imgColumn);
        m_settings.add(m_labelingColumn);
        m_settings.add(m_factorySelection);
        m_settings.add(m_leftFilter);
        m_settings.add(m_rightFilter);
        m_settings.add(m_addDependencies);
        m_settings.add(m_backgroundSelection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        m_labColIndex = inSpecs[0].findColumnIndex(m_labelingColumn.getStringValue());
        if (m_labColIndex == -1) {
            if ((m_labColIndex =
                    NodeTools.autoOptionalColumnSelection(inSpecs[0], m_labelingColumn, LabelingValue.class)) >= 0) {
                setWarningMessage("Auto-configure Label Column: " + m_labelingColumn.getStringValue());
            } else {
                throw new InvalidSettingsException("No column selected!");
            }
        }
        m_imgColIndex = inSpecs[0].findColumnIndex(m_imgColumn.getStringValue());

        final ArrayList<DataColumnSpec> specs = new ArrayList<DataColumnSpec>();
        specs.add(new DataColumnSpecCreator("CroppedImg", ImgPlusCell.TYPE).createSpec());
        DataColumnSpecCreator colspecCreator = new DataColumnSpecCreator("Source Image", ImgPlusCell.TYPE);
        colspecCreator.setProperties(new DataColumnProperties(Collections
                .singletonMap(DataValueRenderer.PROPERTY_PREFERRED_RENDERER, "String")));
        specs.add(colspecCreator.createSpec());
        colspecCreator = new DataColumnSpecCreator("Source Labeling", LabelingCell.TYPE);
        colspecCreator.setProperties(new DataColumnProperties(Collections
                .singletonMap(DataValueRenderer.PROPERTY_PREFERRED_RENDERER, "String")));
        specs.add(colspecCreator.createSpec());
        specs.add(new DataColumnSpecCreator("Label", StringCell.TYPE).createSpec());

        if (m_addDependencies.getBooleanValue()) {
            specs.add(new DataColumnSpecCreator("DependedLabels", StringCell.TYPE).createSpec());
        }

        m_outSpec = new DataTableSpec(specs.toArray(new DataColumnSpec[specs.size()]));
        return new DataTableSpec[]{m_outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        final BufferedDataContainer con = exec.createDataContainer(m_outSpec);

        final RulebasedLabelFilter<L> leftFilter = m_leftFilter.getRulebasedFilter();

        final RulebasedLabelFilter<L> rightFilter = m_rightFilter.getRulebasedFilter();

        final ImgFactory<O> fac = ImgFactoryTypes.getImgFactory(m_factorySelection.getStringValue(), null);

        final RowIterator it = inData[0].iterator();
        final int rowCount = inData[0].getRowCount();
        DataRow row;
        int rowIndex = 0;
        final ImgPlusCellFactory imgCellFactory = new ImgPlusCellFactory(exec);
        while (it.hasNext()) {
            row = it.next();

            final LabelingValue<L> labelingValue = (LabelingValue<L>)row.getCell(m_labColIndex);

            final LabelingDependency<L> labelingDependency = new LabelingDependency<L>(leftFilter, rightFilter, false);

            // If no img selected, create bitmasks
            ImgPlus<T> img = null;
            if (m_imgColIndex != -1) {
                img = ((ImgPlusValue<T>)row.getCell(m_imgColIndex)).getImgPlus();
            }

            Labeling<L> labeling = labelingValue.getLabeling();
            if (img != null) {
                labeling =
                        new LabelingView<L>(MiscViews.synchronizeDimensionality(labelingValue.getLabeling(),
                                                                                labelingValue.getLabelingMetadata(),
                                                                                img, img), labeling.<L> factory());
            }

            final Map<L, List<L>> dependedLabels = Operations.compute(labelingDependency, labeling);

            for (final L l : labeling.firstElement().getMapping().getLabels()) {

                if (!leftFilter.isValid(l)) {
                    continue;
                }

                final IterableRegionOfInterest roi = labeling.getIterableRegionOfInterest(l);
                final long[] min = new long[roi.numDimensions()];
                final long[] max = new long[roi.numDimensions()];
                for (int k = 0; k < max.length; k++) {
                    min[k] = (long)Math.floor(roi.realMin(k));
                    max[k] = (long)Math.ceil(roi.realMax(k));
                }
                final FinalInterval interval = new FinalInterval(min, max);

                Img<O> res = null;
                if (img != null) {
                    O type = (O)img.firstElement().createVariable();
                    O minType = type.createVariable();
                    minType.setReal(type.getMinValue());
                    O maxType = type.createVariable();
                    maxType.setReal(type.getMaxValue());

                    res = fac.create(interval, type.createVariable());

                    Cursor<O> roiCursor = (Cursor<O>)roi.getIterableIntervalOverROI(img).cursor();
                    RandomAccess<O> ra = res.randomAccess();

                    Fill<O, IterableInterval<O>> fill = new Fill<O, IterableInterval<O>>();
                    if (m_backgroundSelection.getStringValue().equals(BACKGROUND_OPTIONS[BACKGROUND.MIN.ordinal()])) {
                        fill.compute(minType, res);
                    } else {
                        fill.compute(maxType, res);
                    }

                    long[] pos = new long[img.numDimensions()];

                    while (roiCursor.hasNext()) {
                        roiCursor.next();
                        for (int d = 0; d < pos.length; d++) {
                            ra.setPosition(roiCursor.getLongPosition(d) - interval.min(d), d);
                        }

                        ra.get().setReal(roiCursor.get().getRealDouble());
                    }
                } else {
                    res = fac.create(interval, (O)new BitType());

                    final RandomAccess<BitType> maskRA = (RandomAccess<BitType>)res.randomAccess();

                    final Cursor<BitType> cur =
                            roi.getIterableIntervalOverROI(new ConstantRandomAccessible<BitType>(new BitType(), res
                                                                   .numDimensions())).localizingCursor();

                    while (cur.hasNext()) {
                        cur.fwd();
                        for (int d = 0; d < 2; d++) {
                            maskRA.setPosition(cur.getLongPosition(d) - interval.min(d), d);
                        }
                        maskRA.get().set(true);
                    }

                }

                final List<DataCell> cells = new ArrayList<DataCell>();

                // TODO: What about color tables?
                final DefaultImgMetadata metadata =
                        new DefaultImgMetadata(labelingValue.getLabelingMetadata(),
                                labelingValue.getLabelingMetadata(), labelingValue.getLabelingMetadata(),
                                new DefaultImageMetadata());

                cells.add(imgCellFactory.createCell(res, metadata, min));
                if (m_imgColIndex != -1) {
                    cells.add(row.getCell(m_imgColIndex));
                } else {
                    cells.add(DataType.getMissingCell());
                }
                cells.add(row.getCell(m_labColIndex));
                cells.add(new StringCell(l.toString()));

                if (m_addDependencies.getBooleanValue()) {
                    cells.add(new StringCell(dependedLabels.get(l).toString()));
                }

                con.addRowToTable(new DefaultRow(row.getKey().toString() + KNIPConstants.IMGID_LABEL_DELIMITER
                        + l.toString(), cells.toArray(new DataCell[cells.size()])));
            }

            exec.checkCanceled();
            exec.setProgress((double)++rowIndex / rowCount);
        }
        con.close();
        m_data = con.getTable();
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
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (final SettingsModel sm : m_settings) {
            sm.loadSettingsFrom(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) {
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
        for (final SettingsModel sm : m_settings) {
            sm.validateSettings(settings);
        }
    }

}
