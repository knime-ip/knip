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
package org.knime.knip.base.nodes.seg.contourdetectwithseeds;

import static org.knime.knip.base.nodes.seg.contourdetectwithseeds.ContourDetectorNodeDialogPane.createAngularDimension;
import static org.knime.knip.base.nodes.seg.contourdetectwithseeds.ContourDetectorNodeDialogPane.createCalculateGradient;
import static org.knime.knip.base.nodes.seg.contourdetectwithseeds.ContourDetectorNodeDialogPane.createGradientDirection;
import static org.knime.knip.base.nodes.seg.contourdetectwithseeds.ContourDetectorNodeDialogPane.createImgColumn;
import static org.knime.knip.base.nodes.seg.contourdetectwithseeds.ContourDetectorNodeDialogPane.createImgDimesions;
import static org.knime.knip.base.nodes.seg.contourdetectwithseeds.ContourDetectorNodeDialogPane.createLeftFilterSelection;
import static org.knime.knip.base.nodes.seg.contourdetectwithseeds.ContourDetectorNodeDialogPane.createLineVariance;
import static org.knime.knip.base.nodes.seg.contourdetectwithseeds.ContourDetectorNodeDialogPane.createMinArea;
import static org.knime.knip.base.nodes.seg.contourdetectwithseeds.ContourDetectorNodeDialogPane.createMinScore;
import static org.knime.knip.base.nodes.seg.contourdetectwithseeds.ContourDetectorNodeDialogPane.createNumAngles;
import static org.knime.knip.base.nodes.seg.contourdetectwithseeds.ContourDetectorNodeDialogPane.createOutOfBoundsSelectionModel;
import static org.knime.knip.base.nodes.seg.contourdetectwithseeds.ContourDetectorNodeDialogPane.createOverlap;
import static org.knime.knip.base.nodes.seg.contourdetectwithseeds.ContourDetectorNodeDialogPane.createPartialProjection;
import static org.knime.knip.base.nodes.seg.contourdetectwithseeds.ContourDetectorNodeDialogPane.createRadius;
import static org.knime.knip.base.nodes.seg.contourdetectwithseeds.ContourDetectorNodeDialogPane.createSeedColumn;
import static org.knime.knip.base.nodes.seg.contourdetectwithseeds.ContourDetectorNodeDialogPane.createSmoothGradient;
import static org.knime.knip.base.nodes.seg.contourdetectwithseeds.ContourDetectorNodeDialogPane.createUseAngularDimension;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.labeling.Labeling;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.iterableinterval.unary.Centroid;
import net.imglib2.ops.operation.subset.views.LabelingView;
import net.imglib2.sampler.special.ConstantRandomAccessible;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.KNIPConstants;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeTools;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelFilterSelection;
import org.knime.knip.core.algorithm.PolarImageFactory;
import org.knime.knip.core.data.algebra.ExtendedPolygon;
import org.knime.knip.core.data.algebra.Vector;
import org.knime.knip.core.ops.filters.DirectionalGradient;
import org.knime.knip.core.ops.filters.DirectionalGradient.GradientDirection;
import org.knime.knip.core.ops.labeling.ContourDetector;
import org.knime.knip.core.ops.labeling.PartialProjectionNodeTools;
import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
@Deprecated
public class ContourDetectorNodeModel<T extends RealType<T>, L extends Comparable<L>> extends NodeModel {

    public static String[] GRADIENT_DIRECTIONS = {"Decreasing", "Increasing"};

    private int m_imgColIdx = -1;

    // Basic Settings
    private final SettingsModelString m_outOfBoundsSelection = createOutOfBoundsSelectionModel();

    private DataTableSpec m_outSpec;

    private int m_seedColIdx = 0;

    private final SettingsModelDimSelection m_smAngularDim = createAngularDimension();

    private final SettingsModelBoolean m_smAngularPartProject = createPartialProjection();

