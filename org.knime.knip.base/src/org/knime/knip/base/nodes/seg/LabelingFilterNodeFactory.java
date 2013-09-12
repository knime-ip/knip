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
package org.knime.knip.base.nodes.seg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import net.imglib2.IterableInterval;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.LabelingType;

import org.knime.base.util.WildcardMatcher;
import org.knime.core.data.DataCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;

/**
 * Node to filter a labeling according to easily calculated characteristics (e.g. segment size, border segments, regular
 * expression, ...)
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LabelingFilterNodeFactory<L extends Comparable<L>> extends ValueToCellNodeFactory<LabelingValue<L>> {

    private static final SettingsModelString createExclPatternModel() {
        return new SettingsModelString("exclude_pattern", "");
    }

    private static final SettingsModelString createInclPatternModel() {
        return new SettingsModelString("include_pattern", "");
    }

    private static final SettingsModelInteger createMaxAreaModel() {
        return new SettingsModelInteger("max_area", Integer.MAX_VALUE);
    }

    private static final SettingsModelInteger createMinAreaModel() {
        return new SettingsModelInteger("min_area", 0);
    }

    private static SettingsModelBoolean createRemoveBorderSegModel() {
        return new SettingsModelBoolean("remove_border_segments", false);
    }

    private static SettingsModelBoolean createContainsNoOverlapsModel() {
        return new SettingsModelBoolean("contains_no_overlapping_segments", false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<LabelingValue<L>> createNodeDialog() {
        return new ValueToCellNodeDialog<LabelingValue<L>>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "Label Name", new DialogComponentString(createExclPatternModel(),
                        "Exclude by label name (with wildcards)"));
                addDialogComponent("Options", "Label Name", new DialogComponentString(createInclPatternModel(),
                        "Include by label name (with wildcards)"));
                addDialogComponent("Options", "Segment Area", new DialogComponentNumber(createMinAreaModel(),
                        "Minimum segment area (pixels)", 1));
                addDialogComponent("Options", "Segment Area", new DialogComponentNumber(createMaxAreaModel(),
                        "Maximum segment area (pixels)", 1));
                addDialogComponent("Options", "Other Filter Criteria", new DialogComponentBoolean(
                        createRemoveBorderSegModel(), "Remove segments touching the border"));
                addDialogComponent("Options", "Speed-up", new DialogComponentBoolean(createContainsNoOverlapsModel(),
                        "Contains NO overlapping segments"));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<LabelingValue<L>, ? extends DataCell> createNodeModel() {
        return new ValueToCellNodeModel<LabelingValue<L>, LabelingCell<L>>() {

            //settings models
            private SettingsModelString m_smExclPattern = createExclPatternModel();

            private SettingsModelString m_smInclPattern = createInclPatternModel();

            private SettingsModelInteger m_smMaxArea = createMaxAreaModel();

            private SettingsModelInteger m_smMinArea = createMinAreaModel();

            private SettingsModelBoolean m_smRemoveBorderSegments = createRemoveBorderSegModel();

            private SettingsModelBoolean m_smContainsNoOverlappingSegments = createContainsNoOverlapsModel();

            // labeling cell factory
            private LabelingCellFactory m_labCellFactory;

            //compiled filter patterns
            private Pattern m_inclPattern = null;

            private Pattern m_exclPattern = null;

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_labCellFactory = new LabelingCellFactory(exec);
                if (m_smInclPattern.getStringValue().length() > 0) {
                    m_inclPattern = Pattern.compile(WildcardMatcher.wildcardToRegex(m_smInclPattern.getStringValue()));
                } else {
                    m_inclPattern = null;
                }

                if (m_smExclPattern.getStringValue().length() > 0) {
                    m_exclPattern = Pattern.compile(WildcardMatcher.wildcardToRegex(m_smExclPattern.getStringValue()));
                } else {
                    m_exclPattern = null;
                }
            }

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_smExclPattern);
                settingsModels.add(m_smInclPattern);
                settingsModels.add(m_smMaxArea);
                settingsModels.add(m_smMinArea);
                settingsModels.add(m_smRemoveBorderSegments);
                settingsModels.add(m_smContainsNoOverlappingSegments);
            }

            @Override
            protected LabelingCell<L> compute(final LabelingValue<L> cellValue) throws Exception {
                Labeling<L> lab = cellValue.getLabeling();

                //calculate labeling statistics
                Collection<L> labels = lab.getLabels();

                //labels to be included in the result labeling
                Set<String> include = new HashSet<String>(labels.size());

                for (L label : labels) {
                    //filter according to the labeling name
                    //exclude regexp
                    if (m_exclPattern != null && m_exclPattern.matcher(label.toString()).matches()) {
                        continue;
                    }

                    //include regexp
                    if (m_inclPattern != null && !m_inclPattern.matcher(label.toString()).matches()) {
                        continue;
                    }

                    //filter according to label size
                    if (m_smMaxArea.getIntValue() != Integer.MAX_VALUE
                            && lab.getArea(label) > m_smMaxArea.getIntValue()) {
                        continue;
                    }
                    if (m_smMinArea.getIntValue() != 0 && lab.getArea(label) < m_smMinArea.getIntValue()) {
                        continue;
                    }

                    //filter segment which are touching the border (i.e. their bounding box)
                    if (m_smRemoveBorderSegments.getBooleanValue()) {
                        long[] min = new long[lab.numDimensions()];
                        long[] max = new long[lab.numDimensions()];
                        lab.getExtents(label, min, max);
                        boolean touchesBorder = false;
                        for (int i = 0; i < max.length; i++) {
                            if (min[i] == 0) {
                                touchesBorder = true;
                                break;
                            }
                            if (max[i] == lab.max(i)) {
                                touchesBorder = true;
                                break;
                            }
                        }
                        if (touchesBorder) {
                            continue;
                        }
                    }

                    include.add(label.toString());
                }

                //TODO speed-up: decide whether to make a copy or an empty copy (depending on the amount of segments to be included/excluded) and remove excluded segments or add included segments
                Labeling<L> res = (Labeling<L>)lab.factory().create(lab);
                LabelingMapping<L> resMapping = res.firstElement().getMapping();
                for (L label : labels) {
                    if (include.contains(label.toString())) {
                        IterableInterval<LabelingType<L>> ii =
                                lab.getIterableRegionOfInterest(label).getIterableIntervalOverROI(res);
                        if (!m_smContainsNoOverlappingSegments.getBooleanValue()) {
                            List<L> tmpList = new ArrayList<L>(5);
                            for (LabelingType<L> type : ii) {
                                tmpList.clear();
                                tmpList.addAll(type.getLabeling());
                                tmpList.add(label);
                                type.setLabeling(tmpList);
                            }
                        } else {
                            //speed-up: pre-create the labeling if overlapping segment are not expected
                            List<L> labeling = resMapping.intern(Arrays.asList(label));
                            for (LabelingType<L> type : ii) {
                                type.setLabeling(labeling);
                            }
                        }
                    }
                }

                return m_labCellFactory.createCell(res, cellValue.getLabelingMetadata());
            }
        };
    }
}
