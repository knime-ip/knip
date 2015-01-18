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
package org.knime.knip.base.nodes.proc.clahe;

import java.util.List;

import net.imagej.ImgPlus;
import net.imglib2.ops.operation.ImgOperations;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeDialog;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeFactory;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeModel;

/**
 * Factory class to create {@link ClaheNodeFactory}
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:daniel.seebacher@uni-konstanz.de">Daniel Seebacher</a>
 *
 * @param <T> extends RealType<T>
 *
 */
public class ClaheNodeFactory<T extends RealType<T>> extends ImgPlusToImgPlusNodeFactory<T, T> {

    private SettingsModelIntegerBounded createCtxDimValue() {
        return new SettingsModelIntegerBounded("nrctxregions", 8, 1, 64);
    }

    private static SettingsModelIntegerBounded createCtxNumberOfBins() {
        return new SettingsModelIntegerBounded("nrbins", 256, 16, 4096);
    }

    private static SettingsModelDoubleBounded createCtxSlope() {
        return new SettingsModelDoubleBounded("slope", 3, 1, Double.MAX_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImgPlusToImgPlusNodeDialog<T> createNodeDialog() {

        return new ImgPlusToImgPlusNodeDialog<T>(2, 5, "X", "Y") {

            @Override
            public void addDialogComponents() {

                addDialogComponent("Options", "CLAHE Options", new DialogComponentNumber(createCtxDimValue(),
                        "Number of contextual regions", 1));

                addDialogComponent("Options", "CLAHE Options", new DialogComponentNumber(createCtxNumberOfBins(),
                        "Number of bins", 1));

                addDialogComponent("Options", "CLAHE Options",
                                   new DialogComponentNumber(createCtxSlope(), "Slope", 0.1));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImgPlusToImgPlusNodeModel<T, T> createNodeModel() {

        return new ImgPlusToImgPlusNodeModel<T, T>(true, "X", "Y") {

            private final SettingsModelIntegerBounded m_ctxValues = createCtxDimValue();

            private final SettingsModelIntegerBounded m_bins = createCtxNumberOfBins();

            private final SettingsModelDoubleBounded m_slope = createCtxSlope();

            @Override
            protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<T>> op(final ImgPlus<T> imgPlus) {
                // store image dimensions and check if image dimensions are larger than the ctxRegions
                ClaheND<T> clahe =
                        new ClaheND<T>(m_ctxValues.getIntValue(), m_bins.getIntValue(), m_slope.getDoubleValue());

                return ImgOperations.wrapRA(clahe, imgPlus.firstElement());
            }

            @Override
            protected int getMinDimensions() {
                return 2;
            }

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_ctxValues);
                settingsModels.add(m_bins);
                settingsModels.add(m_slope);
            }
        };
    }
}
