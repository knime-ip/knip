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
package org.knime.knip.base.nodes.filter;

import java.util.Iterator;
import java.util.List;

import net.imglib2.algorithm.region.localneighborhood.Shape;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.BinaryOperation;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeModel;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.ops.iterable.SlidingShapeOpBinaryInside;
import org.knime.knip.core.ops.iterable.SlidingShapeOpUnaryInside;
import org.knime.knip.core.types.NeighborhoodType;
import org.knime.knip.core.types.OutOfBoundsStrategyFactory;
import org.knime.knip.core.util.ImgPlusFactory;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class AbstractSlidingWindowOperationNodeModel<T extends RealType<T>, V extends RealType<V>> extends
        ImgPlusToImgPlusNodeModel<T, V> {

    protected static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dimensions", "X", "Y");
    }

    protected SettingsModelInteger m_intervalExtend = SlidingWindowOperationNodeFactory.createBoxExtendModel();

    protected NeighborhoodType m_neighborhood;

    protected SettingsModelString m_neighborhoodType = SlidingWindowOperationNodeFactory
            .createNeighborhoodTypeNodeModel();

    protected SettingsModelString m_outOfBoundsStrategy = SlidingWindowOperationNodeFactory.createOutOfBoundsModel();

    protected SettingsModelBoolean m_speedUp = SlidingWindowOperationNodeFactory.createSpeedUpModel();

    public AbstractSlidingWindowOperationNodeModel() {
        super(createDimSelectionModel());
    }

    @Override
    protected void addSettingsModels(final List<SettingsModel> settingsModels) {
        settingsModels.add(m_intervalExtend);
        settingsModels.add(m_outOfBoundsStrategy);
        settingsModels.add(m_neighborhoodType);
        settingsModels.add(m_speedUp);
    }

    /**
     * simplifies the usage of {@link #getSlidingOperation(ImgPlus, RealType, Shape, OutOfBoundsFactory)}
     * 
     * @param op
     * @param type
     * @param neighborhood
     * @param outStrat
     * @return
     */
    protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<V>>
            defaultBinary(final BinaryOperation<Iterator<T>, T, V> op, final V type, final Shape neighborhood,
                          final OutOfBoundsFactory<T, ImgPlus<T>> outStrat) {

        return Operations
                .wrap(new SlidingShapeOpBinaryInside<T, V, ImgPlus<T>, ImgPlus<V>>(neighborhood, op, outStrat),
                      ImgPlusFactory.<T, V> get(type.createVariable()));

    }

    /**
     * simplifies the usage of {@link #getSlidingOperation(ImgPlus, RealType, Shape, OutOfBoundsFactory)}
     * 
     * @param op
     * @param type
     * @param neighborhood
     * @param outStrat
     * @return
     */
    protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<V>>
            defaultUnary(final UnaryOperation<Iterator<T>, V> op, final V type, final Shape neighborhood,
                         final OutOfBoundsFactory<T, ImgPlus<T>> outStrat) {

        return Operations.wrap(new SlidingShapeOpUnaryInside<T, V, ImgPlus<T>, ImgPlus<V>>(neighborhood, op, outStrat),
                               ImgPlusFactory.<T, V> get(type.createVariable()));
    }

    /**
     * Returns a var of the output type. in the case that the operation is used as SlindingWindow<T,T> just return
     * inType.createVariable(). In the case that you use it as <T,V> return the appropriate V e.g. BitType for local
     * thresholding operations.
     * 
     * @param inType
     * @return
     */
    protected abstract V getOutType(T inType);

    private UnaryOutputOperation<ImgPlus<T>, ImgPlus<V>> getSlidingOperation(final ImgPlus<T> img, final V type) {

        final Shape neighborhood =
                NeighborhoodType.getNeighborhood(NeighborhoodType.valueOf(m_neighborhoodType.getStringValue()),
                                                 m_intervalExtend.getIntValue());

        final OutOfBoundsFactory<T, ImgPlus<T>> outStrat =
                OutOfBoundsStrategyFactory.<T, ImgPlus<T>> getStrategy(m_outOfBoundsStrategy.getStringValue(),
                                                                       img.firstElement());

        return getSlidingOperation(img, type, neighborhood, outStrat);
    }

    /**
     * create a sliding window operation and return it. You can implement this directly e.g. to apply speed up
     * strategies like integral images ... or use the provided default methods (#defaultUnary() or #defaultBinary())
     * that only require a op to be used.
     */
    protected abstract UnaryOutputOperation<ImgPlus<T>, ImgPlus<V>>
            getSlidingOperation(ImgPlus<T> img, V type, Shape neighborhood, OutOfBoundsFactory<T, ImgPlus<T>> outStrat);

    @Override
    protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<V>> op(final ImgPlus<T> vin) {
        final V outType = getOutType(vin.firstElement().createVariable());
        return getSlidingOperation(vin, outType);
    }
}
