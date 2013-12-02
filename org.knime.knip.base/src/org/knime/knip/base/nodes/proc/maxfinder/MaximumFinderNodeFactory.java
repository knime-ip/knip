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
package org.knime.knip.base.nodes.proc.maxfinder;

import java.util.List;

import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.ImgOperations;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.NodeDialog;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeDialog;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeFactory;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.node2012.KnimeNodeDocument.KnimeNode;

/**
 * {@link NodeFactory} containint {@link NodeModel} and {@link NodeDialog} for {@link MaximumFinderOp}
 *
 * @author Tino Klingebiel, University of Konstanz
 * @author Jonathan Hale, Unversity of Konstanz
 * @author Martin Horn, University of Konstanz
 * @author Christian Dietz, University of Konstanz
 *
 * @param <T>
 */
public class MaximumFinderNodeFactory<T extends RealType<T> & NativeType<T>> extends
        ImgPlusToImgPlusNodeFactory<T, BitType> {

    private static SettingsModelDouble createToleranceModel() {
        return new SettingsModelDouble("Noise Tolerance", 0);
    }

    private static SettingsModelDouble createSuppressionModel() {
        return new SettingsModelDouble("Suppression", 0);
    }

    private static SettingsModelBoolean createMaxAreaModel() {
        return new SettingsModelBoolean("Tolearance Areas", false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImgPlusToImgPlusNodeModel<T, BitType> createNodeModel() {
        return new ImgPlusToImgPlusNodeModel<T, BitType>() {

            private SettingsModelDouble m_toleranceModel = createToleranceModel();

            private SettingsModelDouble m_suppressionModel = createSuppressionModel();

            private SettingsModelBoolean m_maxAreasModel = createMaxAreaModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_toleranceModel);
                settingsModels.add(m_suppressionModel);
                settingsModels.add(m_maxAreasModel);
            }

            @Override
            protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<BitType>> op(final ImgPlus<T> imgPlus) {
                UnaryOutputOperation<ImgPlus<T>, ImgPlus<BitType>> wrappedOp =
                        ImgOperations.wrapRA(new MaximumFinderOp<T>(m_toleranceModel.getDoubleValue(),
                                m_suppressionModel.getDoubleValue(), m_maxAreasModel.getBooleanValue()), new BitType());

                return wrappedOp;
            }

            @Override
            protected int getMinDimensions() {
                return 1;
            }

        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImgPlusToImgPlusNodeDialog<T> createNodeDialog() {

        return new ImgPlusToImgPlusNodeDialog<T>(1, Integer.MAX_VALUE, "X", "Y") {
            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "Options", new DialogComponentNumber(createToleranceModel(),
                        "Noise Tolerance", 0.5));
                addDialogComponent("Options", "Options", new DialogComponentNumber(createSuppressionModel(),
                        "Supression", 0.5));
                addDialogComponent(new DialogComponentBoolean(createMaxAreaModel(), "Output with Tolerance Areas"));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addNodeDescriptionContent(final KnimeNode node) {
        DialogComponentDimSelection.createNodeDescription(node.getFullDescription().getTabList().get(0).addNewOption());
    }
}
