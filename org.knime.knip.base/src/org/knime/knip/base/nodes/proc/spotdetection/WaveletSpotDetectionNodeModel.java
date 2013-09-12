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
package org.knime.knip.base.nodes.proc.spotdetection;

import java.util.List;

import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeModel;
import org.knime.knip.core.util.ImgPlusFactory;

/*
 * Implementation status:
 * Currently using B3SplineUDWT and WaveletConfigException from ICY both could be replaced by the imglib implementation
 * but the array specific code runs faster than standard convolution. *
 */

/**
 * Implementation of Extraction of spots in biological images using multiscale products (Pattern Recognition 35)<br>
 * Jean-Christophe Olivo-Marin<br>
 * <br>
 * Allows the detection of bright spots over dark background using a multiscale wavelet partition of the image.
 * 
 * 
 * @param <T>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class WaveletSpotDetectionNodeModel<T extends RealType<T>> extends ImgPlusToImgPlusNodeModel<T, BitType> {

    private static final String MAD = "mad";

    /* noise estimation statistics */
    private static final String MEAN_MAD = "mean mad";

    static SettingsModelString createAvgNodeModel() {
        return new SettingsModelString("avgMethod", MEAN_MAD);
    }

    /**
     * @return {@link SettingsModelDoubleArray} for a {@link DialogComponentScaleConfig} with three wavelet levels and
     *         enabled levels two and three.
     */
    static SettingsModelDoubleArray createScaleModel() {
        // TODO find good starting values (are these already good)
        final double[] values =
                DialogComponentScaleConfig.createModelArray(new boolean[]{false, true, true}, new double[]{1.0, 1.0,
                        1.0});

        return new SettingsModelDoubleArray("scaleConfig", values);
    }

    static String[] getAvgOptions() {
        return new String[]{MEAN_MAD, MAD};
    }

    private final SettingsModelString m_avgModel = createAvgNodeModel();

    private final SettingsModelDoubleArray m_scaleModel = createScaleModel();

    public WaveletSpotDetectionNodeModel(final String... axes) {
        super(axes);
    }

    @Override
    protected void addSettingsModels(final List<SettingsModel> settingsModels) {
        settingsModels.add(m_avgModel);
        settingsModels.add(m_scaleModel);
    }

    @Override
    protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<BitType>> op(final ImgPlus<T> imgPlus) {

        boolean useMeanMAD = true;
        if (m_avgModel.getStringValue().equals(MAD)) {
            useMeanMAD = false;
        }

        final boolean[] enabled = DialogComponentScaleConfig.getEnabledState(m_scaleModel);
        final double[] factor = DialogComponentScaleConfig.getThresholdValues(m_scaleModel);

        return Operations.wrap(new WaveletSpotDetection<T>(enabled, factor, useMeanMAD),
                               new ImgPlusFactory<T, BitType>(new BitType()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getMinDimensions() {
        return 2;
    }

}
