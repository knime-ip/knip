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
 * ---------------------------------------------------------------------
 *
 * Created on 13.11.2013 by Christian Dietz
 */
package org.knime.knip.base.node;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.ImgOperations;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.core.util.ImgUtils;

/**
 * Remark: Note this class has some redundant implementations to {@link ImgPlusToImgPlusNodeModel}. Anyway, the
 * functionality provided by this class differs a lot.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 *
 * @param <T> Type of Input
 * @param <V> Type of Output
 * @param <L> Type of Labeling
 *
 */
public abstract class IterableIntervalsNodeModel<T extends RealType<T>, V extends RealType<V>, L extends Comparable<L>>
        extends ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<V>> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(IterableIntervalsNodeModel.class);

    /**
     * Filling Mode. How areas outside the rois are treated
     *
     * @author Christian Dietz
     */
    protected enum FillingMode {
        /**
         * fill with the value of the source img
         */
        SOURCE("Value of Source"),

        /**
         * fill with the minimum of the resulting type
         */
        RESMIN("Minimum of Result Type"),

        /**
         * fill with the maximum of the resulting type
         */
        RESMAX("Maximum of Result Type"),

        /**
         * Keep as is (in most cases it's default zero)
         */
        NOFILLING("No Filling");

        private String name;

        private FillingMode(@SuppressWarnings("hiding") final String name) {
            this.name = name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Create the {@link SettingsModelDimSelection}
     *
     * @param axes
     * @return {@link SettingsModelDimSelection}
     */
    protected static SettingsModelDimSelection createDimSelectionModel(final String... axes) {
        return new SettingsModelDimSelection("dim_selection", axes);
    }

    /**
     * Create the optional column model (here: labeling)
     *
     * @return {@link SettingsModelString}
     */
    protected static SettingsModelString createOptionalColumnModel() {
        return new SettingsModelString("optinal_column_selection", "");
    }

    /**
     * Create model to store whether pixels outside the ROIs should be filled with the values of the source img
     *
     * @return {@link SettingsModelBoolean}
     */
    protected static SettingsModelString createFillingModeModel() {
        return new SettingsModelString("fill_non_roi_pixels", FillingMode.NOFILLING.toString());
    }

    /*
     * Stores the first selected column.
     */
    private final SettingsModelString m_optionalColumnModel = createOptionalColumnModel();

    /*
     * Store the settings model for outside roi pixel filling
     */
    private final SettingsModelString m_fillNonROIPixels = createFillingModeModel();

    /*
     * The optional column (here: labeling)
     */
    private int m_optionalColIdx;

    /*
     * Labeling of the current {@link DataRow}. Can be null if no column with labeling is selected by the user.
     */
    private Labeling<L> m_currentLabeling;

    /**
     * Stores the dimension selection. Can be null.
     */
    protected SettingsModelDimSelection m_dimSelectionModel = null;

    /**
     * Factory to create cells
     */
    protected ImgPlusCellFactory m_cellFactory;

    // indicator whether dimension selection should be added
    private boolean m_hasDimSelection;

    /**
     * Convienience constructor. If you want to avoid dimension selection, you can do so using the other constructor.
     */
    public IterableIntervalsNodeModel() {
        this(true);
    }

    /**
     * @param hasDimSelection set false if you don't want to use dimension selection
     */
    public IterableIntervalsNodeModel(final boolean hasDimSelection) {
        this.m_hasDimSelection = hasDimSelection;
        if (hasDimSelection) {
            m_dimSelectionModel = createDimSelectionModel("X", "Y");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        m_optionalColIdx = getOptionalColumnIdx(((BufferedDataTable)inObjects[0]).getDataTableSpec());
        return super.execute(inObjects, exec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        // for consistency with the GUI we need to disable our dim selection if a labeling is set (which absolutely makes sense)
        // if there exists a labeling, we need to decide what we do with the pixels outside the ROIs
        int idx = getOptionalColumnIdx((DataTableSpec)inSpecs[0]);

        if (idx == -1) {
            if (m_hasDimSelection) {
                m_dimSelectionModel.setEnabled(true);
            }
            m_fillNonROIPixels.setEnabled(false);
        } else {
            if (m_hasDimSelection) {
                m_dimSelectionModel.setEnabled(false);
            }
            m_fillNonROIPixels.setEnabled(true);
        }

        return super.configure(inSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void computeDataRow(final DataRow row) {
        if (m_optionalColIdx != -1 && !row.getCell(m_optionalColIdx).isMissing()) {
            m_currentLabeling = ((LabelingValue<L>)row.getCell(m_optionalColIdx)).getLabeling();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void prepareExecute(final ExecutionContext exec) {
        m_cellFactory = new ImgPlusCellFactory(exec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void collectSettingsModels() {
        super.collectSettingsModels();
        m_settingsModels.add(m_optionalColumnModel);

        if (m_hasDimSelection) {
            m_settingsModels.add(m_dimSelectionModel);
        }
        m_settingsModels.add(m_fillNonROIPixels);
    }

    /**
     * Get the optional column index.
     *
     * @param inSpec spec of the table
     * @return returns -1 if no column is selected.
     *
     * @throws InvalidSettingsException
     */
    protected int getOptionalColumnIdx(final DataTableSpec inSpec) throws InvalidSettingsException {

        int optionalColIdx = -1;
        if (m_optionalColumnModel.getStringValue() != null
                && !m_optionalColumnModel.getStringValue().equalsIgnoreCase("")) {
            optionalColIdx =
                    NodeUtils.autoColumnSelection(inSpec, m_optionalColumnModel, LabelingValue.class, this.getClass());
        }

        return optionalColIdx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImgPlusCell<V> compute(final ImgPlusValue<T> cellValue) throws Exception {

        ImgPlus<T> in = cellValue.getImgPlus();
        ImgPlus<V> res = createResultImage(cellValue.getImgPlus());

        if (m_hasDimSelection && !m_dimSelectionModel.isContainedIn(cellValue.getMetadata())) {
            LOGGER.warn("image " + cellValue.getMetadata().getName() + " does not provide all selected dimensions.");
        }

        V outType = getOutType(in.firstElement());

        int[] selectedDimIndices = null;
        if (m_hasDimSelection) {
            selectedDimIndices = m_dimSelectionModel.getSelectedDimIndices(in);
        } else {
            // set all as selected
            selectedDimIndices = new int[in.numDimensions()];
            for (int i = 0; i < selectedDimIndices.length; i++) {
                selectedDimIndices[i] = i;
            }
        }

        prepareOperation(in.firstElement());
        if (!isLabelingPresent()) {
            UnaryOperation<IterableInterval<T>, IterableInterval<V>> operation = operation();
            SubsetOperations.iterate(ImgOperations.wrapII(operation, outType), selectedDimIndices, in, res,
                                     getExecutorService());
        } else {
            for (L label : m_currentLabeling.getLabels()) {
                IterableRegionOfInterest iterableRegionOfInterest =
                        m_currentLabeling.getIterableRegionOfInterest(label);

                IterableInterval<T> inII = iterableRegionOfInterest.getIterableIntervalOverROI(in);
                IterableInterval<V> outII = iterableRegionOfInterest.getIterableIntervalOverROI(res);

                UnaryOperation<IterableInterval<T>, IterableInterval<V>> operation = operation();

                // TODO parallelize over ROIs (tbd)
                operation.compute(inII, outII);

            }

            // TODO can we speed this up (or is it even slower if we would have empty pixel information?)
            FillingMode mode = EnumUtils.valueForName(m_fillNonROIPixels.getStringValue(), FillingMode.values());

            switch (mode) {
                case NOFILLING:
                    break;
                case RESMIN:
                    fill(Views.flatIterable(res), outType.getMinValue());
                    break;
                case RESMAX:
                    fill(Views.flatIterable(res), outType.getMaxValue());
                    break;
                case SOURCE:
                    // here we need to do something special
                    Cursor<LabelingType<L>> cursor = Views.flatIterable(m_currentLabeling).cursor();
                    Cursor<V> outCursor = Views.flatIterable(res).cursor();
                    Cursor<T> inCursor = Views.flatIterable(in).cursor();
                    while (cursor.hasNext()) {
                        if (cursor.next().getLabeling().isEmpty()) {
                            outCursor.next().setReal(inCursor.next().getRealDouble());
                        } else {
                            outCursor.fwd();
                            inCursor.fwd();
                        }
                    }
                    break;
            }
        }
        return m_cellFactory.createCell(res, cellValue.getMinimum());
    }

    // fills the res with val if labeling contains no labels at a certain position
    private void fill(final IterableInterval<V> res, final double val) {
        Cursor<LabelingType<L>> cursor = Views.flatIterable(m_currentLabeling).cursor();
        Cursor<V> outCursor = res.cursor();
        while (cursor.hasNext()) {
            if (cursor.next().getLabeling().isEmpty()) {
                outCursor.next().setReal(val);
            } else {
                outCursor.fwd();
            }
        }
    }

    /**
     * This method can be overriden. If you do so, make sure, that you also copy the metadata of the incoming
     * {@link ImgPlus} into the resulting {@link ImgPlus}.
     *
     * This methods assumes that the incoming {@link ImgPlus} has the same dimensionality as the outgoing
     * {@link ImgPlus}
     *
     * @return
     */
    private ImgPlus<V> createResultImage(final ImgPlus<T> in) {
        return new ImgPlus<V>(ImgUtils.createEmptyCopy(in, getOutType(in.firstElement())), in);
    }

    /**
     * @return true if labeling is selected by user
     */
    protected boolean isLabelingPresent() {
        return m_optionalColIdx > -1;
    }

    /**
     * Type of the output. Must be of type {@link RealType}
     *
     * @param inType the type of the incoming {@link IterableInterval}
     *
     * @return the type of the output
     */
    protected abstract V getOutType(final T inType);

    /**
     * The {@link UnaryOperation} which will be used to compute the output {@link IterableInterval} of type V given the
     * input {@link IterableInterval} of type T
     *
     * @return the {@link UnaryOperation} which will be utilized for processing
     *
     */
    public abstract UnaryOperation<IterableInterval<T>, IterableInterval<V>> operation();

    /**
     * @param inputType of input {@link IterableInterval}
     */
    public abstract void prepareOperation(final T inputType);
}