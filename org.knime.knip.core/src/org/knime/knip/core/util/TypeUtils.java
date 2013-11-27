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
 * ---------------------------------------------------------------------
 *
 * Created on Nov 27, 2013 by hornm
 */
package org.knime.knip.core.util;

import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.Unsigned12BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

/**
 * Utility methods for {@link Type}s.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 */
public final class TypeUtils {

    private TypeUtils() {
        //utility class
    }

    /**
     * @param types
     * @return the type that fits the given types best
     */
    public static final RealType<?> leastFittingRealType(final RealType... types) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        boolean isIntegerType = true;
        for (RealType<?> t : types) {

            min = Math.min(t.getMinValue(), min);
            max = Math.max(t.getMaxValue(), max);
            if (!(t instanceof IntegerType)) {
                isIntegerType = false;
            }
        }
        return leastFittingRealType(min, max, isIntegerType);
    }

    /**
     * @param min
     * @param max
     * @param isIntegerType
     * @return the type that fits the given min-max range best
     */
    public static final RealType<?>
            leastFittingRealType(final double min, final double max, final boolean isIntegerType) {
        RealType<?> t;
        if (isIntegerType) {
            if (min >= 0) {
                //unsigned
                if (isWithinRange(t = new BitType(), min, max)) {
                    return t;
                } else if (isWithinRange(t = new UnsignedByteType(), min, max)) {
                    return t;
                } else if (isWithinRange(t = new Unsigned12BitType(), min, max)) {
                    return t;
                } else if (isWithinRange(t = new UnsignedShortType(), min, max)) {
                    return t;
                } else if (isWithinRange(t = new UnsignedIntType(), min, max)) {
                    return t;
                }
            } else {
                //signed
                if (isWithinRange(t = new ByteType(), min, max)) {
                    return t;
                } else if (isWithinRange(t = new ShortType(), min, max)) {
                    return t;
                } else if (isWithinRange(t = new IntType(), min, max)) {
                    return t;
                }
            }
            //default integer type
            return new LongType();

        } else {
            if (isWithinRange(t = new FloatType(), min, max)) {
                return t;
            }
            //default real type
            return new DoubleType();
        }

    }

    private static final boolean isWithinRange(final RealType t, final double min, final double max) {
        return min >= t.getMinValue() && max <= t.getMaxValue();
    }

}
