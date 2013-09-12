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
package org.knime.knip.core.features;

import net.imglib2.Cursor;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory.Boundary;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.knime.knip.core.algorithm.convolvers.DirectConvolver;
import org.knime.knip.core.features.seg.ExtractOutlineImg;

/**
 * Input: Outline Image {@link ExtractOutlineImg}
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class CalculatePerimeter implements UnaryOutputOperation<Img<BitType>, DoubleType> {

    private final DirectConvolver<BitType, UnsignedShortType, UnsignedShortType> m_convolve;

    public CalculatePerimeter() {

        m_convolve = new DirectConvolver<BitType, UnsignedShortType, UnsignedShortType>();
    }

    private static synchronized Img<UnsignedShortType> getKernel() {
        @SuppressWarnings("unchecked")
        final ArrayImg<UnsignedShortType, ShortArray> img =
                (ArrayImg<UnsignedShortType, ShortArray>)new ArrayImgFactory<UnsignedShortType>().create(new long[]{3,
                        3}, new UnsignedShortType());

        final short[] storage = img.update(null).getCurrentStorageArray();

        storage[0] = 10;
        storage[1] = 2;
        storage[2] = 10;
        storage[3] = 2;
        storage[4] = 1;
        storage[5] = 2;
        storage[6] = 10;
        storage[7] = 2;
        storage[8] = 10;

        return img;
    }

    @Override
    public DoubleType compute(final Img<BitType> op, final DoubleType r) {
        Img<UnsignedShortType> img = null;
        try {
            img =
                    (Img<UnsignedShortType>)m_convolve
                            .compute(Views.extend(op, new OutOfBoundsMirrorFactory<BitType, Img<BitType>>(
                                    Boundary.SINGLE)), getKernel(), op.factory().imgFactory(new UnsignedShortType())
                                    .create(op, new UnsignedShortType()));
        } catch (final IncompatibleTypeException e) {
            // If factory not compatible
            img = new ArrayImgFactory<UnsignedShortType>().create(op, new UnsignedShortType());
        }
        final Cursor<UnsignedShortType> c = img.cursor();

        int catA = 0;
        int catB = 0;
        int catC = 0;

        while (c.hasNext()) {
            c.fwd();
            final int curr = c.get().get();

            switch (curr) {
                case 15:
                case 7:
                case 25:
                case 5:
                case 17:
                case 27:
                    catA++;
                    break;
                case 21:
                case 33:
                    catB++;
                    break;
                case 13:
                case 23:
                    catC++;
                    break;
            }

        }

        r.set(catA + (catB * Math.sqrt(2)) + (catC * ((1d + Math.sqrt(2)) / 2d)));

        return r;
    }

    @Override
    public UnaryOutputOperation<Img<BitType>, DoubleType> copy() {
        return new CalculatePerimeter();
    }

    @Override
    public UnaryObjectFactory<Img<BitType>, DoubleType> bufferFactory() {
        return new UnaryObjectFactory<Img<BitType>, DoubleType>() {

            @Override
            public DoubleType instantiate(final Img<BitType> a) {
                return new DoubleType();
            }
        };
    }
}
