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
package org.knime.knip.core.types;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorExpWindowingFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.outofbounds.OutOfBoundsPeriodicFactory;
import net.imglib2.type.numeric.RealType;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class OutOfBoundsStrategyFactory {

    public static <T extends RealType<T>, IN extends RandomAccessibleInterval<T>> OutOfBoundsFactory<T, IN>
            getStrategy(final String strategy, final T val) {
        return getStrategy(strategy, val, val);
    }

    public static <T extends RealType<T>, O extends RealType<O>, IN extends RandomAccessibleInterval<T>>
            OutOfBoundsFactory<T, IN> getStrategy(final String strategy, final T val, final O refType) {
        return getStrategy(Enum.valueOf(OutOfBoundsStrategyEnum.class, strategy), val, val);
    }

    public static <T extends RealType<T>, IN extends RandomAccessibleInterval<T>> OutOfBoundsFactory<T, IN>
            getStrategy(final OutOfBoundsStrategyEnum strategyEnum, final T val) {
        return getStrategy(strategyEnum, val, val);
    }

    public static <T extends RealType<T>, O extends RealType<O>, IN extends RandomAccessibleInterval<T>>
            OutOfBoundsFactory<T, IN> getStrategy(final OutOfBoundsStrategyEnum strategyEnum, final T val,
                                                  final O refType) {
        final T inValue = val.createVariable();

        switch (strategyEnum) {
            case MIN_VALUE:
                inValue.setReal(refType.getMinValue());
                return new OutOfBoundsConstantValueFactory<T, IN>(inValue);
            case MAX_VALUE:
                inValue.setReal(refType.getMaxValue());
                return new OutOfBoundsConstantValueFactory<T, IN>(inValue);
            case ZERO_VALUE:
                inValue.setReal(0.0);
                return new OutOfBoundsConstantValueFactory<T, IN>(inValue);
            case MIRROR_SINGLE:
                return new OutOfBoundsMirrorFactory<T, IN>(OutOfBoundsMirrorFactory.Boundary.SINGLE);
            case MIRROR_DOUBLE:
                return new OutOfBoundsMirrorFactory<T, IN>(OutOfBoundsMirrorFactory.Boundary.DOUBLE);
            case PERIODIC:
                return new OutOfBoundsPeriodicFactory<T, IN>();
            case BORDER:
                return new OutOfBoundsBorderFactory<T, IN>();
            case FADE_OUT:
                return new OutOfBoundsMirrorExpWindowingFactory<T, IN>();
            default:
                throw new IllegalArgumentException("Unknown OutOfBounds factory type");
        }
    }
}
