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
package org.knime.knip.base.nodes.features.providers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import net.imglib2.IterableInterval;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.ops.data.CooccurrenceMatrix.MatrixOrientation;
import net.imglib2.ops.operation.iterableinterval.unary.MakeCooccurrenceMatrix.HaralickFeature;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ValuePair;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.knip.core.features.FeatureFactory;
import org.knime.knip.core.features.FeatureSet;
import org.knime.knip.core.features.seg.HaralickFeatureSet;
import org.knime.knip.core.util.EnumListProvider;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class HaralickFeatureSetProvider<T extends RealType<T>> implements
        FeatureSetProvider<ValuePair<IterableInterval<T>, CalibratedSpace>> {

    private SettingsModelBoolean m_computeaverage;

    private SettingsModelStringArray m_coocMtx;

    private SettingsModelInteger m_distance;

    private FeatureFactory m_featFactory;

    private SettingsModelInteger m_greylevel;

    private int m_numFeatureSets;

    private String[] m_selectedCoocMatrices;

    private SettingsModelStringArray m_textFeat;

    @Override
    public void
            calcAndAddFeatures(final ValuePair<IterableInterval<T>, CalibratedSpace> roi, final List<DataCell> cells) {

        m_featFactory.updateFeatureTarget(roi.a);
        m_featFactory.updateFeatureTarget(roi.b);

        // Features
        double[] avgFeatures = null;

        if (m_computeaverage.getBooleanValue()) {
            avgFeatures = new double[m_featFactory.getNumFeatures() / m_numFeatureSets];
        }

        int k = 0;
        for (int featID = 0; featID < m_featFactory.getNumFeatures(); featID++) {

            double val = m_featFactory.getFeatureValue(featID);

            if (!Double.isNaN(val)) {
                cells.add(new DoubleCell(val));
            } else {
                // missing cells instead of NaN
                cells.add(DataType.getMissingCell());
                // no NaN in average
                val = 0;
            }

            if (m_computeaverage.getBooleanValue()) {
                avgFeatures[k++ % (m_featFactory.getNumFeatures() / m_numFeatureSets)] += val;

            }

        }

        if (m_computeaverage.getBooleanValue()) {
            for (final double d : avgFeatures) {
                if (Double.isNaN(d / m_numFeatureSets) || (d == 0)) {
                    cells.add(DataType.getMissingCell());
                } else {
                    cells.add(new DoubleCell(d / m_numFeatureSets));
                }
            }

        }

    }

    private SettingsModelBoolean createCompAverageModel() {
        return new SettingsModelBoolean("CFG_AVERGAGE", false);
    }

    private SettingsModelStringArray createCoocMatrixModel() {
        return new SettingsModelStringArray("haralick_cooc_mtx_selection",
                EnumListProvider.getStringList(MatrixOrientation.values()));
    }

    private SettingsModelInteger createDistanceModel() {
        return new SettingsModelIntegerBounded("haralick_distance", 1, 1, Integer.MAX_VALUE);
    }

    private SettingsModelInteger createGreylevelModel() {
        return new SettingsModelIntegerBounded("haralick_greylevel", 40, 0, Integer.MAX_VALUE);
    }

    private SettingsModelStringArray createTextFeatModel() {
        return new SettingsModelStringArray("haralick_texture_feature_selection",
                EnumListProvider.getStringList(HaralickFeature.values()));
    }

    @Override
    public String getFeatureSetName() {
        return "Haralick";
    }

    @Override
    public void initAndAddColumnSpecs(final List<DataColumnSpec> specs) {
        // select the appropriate cooccurrence matrices
        m_selectedCoocMatrices = m_coocMtx.getStringArrayValue();
        final List<FeatureSet> featSet = new ArrayList<FeatureSet>();
        final BitSet selection = new BitSet();
        int featOffset = 0;

        for (final String s : m_coocMtx.getStringArrayValue()) {

            final HaralickFeatureSet<T> set =
                    new HaralickFeatureSet<T>(m_greylevel.getIntValue(), m_distance.getIntValue(),
                            MatrixOrientation.valueOf(s));

            // select the appropriate features
            final String[] selectedFeatures = m_textFeat.getStringArrayValue();

            int j = 0;
            for (int i = 0; i < set.numFeatures(); i++) {
                if ((j < selectedFeatures.length) && selectedFeatures[j].equals(set.name(i))) {
                    selection.set(featOffset + i);
                    j++;
                }
            }
            featSet.add(set);
            featOffset += set.numFeatures();
        }

        m_featFactory = new FeatureFactory(false, featSet);
        m_featFactory.initFeatureFactory(selection);

        final String[] featNames = m_featFactory.getFeatureNames();

        for (int n = 0; n < featNames.length; n++) {
            specs.add(new DataColumnSpecCreator(featNames[n] + " | "
                    + m_selectedCoocMatrices[((n * m_selectedCoocMatrices.length) / featNames.length)] + " | "
                    + m_distance.getIntValue() + " | " + m_greylevel.getIntValue(), DoubleCell.TYPE).createSpec());
        }

        m_numFeatureSets = featSet.size();

        if (m_computeaverage.getBooleanValue()) {
            for (int i = 0; i < (featNames.length / m_numFeatureSets); i++) {
                specs.add(new DataColumnSpecCreator(featNames[i] + " | (AVG)", DoubleCell.TYPE).createSpec());
            }
        }

    }

    @Override
    public void initAndAddDialogComponents(final List<DialogComponent> dialogComponents) {

        dialogComponents.add(new DialogComponentStringListSelection(createTextFeatModel(), "Features", EnumListProvider
                .getStringCollection(HaralickFeature.values()), true, 5));

        dialogComponents.add(new DialogComponentStringListSelection(createCoocMatrixModel(), "Matrices", Arrays
                .asList(EnumListProvider.getStringList(MatrixOrientation.values())), true, 4));

        dialogComponents.add(new DialogComponentBoolean(createCompAverageModel(), "Compute Average"));

        dialogComponents.add(new DialogComponentNumber(createGreylevelModel(), "greylevel", 1));

        dialogComponents.add(new DialogComponentNumber(createDistanceModel(), "distance", 1));

    }

    @Override
    public void initAndAddSettingsModels(final List<SettingsModel> settingsModels) {

        settingsModels.add(m_greylevel = createGreylevelModel());
        settingsModels.add(m_coocMtx = createCoocMatrixModel());
        settingsModels.add(m_distance = createDistanceModel());
        settingsModels.add(m_textFeat = createTextFeatModel());
        settingsModels.add(m_computeaverage = createCompAverageModel());

    }
}
