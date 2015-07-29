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

import net.imagej.ImgPlus;
import net.imagej.ImgPlusMetadata;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.img.unary.ImgCopyOperation;
import net.imglib2.ops.operation.iterable.unary.Fill;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.KNIPConstants;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.base.node.nodesettings.SettingsModelFilterSelection;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.data.DefaultNamed;
import org.knime.knip.core.data.DefaultSourced;
import org.knime.knip.core.data.img.DefaultImageMetadata;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.core.ops.misc.LabelingDependency;
import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter;
import org.knime.knip.core.util.MiscViews;

/**
 * Crop BitMasks or parts of images according to a Labeling
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @param <L>
 * @param <T>
 */
public class SegmentCropperNodeModel<L extends Comparable<L>, T extends RealType<T>> extends NodeModel implements
        BufferedDataTableHolder {

    private static enum BACKGROUND {
        MIN, MAX, ZERO, SOURCE;
    }

    static final String[] BACKGROUND_OPTIONS = new String[]{"Min Value of Result", "Max Value of Result", "Zero",
            "Source"};

    /**
     * Helper
     *
     * @return SettingsModel to store img column
     */
    static SettingsModelBoolean createAddOverlappingLabels() {
        return new SettingsModelBoolean("cfg_add_dependendcy", false);
    }

    static SettingsModelBoolean createNotEnforceCompleteOverlapModel(final boolean enabled) {
        SettingsModelBoolean sm = new SettingsModelBoolean("no_complete_overlap", false);
        sm.setEnabled(enabled);
        return sm;
    }

    /**
     * Helper
     *
     * @return SettingsModel to store img column
     */
    static SettingsModelString createImgColumnSelectionModel() {
        return new SettingsModelString("cfg_img_col", "");
    }

    /**
     * @return selected value for the background (parts of a bounding box that do not belong to the label.
     */
    static SettingsModelString createBackgroundSelectionModel() {
        return new SettingsModelString("backgroundOptions", BACKGROUND_OPTIONS[BACKGROUND.MIN.ordinal()]);
    }

    /**
     * Helper
     *
     * @return SettingsModelFilterSelection to store left filter selection
     */
    static <LL extends Comparable<LL>> SettingsModelFilterSelection<LL> createLabelFilterModel() {
        return new SettingsModelFilterSelection<LL>("cfg_label_filter_left");
    }

    /**
     * Helper
     *
     * @return SettingsModelFilterSelection to store right filter selection
     */
    static <LL extends Comparable<LL>> SettingsModelFilterSelection<LL>
            createOverlappingLabelFilterModel(final boolean isEnabled) {
        final SettingsModelFilterSelection<LL> sm = new SettingsModelFilterSelection<LL>("cfg_label_filter_right");
        sm.setEnabled(isEnabled);
        return sm;
    }

    /**
     * Helper
     *
     * @return SettingsModel to store labeling column
     */
    static SettingsModelString createSMLabelingColumnSelection() {
        return new SettingsModelString("cfg_labeling_column", "");
    }

    // SM addDependencies
    private final SettingsModelBoolean m_addOverlappingLabels = createAddOverlappingLabels();

    //if segments have to completely overlap or not
    private final SettingsModelBoolean m_noCompleteOverlap = createNotEnforceCompleteOverlapModel(false);

    /* Resulting BufferedDataTable */
    private BufferedDataTable m_data;

    // SettingsModel to store Img column
    private final SettingsModelString m_imgColumn = createImgColumnSelectionModel();

    // SettingsModel to store Labeling column
    private final SettingsModelString m_labelingColumn = createSMLabelingColumnSelection();

    // SM left filter
    private final SettingsModelFilterSelection<L> m_labelFilter = createLabelFilterModel();

    /* Specification of the resulting table */
    private DataTableSpec m_outSpec;

    // SM right filter
    private final SettingsModelFilterSelection<L> m_overlappingLabelFilter = createOverlappingLabelFilterModel(false);

    //value for the label background
    private final SettingsModelString m_backgroundSelection = createBackgroundSelectionModel();

    /**
     * Constructor SegementCropperNodeModel
     */
    public SegmentCropperNodeModel() {
        super(1, 1);
        m_backgroundSelection.setEnabled(!m_imgColumn.getStringValue().equals(""));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        int labColIndex = inSpecs[0].findColumnIndex(m_labelingColumn.getStringValue());
        if (labColIndex == -1) {
            if ((labColIndex = NodeUtils.autoOptionalColumnSelection(inSpecs[0], m_labelingColumn, LabelingValue.class)) >= 0) {
                setWarningMessage("Auto-configure Label Column: " + m_labelingColumn.getStringValue());
            } else {
                throw new InvalidSettingsException("No column selected!");
            }
        }

        final ArrayList<DataColumnSpec> specs = new ArrayList<DataColumnSpec>();
        specs.add(new DataColumnSpecCreator("CroppedImg", ImgPlusCell.TYPE).createSpec());

        DataColumnSpecCreator colspecCreator;
        int imgColIndex = inSpecs[0].findColumnIndex(m_imgColumn.getStringValue());
        if (imgColIndex != -1) {
            colspecCreator = new DataColumnSpecCreator("Source Image", ImgPlusCell.TYPE);
            colspecCreator.setProperties(new DataColumnProperties(Collections
                    .singletonMap(DataValueRenderer.PROPERTY_PREFERRED_RENDERER, "String")));
            specs.add(colspecCreator.createSpec());
        }

        colspecCreator = new DataColumnSpecCreator("Source Labeling", LabelingCell.TYPE);
        colspecCreator.setProperties(new DataColumnProperties(Collections
                .singletonMap(DataValueRenderer.PROPERTY_PREFERRED_RENDERER, "String")));
        specs.add(colspecCreator.createSpec());
        specs.add(new DataColumnSpecCreator("Label", StringCell.TYPE).createSpec());

        if (m_addOverlappingLabels.getBooleanValue()) {
            specs.add(new DataColumnSpecCreator("DependedLabels", StringCell.TYPE).createSpec());
        }

        m_outSpec = new DataTableSpec(specs.toArray(new DataColumnSpec[specs.size()]));
        return new DataTableSpec[]{m_outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        //retrieve column indices
        int labColIndex = inData[0].getDataTableSpec().findColumnIndex(m_labelingColumn.getStringValue());
        if (labColIndex == -1) {
            if ((labColIndex =
                    NodeUtils.autoOptionalColumnSelection(inData[0].getDataTableSpec(), m_labelingColumn,
                                                          LabelingValue.class)) >= 0) {
                setWarningMessage("Auto-configure Label Column: " + m_labelingColumn.getStringValue());
            } else {
                throw new InvalidSettingsException("No column selected!");
            }
        }

        int imgColIndex = inData[0].getDataTableSpec().findColumnIndex(m_imgColumn.getStringValue());

        final BufferedDataContainer con = exec.createDataContainer(m_outSpec);

        final RulebasedLabelFilter<L> leftFilter = m_labelFilter.getRulebasedFilter();

        final RulebasedLabelFilter<L> rightFilter = m_overlappingLabelFilter.getRulebasedFilter();

        final RowIterator it = inData[0].iterator();
        final int rowCount = inData[0].getRowCount();
        DataRow row;
        int rowIndex = 0;
        final ImgPlusCellFactory imgCellFactory = new ImgPlusCellFactory(exec);
        while (it.hasNext()) {
            row = it.next();

            if (row.getCell(labColIndex).isMissing()) {
                setWarningMessage("Rows with missing cells (labeling) have been skipped!");
                continue;
            }
            final LabelingValue<L> labelingValue = (LabelingValue<L>)row.getCell(labColIndex);

            final LabelingDependency<L> labelingDependency =
                    new LabelingDependency<L>(leftFilter, rightFilter, m_noCompleteOverlap.getBooleanValue());

            // If no img selected, create bitmasks
            ImgPlus<T> img = null;
            if (imgColIndex != -1) {
                if (row.getCell(imgColIndex).isMissing()) {
                    setWarningMessage("Rows with missing cells (image) have been skipped!");
                    continue;
                }
                img = ((ImgPlusValue<T>)row.getCell(imgColIndex)).getImgPlus();
            }

            RandomAccessibleInterval<LabelingType<L>> labeling = labelingValue.getLabeling();
            if (img != null) {
                labeling =
                        MiscViews.synchronizeDimensionality(labelingValue.getLabeling(),
                                                            labelingValue.getLabelingMetadata(), img, img);
            }

            ImgFactory<T> fac = null;
            if (labeling instanceof ImgLabeling && ((ImgLabeling)labeling).getIndexImg() instanceof Img) {
                fac = ((Img)((ImgLabeling)labeling).getIndexImg()).factory();
            } else if (img != null) {
                fac = img.factory();
            } else {
                // if we don't have an image and no labeling where we can derive the factory from we simply create ArrayImg BitMasks
                fac = (ImgFactory<T>)new ArrayImgFactory<BitType>();
            }

            final Map<L, List<L>> dependedLabels = Operations.compute(labelingDependency, labeling);
            final StringBuffer stringBuffer = new StringBuffer();
            final LabelRegions<L> regions = KNIPGateway.regions().regions(labeling);
            for (final L l : regions.getExistingLabels()) {

                if (!leftFilter.isValid(l)) {
                    continue;
                }

                final IterableRegion<BoolType> roi = Regions.iterable(regions.getLabelRegion(l));

                final long[] min = new long[roi.numDimensions()];
                final long[] max = new long[roi.numDimensions()];
                final long[] negativeMin = new long[roi.numDimensions()];
                for (int k = 0; k < max.length; k++) {
                    min[k] = (long)Math.floor(roi.realMin(k));
                    max[k] = (long)Math.ceil(roi.realMax(k));
                    negativeMin[k] = -min[k];
                }

                Img<T> res = null;
                if (img != null) {
                    T type = img.firstElement().createVariable();
                    T minType = type.createVariable();
                    minType.setReal(type.getMinValue());

                    Fill<T> fill = new Fill<T>();
                    if (m_backgroundSelection.getStringValue().equals(BACKGROUND_OPTIONS[BACKGROUND.MIN.ordinal()])) {
                        res = fac.create(new FinalInterval(min, max), type.createVariable());

                        if (minType.getRealDouble() != 0) {
                            fill.compute(minType, res.iterator());
                        }
                        writeInRes(res, img, roi);
                    } else if (m_backgroundSelection.getStringValue().equals(BACKGROUND_OPTIONS[BACKGROUND.MAX
                                                                                     .ordinal()])) {
                        T maxType = type.createVariable();
                        maxType.setReal(type.getMaxValue());
                        res = fac.create(new FinalInterval(min, max), type.createVariable());
                        fill.compute(maxType, res.iterator());
                        writeInRes(res, img, roi);
                    } else if (m_backgroundSelection.getStringValue().equals(BACKGROUND_OPTIONS[BACKGROUND.ZERO
                                                                                     .ordinal()])) {
                        res = fac.create(new FinalInterval(min, max), type.createVariable());
                        writeInRes(res, img, roi);
                    } else {
                        res = fac.create(new FinalInterval(min, max), type.createVariable());
                        new ImgCopyOperation()
                                .compute(new ImgView<T>(Views.zeroMin(Views.interval(img, min, max)), fac),
                                         Views.flatIterable(res));
                    }

                } else {
                    res = (Img<T>)fac.imgFactory(new BitType()).create(new FinalInterval(min, max), new BitType());
                    copy((ImgView<BoolType>)new ImgView<T>(Views.zeroMin((RandomAccessibleInterval<T>)Views
                                 .interval(roi, new FinalInterval(min, max))), fac),
                         (IterableInterval<BitType>)Views.flatIterable(res));
                }

                final List<DataCell> cells = new ArrayList<DataCell>();

                // TODO: What about color tables?
                final LabelingMetadata lmdata = labelingValue.getLabelingMetadata();
                final ImgPlusMetadata metadata =
                        new DefaultImgMetadata(lmdata, new DefaultNamed(l.toString()), new DefaultSourced(
                                lmdata.getSource()), new DefaultImageMetadata());

                ImgPlus<T> resImgPlus = new ImgPlus<>(ImgView.wrap(Views.translate(res, min), res.factory()), metadata);
                resImgPlus.setSource(metadata.getSource());
                cells.add(imgCellFactory.createCell(resImgPlus));
                if (imgColIndex != -1) {
                    cells.add(row.getCell(imgColIndex));
                }
                cells.add(row.getCell(labColIndex));
                cells.add(new StringCell(l.toString()));

                if (m_addOverlappingLabels.getBooleanValue()) {
                    List<L> labels;
                    if ((labels = dependedLabels.get(l)) != null) {
                        stringBuffer.setLength(0);
                        for (final L s : labels) {
                            stringBuffer.append(s.toString());
                            stringBuffer.append(";");
                        }

                        if (stringBuffer.length() > 0) {
                            stringBuffer.deleteCharAt(stringBuffer.length() - 1);
                        }
                        cells.add(new StringCell(stringBuffer.toString()));
                    } else {
                        cells.add(new StringCell(""));
                    }
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
     * @param imgView
     * @param flatIterable
     */
    private void copy(final ImgView<BoolType> imgView, final IterableInterval<BitType> iterable) {
        Cursor<BoolType> inCursor = Views.flatIterable(imgView).cursor();
        Cursor<BitType> outCursor = iterable.cursor();

        while (inCursor.hasNext()) {
            outCursor.next().set(inCursor.next().get());
        }

    }

    /**
     * @param res
     * @param img
     * @param roi
     */
    private void writeInRes(final Img<T> res, final ImgPlus<T> img, final IterableRegion<BoolType> roi) {
        final IterableInterval<T> interval = Regions.sample(roi, img);
        final Cursor<T> roiCursor = interval.cursor();
        final RandomAccess<T> ra = res.randomAccess();
        final long[] pos = new long[img.numDimensions()];
        while (roiCursor.hasNext()) {
            roiCursor.next();
            for (int d = 0; d < pos.length; d++) {
                ra.setPosition(roiCursor.getLongPosition(d) - interval.min(d), d);
            }
            ra.get().setReal(roiCursor.get().getRealDouble());
        }
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
        m_imgColumn.loadSettingsFrom(settings);
        m_labelingColumn.loadSettingsFrom(settings);
        m_labelFilter.loadSettingsFrom(settings);
        m_overlappingLabelFilter.loadSettingsFrom(settings);
        m_addOverlappingLabels.loadSettingsFrom(settings);
        m_backgroundSelection.loadSettingsFrom(settings);
        try {
            m_noCompleteOverlap.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            //new setting with 1.1.2, use default if not existent
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
        m_imgColumn.saveSettingsTo(settings);
        m_labelingColumn.saveSettingsTo(settings);
        m_labelFilter.saveSettingsTo(settings);
        m_overlappingLabelFilter.saveSettingsTo(settings);
        m_addOverlappingLabels.saveSettingsTo(settings);
        m_backgroundSelection.saveSettingsTo(settings);
        m_noCompleteOverlap.saveSettingsTo(settings);
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
        m_imgColumn.validateSettings(settings);
        m_labelingColumn.validateSettings(settings);
        m_labelFilter.validateSettings(settings);
        m_overlappingLabelFilter.validateSettings(settings);
        m_addOverlappingLabels.validateSettings(settings);
        m_backgroundSelection.validateSettings(settings);
        try {
            m_noCompleteOverlap.validateSettings(settings);
        } catch (InvalidSettingsException e) {
            //new setting with 1.1.2, use default if not existent
        }
    }
}
