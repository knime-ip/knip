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
package org.knime.knip.base.nodes.filter.nonlinear;

import java.util.List;

import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeDialog;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeFactory;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeModel;
import org.knime.knip.core.ops.filters.BilateralFilter;
import org.knime.knip.core.util.ImgPlusFactory;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class BilateralFilterNodeFactory<T extends RealType<T>> extends ImgPlusToImgPlusNodeFactory<T, T> {

    private static SettingsModelInteger createRadiusModel() {
        return new SettingsModelInteger("radius", 10);
    }

    private static SettingsModelDouble createSigmaRModel() {
        return new SettingsModelDouble("sigma_r", 15.0);
    }

    private static SettingsModelDouble createSigmaSModel() {
        return new SettingsModelDouble("sigma_s", 5.0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImgPlusToImgPlusNodeDialog<T> createNodeDialog() {
        return new ImgPlusToImgPlusNodeDialog<T>(2, 2, "X", "Y") {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "Deviations", new DialogComponentNumber(createSigmaRModel(),
                        "Sigma_r (intensity domain)", 15));
                addDialogComponent("Options", "Deviations", new DialogComponentNumber(createSigmaSModel(),
                        "Sigma_s (spacial domain)", 5));
                addDialogComponent("Options", "Radius", new DialogComponentNumber(createRadiusModel(),
                        "Radius to consider", 10));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImgPlusToImgPlusNodeModel<T, T> createNodeModel() {
        return new ImgPlusToImgPlusNodeModel<T, T>("X", "Y") {

            private final SettingsModelInteger m_smRadius = createRadiusModel();

            private final SettingsModelDouble m_smSigmaR = createSigmaRModel();

            private final SettingsModelDouble m_smSigmaS = createSigmaSModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_smRadius);
                settingsModels.add(m_smSigmaR);
                settingsModels.add(m_smSigmaS);
            }

            @Override
            protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<T>> op(final ImgPlus<T> imgPlus) {
                return Operations.wrap(new BilateralFilter<T, ImgPlus<T>>(m_smSigmaR.getDoubleValue(), m_smSigmaS
                                               .getDoubleValue(), m_smRadius.getIntValue()), ImgPlusFactory
                                               .<T, T> get(imgPlus.firstElement()));
            }

            @Override
            protected int getMinDimensions() {
                return 2;
            }
        };
    }
}
