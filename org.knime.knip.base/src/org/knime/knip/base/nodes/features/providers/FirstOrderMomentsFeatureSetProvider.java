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

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.meta.CalibratedSpace;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ValuePair;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.knip.core.features.FeatureFactory;
import org.knime.knip.core.features.seg.FirstOrderMomentsFeatureSet;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class FirstOrderMomentsFeatureSetProvider<T extends RealType<T>> implements
        FeatureSetProvider<ValuePair<IterableInterval<T>, CalibratedSpace>> {

    private SettingsModelBoolean m_appendHistogram;

    private FeatureFactory m_featFac;

    private FirstOrderMomentsFeatureSet<T> m_featSet;

    private SettingsModelStringArray m_fosFeat;

    private SettingsModelIntegerBounded m_histogramBins;

    private SettingsModelString m_percentiles;

    private final ArrayList<Double> m_percentileValues = new ArrayList<Double>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void
            calcAndAddFeatures(final ValuePair<IterableInterval<T>, CalibratedSpace> roi, final List<DataCell> cells) {
        m_featFac.updateFeatureTarget(roi.a);
        m_featFac.updateFeatureTarget(roi.b);

        // Features

        for (int featID = 0; featID < m_featFac.getNumFeatures(); featID++) {
            final double val = m_featFac.getFeatureValue(featID);
            if (Double.isNaN(val)) {
                cells.add(DataType.getMissingCell());
            } else {
                cells.add(new DoubleCell(val));
            }

        }
        if (m_appendHistogram.getBooleanValue()) {
            final Cursor<T> c = roi.a.cursor();

            final double min = c.get().createVariable().getMinValue();
            final double max = c.get().createVariable().getMaxValue();
            final int[] hist = new int[m_histogramBins.getIntValue()];
            final double scale = (hist.length - 1) / (max - min);

            while (c.hasNext()) {
                c.fwd();
                hist[(int)((c.get().getRealDouble() - min) * scale)]++;
            }
            for (int j = 0; j < m_histogramBins.getIntValue(); j++) {
                cells.add(new IntCell(hist[j]));
            }
        }

        for (int i = 0; i < m_percentileValues.size(); i++) {
            cells.add(new DoubleCell(m_featSet.getPercentile(roi.a, m_percentileValues.get(i))));
        }

    }

    private SettingsModelBoolean createAppendHistModel() {
        return new SettingsModelBoolean("append_histogram", false);
    }

    private SettingsModelStringArray createFosFeatModel() {
        return new SettingsModelStringArray("fos_feature_selection", FirstOrderMomentsFeatureSet.FEATURES);
    }

    private SettingsModelIntegerBounded createHistBinsModel() {
        return new SettingsModelIntegerBounded("histogram_bins", 64, 1, Integer.MAX_VALUE);
    }

    private SettingsModelString createPercentilesModel() {
        return new SettingsModelString("percentiles", "");
    }

    @Override
    public String getFeatureSetName() {
        return "First order statistics";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initAndAddColumnSpecs(final List<DataColumnSpec> specs) {

        m_featSet = new FirstOrderMomentsFeatureSet<T>();
        m_featFac = new FeatureFactory(false, m_featSet);

        // select the appropriate features
        final String[] selectedFeatures = m_fosFeat.getStringArrayValue();

        final String[] allFeat = FirstOrderMomentsFeatureSet.FEATURES;
        final BitSet selection = new BitSet(allFeat.length);
        int j = 0;
        for (int i = 0; i < allFeat.length; i++) {
            if ((j < selectedFeatures.length) && selectedFeatures[j].equals(allFeat[i])) {
                selection.set(i);
                j++;
            }
        }
        m_featFac.initFeatureFactory(selection);

        // create outspec according to the selected features
        final String[] featNames = m_featFac.getFeatureNames();
        for (int i = 0; i < featNames.length; i++) {
            specs.add(new DataColumnSpecCreator(featNames[i], DoubleCell.TYPE).createSpec());
        }
        if (m_appendHistogram.getBooleanValue()) {
            for (int i = 0; i < m_histogramBins.getIntValue(); i++) {
                specs.add(new DataColumnSpecCreator("h_" + i, IntCell.TYPE).createSpec());
            }
        }

        final String[] percentiles = m_percentiles.getStringValue().split(",");
        m_percentileValues.clear();
        for (int i = 0; i < percentiles.length; i++) {
            if (percentiles[i].trim().length() == 0) {
                continue;
            }
            try {
                final double p = Double.parseDouble(percentiles[i]);

                m_percentileValues.add(p);
                specs.add(new DataColumnSpecCreator(percentiles[i] + "th Percentile_", DoubleCell.TYPE).createSpec());
            } catch (final NumberFormatException e) {
                throw new NumberFormatException("Wrong format of the percentile specification!");

            }
        }
    }

    @Override
    public void initAndAddDialogComponents(final List<DialogComponent> dialogComponents) {

        dialogComponents.add(new DialogComponentStringListSelection(createFosFeatModel(), "Features", Arrays
                .asList(FirstOrderMomentsFeatureSet.FEATURES), true, 5));

        dialogComponents.add(new DialogComponentBoolean(createAppendHistModel(), "Append Histogram"));

        dialogComponents.add(new DialogComponentNumberEdit(createHistBinsModel(), "Histogram bins"));

        dialogComponents.add(new DialogComponentString(createPercentilesModel(),
                "The pth percentile (comma-separated list of p's)"));

    }

    @Override
    public void initAndAddSettingsModels(final List<SettingsModel> settingsModels) {
        settingsModels.add(m_appendHistogram = createAppendHistModel());
        settingsModels.add(m_fosFeat = createFosFeatModel());
        settingsModels.add(m_histogramBins = createHistBinsModel());
        settingsModels.add(m_percentiles = createPercentilesModel());

    }
}
