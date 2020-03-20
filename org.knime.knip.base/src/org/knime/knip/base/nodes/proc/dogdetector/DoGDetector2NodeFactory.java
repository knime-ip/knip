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
package org.knime.knip.base.nodes.proc.dogdetector;

import java.util.List;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeDialog;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeFactory;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeModel;
import org.knime.knip.core.types.OutOfBoundsStrategyEnum;
import org.knime.knip.core.types.OutOfBoundsStrategyFactory;
import org.knime.knip.core.util.EnumUtils;

import net.imagej.ImgPlus;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.dog.DogDetection.ExtremaType;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

/**
 * {@link NodeFactory} for {@link DoGDetector2NodeFactory} wrapping the {@link DogDetection}
 *
 * @param <T>
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 */
@Deprecated
public class DoGDetector2NodeFactory<T extends RealType<T> & NativeType<T>> extends
        ImgPlusToImgPlusNodeFactory<T, BitType> {

    static SettingsModelDouble createSigma2Model() {
        return new SettingsModelDoubleBounded("sigma1_value", 5, 0.001, Double.MAX_VALUE);
    }

    static SettingsModelDouble createSigma1Model() {
        return new SettingsModelDoubleBounded("sigma2_value", 3, 0.001, Double.MAX_VALUE);
    }

    static SettingsModelDouble createThresholdModel() {
        return new SettingsModelDoubleBounded("threshold_value", 0.5, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    static SettingsModelString createExtremaTypeModel() {
        return new SettingsModelString("extrema_model", ExtremaType.MINIMA.toString());
    }

    static SettingsModelString createOutOfBoundsModel() {
        return new SettingsModelString("outofboundsstrategy", OutOfBoundsStrategyEnum.BORDER.toString());
    }

    static SettingsModelBoolean createNormalizeModel() {
        return new SettingsModelBoolean("normalize_model", false);
    }

    @Override
    protected ImgPlusToImgPlusNodeDialog<T> createNodeDialog() {
        return new ImgPlusToImgPlusNodeDialog<T>(2, Integer.MAX_VALUE, "X", "Y") {

            @Override
            public void addDialogComponents() {

                addDialogComponent("Options", "", new DialogComponentStringSelection(createExtremaTypeModel(),
                        "Minima/Maxima?", EnumUtils.getStringListFromToString(ExtremaType.values())));

                addDialogComponent("Options", "", new DialogComponentNumber(createThresholdModel(), "Threshold", 1.5));

                addDialogComponent("Options", "", new DialogComponentBoolean(createNormalizeModel(), "Normalize Threshold?"));

                addDialogComponent("Options", "", new DialogComponentNumber(createSigma1Model(), "Sigma 1", 1.5));

                addDialogComponent("Options", "", new DialogComponentNumber(createSigma2Model(), "Sigma 2", 1.5));

                addDialogComponent("Options",
                                   "Out of Bounds Strategy",
                                   new DialogComponentStringSelection(DoGDetector2NodeFactory.createOutOfBoundsModel(),
                                           "Out of Bounds Strategy", EnumUtils
                                                   .getStringCollectionFromToString(OutOfBoundsStrategyEnum.values())));

            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected String getDefaultSuffixForAppend() {
                return "_DoG";
            }
        };
    }

    @Override
    public ImgPlusToImgPlusNodeModel<T, BitType> createNodeModel() {
        return new ImgPlusToImgPlusNodeModel<T, BitType>("X", "Y") {

            private SettingsModelDouble m_sigma1Model = createSigma1Model();

            private SettingsModelDouble m_sigma2Model = createSigma2Model();

            private SettingsModelString m_extremaModel = createExtremaTypeModel();

            private SettingsModelDouble m_thresholdModel = createThresholdModel();

            private SettingsModelString m_oob = createOutOfBoundsModel();

            private final SettingsModelBoolean m_normalizeModel = createNormalizeModel();

            @Override
            protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<BitType>> op(final ImgPlus<T> imgPlus) {
                return new DoGDetectorOp2<T>(m_sigma1Model.getDoubleValue(), m_sigma2Model.getDoubleValue(),
                        ExtremaType.valueOf(m_extremaModel.getStringValue()),
                        OutOfBoundsStrategyFactory.getStrategy(m_oob.getStringValue(), imgPlus.firstElement()),
                        m_thresholdModel.getDoubleValue(), m_normalizeModel.getBooleanValue(), getExecutorService());
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                enableParallelization(false);
                super.prepareExecute(exec);
            }

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_sigma1Model);
                settingsModels.add(m_sigma2Model);
                settingsModels.add(m_extremaModel);
                settingsModels.add(m_oob);
                settingsModels.add(m_thresholdModel);
                settingsModels.add(m_normalizeModel);
            }

            @Override
            protected int getMinDimensions() {
                return 2;
            }
        };
    }
}