    // Gradient
    private final SettingsModelBoolean m_smCalcGradient = createCalculateGradient();

    private final SettingsModelString m_smGradientDirection = createGradientDirection();

    // Image
    private final SettingsModelString m_smImgColumn = createImgColumn();

    private final SettingsModelDimSelection m_smImgDimensions = createImgDimesions();

    @SuppressWarnings("rawtypes")
    private final SettingsModelFilterSelection m_smLeftFilter = createLeftFilterSelection();

    private final SettingsModelInteger m_smLineVariance = createLineVariance();

    private final SettingsModelInteger m_smMinArea = createMinArea();

    private final SettingsModelDoubleBounded m_smMinScore = createMinScore();

    private final SettingsModelInteger m_smNumAngles = createNumAngles();

    private final SettingsModelDoubleBounded m_smOverlap = createOverlap();

    private final SettingsModelInteger m_smRadius = createRadius();

    private final SettingsModelString m_smSeedColumn = createSeedColumn();

    private final SettingsModelBoolean m_smSmoothGradient = createSmoothGradient();

    // Angular
    private final SettingsModelBoolean m_smUseAngular = createUseAngularDimension();

    protected ContourDetectorNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        m_imgColIdx = inSpecs[0].findColumnIndex(m_smImgColumn.getStringValue());
        if (m_imgColIdx == -1) {
            if ((m_imgColIdx = NodeTools.autoOptionalColumnSelection(inSpecs[0], m_smImgColumn, ImgPlusValue.class)) >= 0) {
                setWarningMessage("Auto-configure Column: " + m_smImgColumn.getStringValue());
            } else {
                throw new InvalidSettingsException("No img column selected!");
            }
        }

        m_seedColIdx = inSpecs[0].findColumnIndex(m_smSeedColumn.getStringValue());
        if (m_seedColIdx == -1) {
            if ((m_seedColIdx = NodeTools.autoOptionalColumnSelection(inSpecs[0], m_smSeedColumn, ImgPlusValue.class)) >= 0) {
                setWarningMessage("Auto-configure Column: " + m_smSeedColumn.getStringValue());
            } else {
                throw new InvalidSettingsException("No seed column selected!");
            }
        }

