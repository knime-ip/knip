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
package org.knime.knip.base.nodes.filter.maxhomogenity;

import java.util.Arrays;
import java.util.List;

import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.exceptions.KNIPRuntimeException;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeModel;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.ops.img.MaxHomogenityOp;
import org.knime.knip.core.types.OutOfBoundsStrategyEnum;
import org.knime.knip.core.types.OutOfBoundsStrategyFactory;
import org.knime.knip.core.util.ImgPlusFactory;

/**
 * 
 * 
 * @param <T> the pixel type of the input and output image
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Lukas Siedentop (University of Konstanz)
 */
public class MaxHomogenityNodeModel<T extends RealType<T>> extends ImgPlusToImgPlusNodeModel<T, T> {

    protected static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dimselection", "X", "Y");
    }

    protected static SettingsModelDouble createLambdaModel() {
        return new SettingsModelDouble("lambda", 3);
    }

    protected static SettingsModelString createOutOfBoundsModel() {
        return new SettingsModelString("outofboundsstrategy", OutOfBoundsStrategyEnum.BORDER.toString());
    }

    protected static SettingsModelInteger createWindowSize() {
        return new SettingsModelInteger("windowsize", 15);
    }

    private final SettingsModelDouble m_lambda = createLambdaModel();

    private final SettingsModelString m_outOfBoundsStrategy = createOutOfBoundsModel();

    private final SettingsModelInteger m_windowSize = createWindowSize();

    protected MaxHomogenityNodeModel() {
        super(createDimSelectionModel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSettingsModels(final List<SettingsModel> settingsModels) {

        settingsModels.add(m_outOfBoundsStrategy);
        settingsModels.add(m_windowSize);
        settingsModels.add(m_lambda);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImgPlusCell<T> compute(final ImgPlusValue<T> cellValue) throws Exception {
        if ((m_windowSize.getIntValue() % 2) == 0) {
            throw new InvalidSettingsException("Only odd numbers are allowed");
        }

        return super.compute(cellValue);
    }

    @Override
    protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<T>> op(final ImgPlus<T> imgPlus) {

        final long[] spans = new long[2];
        Arrays.fill(spans, m_windowSize.getIntValue());

        if (m_dimSelection.getNumSelectedDimLabels(imgPlus) != 2) {
            throw new KNIPRuntimeException("image " + imgPlus.getName()
                    + " does not contain both of the two selected dimensions.");
        }

        //max homogenity op works only on 2d at the moment (may 2013)
        return Operations.wrap(new MaxHomogenityOp<T, ImgPlus<T>>(m_lambda.getDoubleValue(), spans,
                                       OutOfBoundsStrategyFactory.<T, ImgPlus<T>> getStrategy(m_outOfBoundsStrategy
                                               .getStringValue(), imgPlus.firstElement())), ImgPlusFactory
                                       .<T, T> get(imgPlus.firstElement()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getMinDimensions() {
        return 1;
    }
}
