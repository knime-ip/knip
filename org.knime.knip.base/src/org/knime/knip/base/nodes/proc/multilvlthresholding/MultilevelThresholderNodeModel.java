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
package org.knime.knip.base.nodes.proc.multilvlthresholding;

import java.util.List;

import net.imagej.ImgPlus;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.ops.operation.iterableinterval.unary.multilevelthresholder.MultilevelThresholderOp;
import net.imglib2.ops.operation.iterableinterval.unary.multilevelthresholder.MultilevelThresholderType;
import net.imglib2.ops.operation.iterableinterval.unary.multilevelthresholder.OtsuMultilevelThresholder;
import net.imglib2.ops.operation.iterableinterval.unary.multilevelthresholder.ThresholdValueCollection;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeModel;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.util.ImgPlusFactory;

/**
 * Multi-Level OTSU Thresholding
 *
 * @param <T> the pixel type of the input and output image
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 *
 * @author friedrichm, University of Konstanz
 */
public class MultilevelThresholderNodeModel<T extends RealType<T>> extends ImgPlusToImgPlusNodeModel<T, T> {

    static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dimselection", "X", "Y");
    }

    static SettingsModelIntegerBounded createNumberOfIntensities() {
        return new SettingsModelIntegerBounded("number_of_intensities", 256, 2, Integer.MAX_VALUE);
    }

    static SettingsModelIntegerBounded createNumberOfLevels() {
        return new SettingsModelIntegerBounded("number_of_levels", 3, 2, Integer.MAX_VALUE);
    }

    static SettingsModelString createThresholderModel() {
        return new SettingsModelString("thresholder", MultilevelThresholderType.OTSU.name());
    }

    private final SettingsModelIntegerBounded m_numberOfIntensities = createNumberOfIntensities();

    private final SettingsModelIntegerBounded m_numberOfLevels = createNumberOfLevels();

    private final SettingsModelString m_thresholder = createThresholderModel();

    /**
     * Constructor
     */
    protected MultilevelThresholderNodeModel() {
        super(createDimSelectionModel());
    }

    @Override
    protected void addSettingsModels(final List<SettingsModel> settingsModels) {
        settingsModels.add(m_thresholder);
        settingsModels.add(m_numberOfLevels);
        settingsModels.add(m_numberOfIntensities);
    }

    @Override
    protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<T>> op(final ImgPlus<T> imgPlus) {

        UnaryOutputOperation<ImgPlus<T>, ThresholdValueCollection> op = null;

        switch (Enum.valueOf(MultilevelThresholderType.class, m_thresholder.getStringValue())) {
            case OTSU:
                op =
                        new OtsuMultilevelThresholder<T, ImgPlus<T>>(m_numberOfLevels.getIntValue(),
                                m_numberOfIntensities.getIntValue());
                break;
            default:
                throw new IllegalArgumentException("Unknown thresholding method");
        }

        return Operations.wrap(new MultilevelThresholderOp<T, ImgPlus<T>, ImgPlus<T>>(op),
                               ImgPlusFactory.<T, T> get(imgPlus));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getMinDimensions() {
        return 2;
    }
}
