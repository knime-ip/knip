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
package org.knime.knip.base.nodes.features;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.meta.MetadataUtil;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.subset.views.LabelingView;
import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.sampler.special.ConstantRandomAccessible;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ValuePair;

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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.knip.base.KNIMEKNIPPlugin;
import org.knime.knip.base.KNIPConstants;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeTools;
import org.knime.knip.base.node.nodesettings.SettingsModelFilterSelection;
import org.knime.knip.base.nodes.features.providers.FeatureSetProvider;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.core.ops.misc.LabelingDependency;
import org.knime.knip.core.util.MiscViews;

/**
 * A abstract node model, which allows to calculate arbitrary features on {@link IterableInterval}s. That intervals can
 * be either derived from a single image, from a single segments of a labeling or from segments and the according source
 * image toghether (region of interest).
 * 
 * Subclasses basically only need to specify the type on what the features should be calculated (image, labeling,
 * image_and_labeling) which determines whether to select only an image column, only a labeling column, or an image and
 * a labeling column in the configuration dialog.
 * 
 * The method {@link IntervalFeatureSetNodeModel#getFeatureSetProviders()}, to be overwritten in the subclasses,
 * determines the available features sets (which provide additional dialog components to configure the features, and
 * actually calculates the feature values).
 * 
 * 
 * 
 * 
 * @param <L>
 * @param <T>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class IntervalFeatureSetNodeModel<L extends Comparable<L>, T extends RealType<T>> extends NodeModel {

    public enum FeatureType {
        IMAGE, IMAGE_AND_LABELING, LABELING;
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(IntervalFeatureSetNodeModel.class);

    static SettingsModelStringArray createActiveFeatureSetModel() {
        return new SettingsModelStringArray("active_feature_sets", new String[0]);
    }

    static SettingsModelBoolean createAppendDependenciesModel() {
        return new SettingsModelBoolean("appendDependencies", false);
    }

    static SettingsModelBoolean createAppendSegmentInfoModel() {
        return new SettingsModelBoolean("append_segment_information", true);
    }

    static SettingsModelString createImgColumnModel() {
        return new SettingsModelString("img_column_selection", "");
    }

    static SettingsModelBoolean createIntersectionModeModel() {
        return new SettingsModelBoolean("mode", false);
    }

    static SettingsModelString createLabColumnModel() {
        return new SettingsModelString("lab_column_selection", "");
    }

    static <L extends Comparable<L>> SettingsModelFilterSelection<L> createLeftFilterSelectionModel() {
        return new SettingsModelFilterSelection<L>("filter_left");
    }

    static <L extends Comparable<L>> SettingsModelFilterSelection<L> createRightFilterSelectionModel() {
        return new SettingsModelFilterSelection<L>("filter_right");
    }

    private final SettingsModelStringArray m_activeFeatureSets = createActiveFeatureSetModel();

    private final SettingsModelBoolean m_appendDependencies = createAppendDependenciesModel();

    private final SettingsModelBoolean m_appendSegmentInformation = createAppendSegmentInfoModel();

    private final FeatureSetProvider<ValuePair<IterableInterval<T>, CalibratedSpace>>[] m_featSetProviders;

    private final SettingsModelString m_imgColumn = createImgColumnModel();

    private final SettingsModelBoolean m_intersectionMode = createIntersectionModeModel();

    private final SettingsModelString m_labColumn = createLabColumnModel();

    private final SettingsModelFilterSelection<L> m_leftFilterSelection = createLeftFilterSelectionModel();

    private final SettingsModelFilterSelection<L> m_rightFilterSelection = createRightFilterSelectionModel();

    private final List<SettingsModel> m_settingsModels;

    private final FeatureType m_type;

    /**
     * @param type the feature type, i.e. which objects are needed to calculate the features: there are either a single
     *            image, image AND labeling or only a labeling
     * 
     */
    protected IntervalFeatureSetNodeModel(final FeatureType type,
                                          final FeatureSetProvider<ValuePair<IterableInterval<T>, CalibratedSpace>>[] fsetProviders) {
        super(1, 1);
        m_type = type;
        m_featSetProviders = fsetProviders.clone();
        m_settingsModels = new ArrayList<SettingsModel>();

        for (final FeatureSetProvider<ValuePair<IterableInterval<T>, CalibratedSpace>> featFacWrapper : m_featSetProviders) {
            featFacWrapper.initAndAddSettingsModels(m_settingsModels);
        }

        m_settingsModels.add(m_activeFeatureSets);
        if ((m_type == FeatureType.IMAGE) || (m_type == FeatureType.IMAGE_AND_LABELING)) {
            m_settingsModels.add(m_imgColumn);
        }
        if ((m_type == FeatureType.LABELING) || (m_type == FeatureType.IMAGE_AND_LABELING)) {
            m_settingsModels.add(m_labColumn);
            m_settingsModels.add(m_intersectionMode);
            m_settingsModels.add(m_leftFilterSelection);
            m_settingsModels.add(m_rightFilterSelection);
            m_settingsModels.add(m_appendDependencies);
            m_settingsModels.add(m_appendSegmentInformation);
        }

        m_settingsModels.add(m_activeFeatureSets);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        int labColIdx = getLabColIdx(inSpecs[0]);
        getImgColIdx(inSpecs[0]);
        return new DataTableSpec[]{createOutSpec(inSpecs[0], labColIdx)};
    }

    /*
     * Helper to create a binary mask from a region of interest.
     */
    private Img<BitType> createBinaryMask(final IterableInterval<BitType> ii) {
        final Img<BitType> mask = new ArrayImgFactory<BitType>().create(ii, new BitType());

        final RandomAccess<BitType> maskRA = mask.randomAccess();
        final Cursor<BitType> cur = ii.localizingCursor();
        while (cur.hasNext()) {
            cur.fwd();
            for (int d = 0; d < cur.numDimensions(); d++) {
                maskRA.setPosition(cur.getLongPosition(d) - ii.min(d), d);
            }
            maskRA.get().set(true);
        }

        return mask;

    }

    @SuppressWarnings("unchecked")
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        int imgColIdx = getImgColIdx(inData[0].getDataTableSpec());
        int labColIdx = getLabColIdx(inData[0].getDataTableSpec());

        final BufferedDataContainer con =
                exec.createDataContainer(createOutSpec(inData[0].getDataTableSpec(), labColIdx));
        final RowIterator it = inData[0].iterator();
        DataRow row;

        final double count = inData[0].getRowCount();
        double i = 0;
        final ImgPlusCellFactory cellFactory = new ImgPlusCellFactory(exec);

        while (it.hasNext()) {
            row = it.next();

            ImgPlus<T> img = null;
            Labeling<L> labeling = null;
            LabelingMetadata labelingMetadata = null;

            DataCell imgCell = null;
            DataCell labelCell = null;

            // test for missing cells and init img, labeling,
            // labeldingMetadata according to the type field
            boolean skip = false;
            if (m_type == FeatureType.IMAGE) {
                // getImg
                imgCell = row.getCell(imgColIdx);
                if (imgCell.isMissing()) {
                    skip = true;
                }
            } else if (m_type == FeatureType.LABELING) {
                // getLabeling
                labelCell = row.getCell(labColIdx);
                if (labelCell.isMissing()) {
                    skip = true;
                }

            } else {
                imgCell = row.getCell(imgColIdx);
                labelCell = row.getCell(labColIdx);
                if (labelCell.isMissing() || imgCell.isMissing()) {
                    skip = true;
                }
            }

            // stop if missing cell error
            if (skip) {
                LOGGER.warn("Missing cell was ignored at row " + row.getKey());
                continue;
            }

            final List<DataCell> cells = new ArrayList<DataCell>();
            if (m_type == FeatureType.IMAGE) {
                img = ((ImgPlusValue<T>)imgCell).getImgPlus();
                for (final FeatureSetProvider<ValuePair<IterableInterval<T>, CalibratedSpace>> featFacWrapper : m_featSetProviders) {
                    if (isFeatureSetActive(featFacWrapper.getFeatureSetName())) {
                        featFacWrapper
                                .calcAndAddFeatures(new ValuePair<IterableInterval<T>, CalibratedSpace>(img, img),
                                                    cells);
                    }
                }
                exec.setProgress(i++ / count, "Calculated features for row " + row.getKey().toString());
                exec.checkCanceled();
                con.addRowToTable(new DefaultRow(row.getKey(), cells.toArray(new DataCell[cells.size()])));
                continue;
            } else if (m_type == FeatureType.LABELING) {
                labeling = ((LabelingValue<L>)labelCell).getLabeling();
            } else {
                // check dimensionality!
                // Definition: Img is NEVER virtually extended.
                // Just the labeling is!
                final long[] imgDims = ((ImgPlusValue<T>)imgCell).getDimensions();
                final long[] labDims = ((LabelingValue<L>)labelCell).getDimensions();

                img = ((ImgPlusValue<T>)imgCell).getImgPlus();

                labeling = ((LabelingValue<L>)labelCell).getLabeling();

                labelingMetadata = ((LabelingValue<L>)labelCell).getLabelingMetadata();

                if (!Arrays.equals(imgDims, labDims)) {
                    LOGGER.warn("The dimensions of Labeling and Image in Row " + row.getKey()
                            + " are not compatible. Dimensions of labeling are virtually adjusted to match size.");

                    labeling =
                            new LabelingView<L>(MiscViews.synchronizeDimensionality(labeling, labelingMetadata, img,
                                                                                    img), labeling.<L> factory());
                }

            }

            final LabelingDependency<L> dependencyOp =
                    new LabelingDependency<L>(m_leftFilterSelection.getRulebasedFilter(),
                            m_rightFilterSelection.getRulebasedFilter(), m_intersectionMode.getBooleanValue());

            final Map<L, List<L>> dependencies = Operations.compute(dependencyOp, labeling);

            final List<L> labels = labeling.firstElement().getMapping().getLabels();

            IterableRegionOfInterest labelRoi;
            for (final L label : labels) {
                if (!dependencies.keySet().contains(label)) {
                    continue;
                }

                labelRoi = labeling.getIterableRegionOfInterest(label);
                IterableInterval<T> ii;

                // segment image
                final LabelingValue<L> labVal = (LabelingValue<L>)row.getCell(labColIdx);

                ImgPlusMetadata mdata;
                if (img != null) {
                    mdata = new DefaultImgMetadata(((ImgPlusValue<T>)imgCell).getMetadata());

                } else {
                    mdata = new DefaultImgMetadata(labVal.getLabeling().numDimensions());
                    MetadataUtil.copyTypedSpace(labVal.getLabelingMetadata(), mdata);
                    MetadataUtil.copyName(labVal.getLabelingMetadata(), mdata);
                    MetadataUtil.copySource(labVal.getLabelingMetadata(), mdata);
                }

                mdata.setName(label.toString());
                mdata.setSource(labVal.getLabelingMetadata().getName());

                if (img == null) {
                    ii =
                            labelRoi.getIterableIntervalOverROI(new ConstantRandomAccessible<T>((T)new BitType(),
                                    labeling.numDimensions()));
                } else {
                    ii = labelRoi.getIterableIntervalOverROI(img);
                }
                cells.clear();
                if (m_appendSegmentInformation.getBooleanValue()) {

                    final Img<BitType> bitMask =
                            createBinaryMask(labelRoi.getIterableIntervalOverROI(new ConstantRandomAccessible<BitType>(
                                    new BitType(), labeling.numDimensions())));

                    // min
                    final long[] min = new long[ii.numDimensions()];

                    for (int j = 0; j < min.length; j++) {
                        min[j] = ii.min(j);
                    }

                    cells.add(cellFactory.createCell(bitMask, mdata, min));

                    // Segment label
                    cells.add(new StringCell(label.toString()));

                    // source row key
                    // cells.add(new StringCell(row.getKey()
                    // .toString()));
                    cells.add(row.getCell(labColIdx));

                }
                if (m_appendDependencies.getBooleanValue()) {
                    final StringBuffer buf = new StringBuffer();
                    for (final L s : dependencies.get(label)) {
                        buf.append(s.toString());
                        buf.append(";");
                    }

                    if (buf.length() > 0) {
                        buf.deleteCharAt(buf.length() - 1);
                    } else {
                        NodeLogger.getLogger(IntervalFeatureSetNodeModel.class)
                                .warn("No label dependencies found for feature node for label: " + label.toString());
                    }

                    cells.add(new StringCell(buf.toString()));
                }

                for (final FeatureSetProvider<ValuePair<IterableInterval<T>, CalibratedSpace>> featFacWrapper : m_featSetProviders) {
                    if (isFeatureSetActive(featFacWrapper.getFeatureSetName())) {
                        featFacWrapper
                                .calcAndAddFeatures(new ValuePair<IterableInterval<T>, CalibratedSpace>(ii, mdata),
                                                    cells);
                    }
                }

                con.addRowToTable(new DefaultRow(row.getKey() + KNIPConstants.IMGID_LABEL_DELIMITER + label.toString(),
                        cells));

            }
            exec.checkCanceled();
            exec.setProgress(++i / count);
        }
        con.close();
        return new BufferedDataTable[]{con.getTable()};
    }

    private int getImgColIdx(final DataTableSpec inSpec) throws InvalidSettingsException {
        int imgColIndex = -1;
        if ((m_type == FeatureType.IMAGE) || (m_type == FeatureType.IMAGE_AND_LABELING)) {
            imgColIndex = inSpec.findColumnIndex(m_imgColumn.getStringValue());
            if (imgColIndex == -1) {
                if ((imgColIndex = NodeTools.autoOptionalColumnSelection(inSpec, m_imgColumn, ImgPlusValue.class)) >= 0) {
                    setWarningMessage("Auto-configure Image Column: " + m_imgColumn.getStringValue());
                } else {
                    throw new InvalidSettingsException("No column selected!");
                }
            }
        }
        return imgColIndex;
    }

    private int getLabColIdx(final DataTableSpec inSpec) throws InvalidSettingsException {

        int labColIndex = -1;
        if ((m_type == FeatureType.LABELING) || (m_type == FeatureType.IMAGE_AND_LABELING)) {
            labColIndex = inSpec.findColumnIndex(m_labColumn.getStringValue());
            if (labColIndex == -1) {
                if ((labColIndex = NodeTools.autoOptionalColumnSelection(inSpec, m_labColumn, LabelingValue.class)) >= 0) {
                    setWarningMessage("Auto-configure Labeling Column: " + m_labColumn.getStringValue());
                } else {
                    throw new InvalidSettingsException("No column selected!");
                }
            }
        }
        return labColIndex;
    }

    private DataTableSpec createOutSpec(final DataTableSpec inSpec, final int labColIdx) {
        final List<DataColumnSpec> specs = new ArrayList<DataColumnSpec>();
        if ((m_type == FeatureType.LABELING) || (m_type == FeatureType.IMAGE_AND_LABELING)) {
            if (m_appendSegmentInformation.getBooleanValue()) {
                specs.add(new DataColumnSpecCreator("Bitmask", ImgPlusCell.TYPE).createSpec());
                specs.add(new DataColumnSpecCreator("Label", StringCell.TYPE).createSpec());
                // specs.add(new DataColumnSpecCreator("Source",
                // StringCell.TYPE).createSpec());
                final DataColumnSpecCreator colspecCreator =
                        new DataColumnSpecCreator("Source Labeling", inSpec.getColumnSpec(labColIdx).getType());
                colspecCreator.setProperties(new DataColumnProperties(Collections
                        .singletonMap(DataValueRenderer.PROPERTY_PREFERRED_RENDERER, "String")));
                specs.add(colspecCreator.createSpec());
            }

            if (m_appendDependencies.getBooleanValue()) {
                specs.add(new DataColumnSpecCreator("LabelDependencies", StringCell.TYPE).createSpec());
            }
        }
        for (final FeatureSetProvider<ValuePair<IterableInterval<T>, CalibratedSpace>> featFactoryWrapper : m_featSetProviders) {
            if (isFeatureSetActive(featFactoryWrapper.getFeatureSetName())) {
                featFactoryWrapper.initAndAddColumnSpecs(specs);
            }
        }

        return new DataTableSpec(specs.toArray(new DataColumnSpec[specs.size()]));

    }

    /*
     * @param featureSetName
     *
     * @return true, if the specified feature set is marked as active in the
     * dialog
     */
    private boolean isFeatureSetActive(final String featureSetName) {
        for (final String s : m_activeFeatureSets.getStringArrayValue()) {
            if (featureSetName.equals(s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (final SettingsModel sm : m_settingsModels) {
            try {
                sm.loadSettingsFrom(settings);
            } catch (final InvalidSettingsException e) {

                LOGGER.warn("Problems occurred loading the settings " + sm.toString() + ": " + e.getLocalizedMessage());
                setWarningMessage("Problems occurred while loading " + sm.toString() + ".");
            }
            if (sm.toString().equalsIgnoreCase("SettingsModelStringArray ('segment_feature_set')")) {
                final String[] array = ((SettingsModelStringArray)sm).getStringArrayValue();

                final String[] axisLabels = KNIMEKNIPPlugin.parseDimensionLabels();

                // COMPABILITY: Backwards compability
                for (int k = 0; k < array.length; k++) {
                    if (array[k].equalsIgnoreCase("Centroid 0")) {
                        array[k] = "Centroid " + axisLabels[0];
                    } else if (array[k].equalsIgnoreCase("Centroid 1")) {
                        array[k] = "Centroid " + axisLabels[1];
                    } else if (array[k].equalsIgnoreCase("Centroid 2")) {
                        array[k] = "Centroid " + axisLabels[2];
                    } else if (array[k].equalsIgnoreCase("Centroid 3")) {
                        array[k] = "Centroid " + axisLabels[3];
                    } else if (array[k].equalsIgnoreCase("Centroid 4")) {
                        array[k] = "Centroid " + axisLabels[4];
                    }
                }

                ((SettingsModelStringArray)sm).setStringArrayValue(array);

            }

        }
    }

    @Override
    protected void reset() {
        // Nothing to do here
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        for (final SettingsModel sm : m_settingsModels) {
            sm.saveSettingsTo(settings);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (final SettingsModel sm : m_settingsModels) {
            try {
                sm.validateSettings(settings);
            } catch (final InvalidSettingsException e) {

                LOGGER.warn("Problems occurred validating " + sm.toString() + ": " + e.getLocalizedMessage());
                setWarningMessage("Problems occurred while validating settings " + sm.toString() + ".");
            }
        }

    }

}
