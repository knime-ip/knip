/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2017
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
package org.knime.knip.base.nodes.orientationj.measure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.util.Pair;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellsNodeModel;
import org.knime.knip.core.KNIPGateway;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * The node model of the node which calculates OrientationJ features.
 *
 * @author Simon Schmid, University of Konstanz, Germany
 */
final class OrientationJMeasurementNodeModel<T extends RealType<T>> extends ValueToCellsNodeModel<ImgPlusValue<T>> {

    private final SettingsModelDoubleBounded m_sigmaModel = createSigmaDoubleModel();

    private final SettingsModelBoolean m_featureEnergyModel = createFeatureBooleanModel("energy");

    private final SettingsModelBoolean m_featureOrientationModel = createFeatureBooleanModel("orientation");

    private final SettingsModelBoolean m_featureCoherencyModel = createFeatureBooleanModel("coherency");

    /** @return the double model, used in both dialog and model. */
    static SettingsModelDoubleBounded createSigmaDoubleModel() {
        return new SettingsModelDoubleBounded("sigma", 0.0, 0.0, 100.0);
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createFeatureBooleanModel(final String feature) {
        return new SettingsModelBoolean("feature_" + feature, true);
    }

    @Override
    protected void addSettingsModels(List<SettingsModel> settingsModels) {
        settingsModels.add(m_sigmaModel);
        settingsModels.add(m_featureEnergyModel);
        settingsModels.add(m_featureOrientationModel);
        settingsModels.add(m_featureCoherencyModel);
    }

    @Override
    protected DataCell[] compute(ImgPlusValue<T> cellValue) throws Exception {
        final RandomAccessibleInterval<T> input = Views.dropSingletonDimensions(cellValue.getImgPlus());
        if (input.numDimensions() > 2) {
            throw new IllegalArgumentException("Input image must be 2-dimensional!");
        }

        // Apply a LoG filter, if sigma is greater than zero.
        final RandomAccessibleInterval<T> img;
        if (m_sigmaModel.getDoubleValue() > 0.0) {
            img = new LaplacianOfGaussian<T>().calculate(input, m_sigmaModel.getDoubleValue());
        } else {
            img = input;
        }

        // Calculate features.
        final Measure<T> measure = new Measure<T>();
        KNIPGateway.getInstance().ctx().inject(measure);
        final double[] measurements = measure.calculate(img);

        // Create output.
        final ArrayList<DataCell> outputs = new ArrayList<>();
        if (m_featureEnergyModel.getBooleanValue())
            outputs.add(DoubleCellFactory.create(measurements[0]));
        if (m_featureOrientationModel.getBooleanValue())
            outputs.add(DoubleCellFactory.create(measurements[1]));
        if (m_featureCoherencyModel.getBooleanValue())
            outputs.add(DoubleCellFactory.create(measurements[2]));
        final DataCell[] output = new DataCell[outputs.size()];
        outputs.toArray(output);
        return output;
    }

    @Override
    protected Pair<DataType[], String[]> getDataOutTypeAndName() {
        final ArrayList<String> outputs = new ArrayList<>();
        if (m_featureEnergyModel.getBooleanValue())
            outputs.add("Energy");
        if (m_featureOrientationModel.getBooleanValue())
            outputs.add("Orientation");
        if (m_featureCoherencyModel.getBooleanValue())
            outputs.add("Coherency");
        final DataType[] types = new DataType[outputs.size()];
        final String[] names = new String[outputs.size()];
        Arrays.fill(types, DoubleCell.TYPE);
        outputs.toArray(names);
        return new Pair<DataType[], String[]>(types, names);
    }

}