        m_outSpec =
                new DataTableSpec(new String[]{"BitMasks", "Seeding Labeling", "Score", "Label"}, new DataType[]{
                        ImgPlusCell.TYPE, LabelingCell.TYPE, DoubleCell.TYPE, StringCell.TYPE});
        return new DataTableSpec[]{m_outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        final ImgPlusCellFactory cellFactory = new ImgPlusCellFactory(exec);

        final RowIterator rowIterator = inData[0].iterator();
        final BufferedDataContainer con = exec.createDataContainer(m_outSpec);

        final int numRows = inData[0].getRowCount();
        int processedRows = 0;

        // Initializing centroid op
        final Centroid centroidOp = new Centroid();

        // Pre-Processing dimension selection
        final Set<String> processPlaneLabels = m_smImgDimensions.getSelectedDimLabels();
        final Set<String> processPlaneAndangularDimLabels =
                new HashSet<String>(m_smImgDimensions.getSelectedDimLabels());

        final SettingsModelDimSelection smAngularDimensions = new SettingsModelDimSelection("fake");

        if (m_smAngularDim.getSelectedDimLabels().size() == 1) {
            processPlaneAndangularDimLabels.addAll(m_smAngularDim.getSelectedDimLabels());
        }

        smAngularDimensions.setDimSelectionValue(processPlaneAndangularDimLabels);

        final RulebasedLabelFilter<L> filter = m_smLeftFilter.getRulebasedFilter();

        int totalLabels = 0;
        while (rowIterator.hasNext()) {

            // Settings execution context
            exec.setProgress((double)processedRows / numRows);

            // First row
            final DataRow currentRow = rowIterator.next();

            // Initializing img and potentially labeling
            ImgPlus<T> img = ((ImgPlusValue<T>)currentRow.getCell(m_imgColIdx)).getImgPlus();

            // Creating seed intervals according to img plane
            // selection (must be 2D)
            final Labeling<L> seeds = ((LabelingValue<L>)currentRow.getCell(m_seedColIdx)).getLabeling();

            final Interval[] resIntervals =
                    m_smImgDimensions.getIntervals((((LabelingValue<L>)currentRow.getCell(m_seedColIdx))
                            .getLabelingMetadata()), seeds);

            // Creating intervals plane selection + angular
            final Interval[] imgWithPotentiallyAnglularDimIntervals = smAngularDimensions.getIntervals(img, img);

            int angularDim = -1;
            if (m_smUseAngular.getBooleanValue()) {
                final int[] tmp = m_smAngularDim.getSelectedDimIndices(img.numDimensions(), img);
                if (tmp.length == 1) {
                    angularDim = tmp[0];
                } else {
                    setWarningMessage("Angular dimension is not regarded!");
                }
            }

            // partial projection if desired and possible
            if (m_smUseAngular.getBooleanValue() && m_smAngularPartProject.getBooleanValue()) {
                img = new ImgPlus<T>(PartialProjectionNodeTools.partialMaximumProjection(img, angularDim, 1), img);
            }
            for (int i = 0; i < imgWithPotentiallyAnglularDimIntervals.length; i++) {

                if (imgWithPotentiallyAnglularDimIntervals.length != resIntervals.length) {
                    throw new IllegalStateException(
                            "there MUST be the same number of process planes in contour detector!");
                }

                // Creating subImg potentially including angular
                // dimension
                final RandomAccessibleInterval<T> iterableSubsetView =
                        SubsetOperations.subsetview(img, imgWithPotentiallyAnglularDimIntervals[i]);
                final T minVal = img.firstElement().createVariable();
                minVal.setReal(minVal.getMinValue());

                PolarImageFactory<T> polarFac;
                if (angularDim == -1) {
                    polarFac =
                            new PolarImageFactory<T>(
                                    Views.extend(iterableSubsetView,
                                                 org.knime.knip.core.types.OutOfBoundsStrategyFactory
                                                         .<T, RandomAccessibleInterval<T>> getStrategy(m_outOfBoundsSelection
                                                                                                               .getStringValue(),
                                                                                                       img.firstElement()))

                            );
                } else {
                    polarFac =
                            new PolarImageFactory<T>(
                                    Views.extend(iterableSubsetView,
                                                 org.knime.knip.core.types.OutOfBoundsStrategyFactory
                                                         .<T, RandomAccessibleInterval<T>> getStrategy(m_outOfBoundsSelection
                                                                                                               .getStringValue(),
                                                                                                       img.firstElement())),

                                    angularDim, img.dimension(angularDim));
                }

                // Create Directional Gradient transformed
                // source img
                DirectionalGradient<T, Img<T>> preProc = null;
                if (m_smCalcGradient.getBooleanValue()) {
                    preProc =
                            new DirectionalGradient<T, Img<T>>(GradientDirection.HORIZONTAL, m_smGradientDirection
                                    .getStringValue().equals(GRADIENT_DIRECTIONS[1]));
                }

                // Initializing seeding points
                Vector[] seedingPoints;
                List<L> usedLabels = null;

                final Labeling<L> subLabeling =
                        new LabelingView<L>(SubsetOperations.subsetview(seeds, resIntervals[i]), seeds.<L> factory());

                if (subLabeling.numDimensions() != 2) {
                    throw new IllegalStateException("Seeds should be two accessed via a 2-dimensional interval");
                }

                final Collection<L> labels = subLabeling.getLabels();

                if (labels.size() == 0) {
                    continue;
                }
                final List<Vector> seedsVector = new ArrayList<Vector>(labels.size());
                usedLabels = new ArrayList<L>();
                for (final L label : subLabeling.getLabels()) {
                    if (!filter.isValid(label)) {
                        continue;
                    }
                    final double[] centroidAsDouble =
                            centroidOp
                                    .compute(subLabeling.getIterableRegionOfInterest(label)
                                                     .getIterableIntervalOverROI(new ConstantRandomAccessible<BitType>(
                                                                                         new BitType(), subLabeling
                                                                                                 .numDimensions())),
                                             new double[subLabeling.numDimensions()]);
                    final long[] centroid = new long[centroidAsDouble.length];
                    int a = 0;
                    for (final double d : centroidAsDouble) {
                        centroid[a++] = Math.round(d);
                    }

                    usedLabels.add(label);
                    seedsVector.add(new Vector(centroid));
                }
                seedingPoints = seedsVector.toArray(new Vector[seedsVector.size()]);

                final ContourDetector<T> cd =
                        new ContourDetector<T>(new PolarImageFactory[]{polarFac}, preProc, m_smRadius.getIntValue(),
                                m_smNumAngles.getIntValue(), seedingPoints, m_smLineVariance.getIntValue(),
                                m_smOverlap.getDoubleValue(), m_smMinScore.getDoubleValue(), m_smMinArea.getIntValue(),
                                m_smSmoothGradient.getBooleanValue());
                cd.detectContours();
                for (int c = 0; c < cd.getNumDetectedContours(); c++) {
                    final ExtendedPolygon polygon = cd.getContour(c);
                    final Img<BitType> bitMask = cd.getContour(c).createBitmask();

                    String rowKey;
                    final Rectangle r = cd.getContour(c).getBounds();

                    // Res intervals must have the same size
                    // as the input img - angular dim or the
                    // seeding points in the seeded case
                    final long[] min = new long[resIntervals[i].numDimensions()];
                    final long[] max = new long[resIntervals[i].numDimensions()];

                    resIntervals[i].min(min);
                    resIntervals[i].max(max);

                    final int[] selected =
                            m_smImgDimensions.getSelectedDimIndices(iterableSubsetView.numDimensions(), img);

                    min[selected[0]] = r.x;
                    min[selected[1]] = r.y;

                    max[selected[0]] = r.x + r.width;
                    max[selected[1]] = r.y + r.height;

                    // minmax got set
                    L label = null;
                    int k = 0;
                    for (final Vector v : seedingPoints) {
                        if (polygon.contains(v.getIntPosition(0), v.getIntPosition(1))) {
                            label = usedLabels.get(k);
                            break;
                        }
                        k++;
                    }
                    if (label == null) {
                        continue;
                    }

                    // Use source label
                    rowKey = currentRow.getKey().getString() + KNIPConstants.IMGID_LABEL_DELIMITER + totalLabels++;

                    final long[] dims = new long[img.numDimensions()];
                    Arrays.fill(dims, 1);
                    dims[selected[0]] = bitMask.dimension(0);
                    dims[selected[1]] = bitMask.dimension(1);
                    final Img<BitType> resBitMask = img.factory().imgFactory(new BitType()).create(dims, new BitType());

                    final Cursor<BitType> resCursor =
                            Views.flatIterable(SubsetOperations.subsetview(resBitMask, new FinalInterval(dims)))
                                    .cursor();
                    final Cursor<BitType> srcCursor = Views.flatIterable(bitMask).cursor();

                    while (resCursor.hasNext()) {
                        resCursor.next().set(srcCursor.next().get());
                    }

                    final long[] resMin = new long[dims.length];
                    resMin[selected[0]] = (long)r.getMinX();
                    resMin[selected[1]] = (long)r.getMinY();

                    con.addRowToTable(new DefaultRow(new RowKey(rowKey), cellFactory.createCell(new ImgPlus<BitType>(
                            resBitMask, img), resMin), currentRow.getCell(m_seedColIdx), new DoubleCell(cd
                            .getContourScore(c)), new StringCell(label.toString())));
                }
                exec.checkCanceled();
                processedRows++;

            }

        }
        con.close();
        processPlaneLabels.removeAll(m_smAngularDim.getSelectedDimLabels());
        m_smImgDimensions.setDimSelectionValue(processPlaneLabels);
        return new BufferedDataTable[]{con.getTable()};
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_smSeedColumn.loadSettingsFrom(settings);
        m_outOfBoundsSelection.loadSettingsFrom(settings);
        m_smImgColumn.loadSettingsFrom(settings);
        m_smRadius.loadSettingsFrom(settings);
        m_smNumAngles.loadSettingsFrom(settings);
        m_smLineVariance.loadSettingsFrom(settings);
        m_smOverlap.loadSettingsFrom(settings);
        m_smMinScore.loadSettingsFrom(settings);
        m_smMinArea.loadSettingsFrom(settings);
        m_smSmoothGradient.loadSettingsFrom(settings);
        m_smImgDimensions.loadSettingsFrom(settings);
        m_smUseAngular.loadSettingsFrom(settings);
        m_smAngularDim.loadSettingsFrom(settings);
        m_smAngularPartProject.loadSettingsFrom(settings);
        m_smCalcGradient.loadSettingsFrom(settings);
        m_smGradientDirection.loadSettingsFrom(settings);
        m_smLeftFilter.loadSettingsFrom(settings);
    }

