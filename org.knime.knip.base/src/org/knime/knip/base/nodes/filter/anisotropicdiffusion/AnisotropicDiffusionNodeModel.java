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
package org.knime.knip.base.nodes.filter.anisotropicdiffusion;

import java.util.List;

import net.imglib2.algorithm.pde.PeronaMalikAnisotropicDiffusion;
import net.imglib2.algorithm.pde.PeronaMalikAnisotropicDiffusion.DiffusionFunction;
import net.imglib2.algorithm.pde.PeronaMalikAnisotropicDiffusion.StrongEdgeEnhancer;
import net.imglib2.algorithm.pde.PeronaMalikAnisotropicDiffusion.WideRegionEnhancer;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeModel;
import org.knime.knip.core.ops.filters.PeronaMalikAnisotropicDiffusionOp;
import org.knime.knip.core.util.ImgPlusFactory;

/**
 * 
 * This implements the Perona & Malik Anisotropic Diffusion algorithm as a Node. For the actual implementation of the
 * algorithm see {@link PeronaMalikAnisotropicDiffusion}.
 * 
 * @param <T> the pixel type of the input and output image
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Lukas Siedentop
 */
public class AnisotropicDiffusionNodeModel<T extends RealType<T> & NativeType<T>> extends
        ImgPlusToImgPlusNodeModel<T, T> {

    public enum DiffusionFunctionType {
        STRONG_EDGE_ENHANCER, WIDE_REGION_ENHANCER
    }

    protected static SettingsModelDouble createDeltaTModel() {
        return new SettingsModelDouble("deltat", 0.5);
    }

    protected static SettingsModelString createDiffFunModel() {
        return new SettingsModelString("diffFun",
                AnisotropicDiffusionNodeModel.DiffusionFunctionType.STRONG_EDGE_ENHANCER.toString());
    }

    protected static SettingsModelInteger createIterationModel() {
        return new SettingsModelInteger("num_iterations", 10);
    }

    protected static SettingsModelDouble createKappaModel() {
        return new SettingsModelDouble("kappa", 7);
    }

    // the integration constant for the numerical integration scheme.
    // Typically
    // less that 1. See {@link PeronaMalikAnisotropicDiffusion}.
    private final SettingsModelDouble m_smDeltat = createDeltaTModel();

    // the Function to be used for filtering
    private final SettingsModelString m_smDiffFun = createDiffFunModel();

    // the constant for the diffusion function that sets its gradient
    // threshold.
    // See {@link DiffusionFunction}.
    private final SettingsModelDouble m_smKappa = createKappaModel();

    // number of Iterations
    private final SettingsModelInteger m_smn = createIterationModel();

    protected AnisotropicDiffusionNodeModel() {
        super("X", "Y");
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void addSettingsModels(final List<SettingsModel> settingsModels) {

        // Delta t
        settingsModels.add(m_smDeltat);

        // Kappa
        settingsModels.add(m_smKappa);

        // Diffusion Function
        settingsModels.add(m_smDiffFun);

        // Number of Iterations
        settingsModels.add(m_smn);
    }

    /**
     * @return the diffusion function which is selected
     * @throws InvalidSettingsException
     */
    private DiffusionFunction getFun() {
        if (m_smDiffFun.getStringValue().equalsIgnoreCase("STRONG_EDGE_ENHANCER")) {
            return new StrongEdgeEnhancer(m_smKappa.getDoubleValue());
        } else if (m_smDiffFun.getStringValue().equalsIgnoreCase("WIDE_REGION_ENHANCER")) {
            return new WideRegionEnhancer(m_smKappa.getDoubleValue());
        } else {
            throw new RuntimeException("Unknown Diffusion Function");
        }
    }

    @Override
    protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<T>> op(final ImgPlus<T> imgPlus) {
        return Operations.wrap(new PeronaMalikAnisotropicDiffusionOp<T, ImgPlus<T>>(m_smDeltat.getDoubleValue(), m_smn
                                       .getIntValue(), getFun(), 1), ImgPlusFactory.<T, T> get(imgPlus.firstElement()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getMinDimensions() {
        return 1;
    }
}