    /*
     * Fast calculation of the next power of 2.
     */
    private final int nextPow2(int x) {
        --x;
        x |= x >> 1;
        x |= x >> 2;
        x |= x >> 4;
        x |= x >> 8;
        x |= x >> 16;
        return ++x;
    }

    @Override
    protected void reset() {
        //
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        //
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_smSeedColumn.saveSettingsTo(settings);
        m_outOfBoundsSelection.saveSettingsTo(settings);
        m_smImgColumn.saveSettingsTo(settings);
        m_smRadius.saveSettingsTo(settings);
        m_smNumAngles.saveSettingsTo(settings);
        m_smLineVariance.saveSettingsTo(settings);
        m_smOverlap.saveSettingsTo(settings);
        m_smMinScore.saveSettingsTo(settings);
        m_smMinArea.saveSettingsTo(settings);
        m_smSmoothGradient.saveSettingsTo(settings);
        m_smImgDimensions.saveSettingsTo(settings);
        m_smUseAngular.saveSettingsTo(settings);
        m_smAngularDim.saveSettingsTo(settings);
        m_smAngularPartProject.saveSettingsTo(settings);
        m_smCalcGradient.saveSettingsTo(settings);
        m_smGradientDirection.saveSettingsTo(settings);
        m_smLeftFilter.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

        int numAngles = m_smNumAngles.getIntValue();
        if (m_smSmoothGradient.getBooleanValue() && (Integer.highestOneBit(numAngles) != numAngles)) {
            numAngles = nextPow2(numAngles);
            NodeLogger.getLogger(this.getClass()).warn("Number of angles is not a power of 2 and was replaced by "
                                                               + numAngles + ".");
        }
        m_smSeedColumn.validateSettings(settings);
        m_outOfBoundsSelection.validateSettings(settings);
        m_smImgColumn.validateSettings(settings);
        m_smRadius.validateSettings(settings);
        m_smNumAngles.validateSettings(settings);
        m_smLineVariance.validateSettings(settings);
        m_smOverlap.validateSettings(settings);
        m_smMinScore.validateSettings(settings);
        m_smMinArea.validateSettings(settings);
        m_smSmoothGradient.validateSettings(settings);
        m_smImgDimensions.validateSettings(settings);
        m_smUseAngular.validateSettings(settings);
        m_smAngularDim.validateSettings(settings);
        m_smAngularPartProject.validateSettings(settings);
        m_smCalcGradient.validateSettings(settings);
        m_smGradientDirection.validateSettings(settings);
        m_smLeftFilter.validateSettings(settings);
    }
}
