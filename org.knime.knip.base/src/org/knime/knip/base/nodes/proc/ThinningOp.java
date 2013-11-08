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
 * Created on 07.11.2013 by Andreas
 */
package org.knime.knip.base.nodes.proc;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 *
 * @author Andreas
 */
public class ThinningOp<T extends RealType<T>> implements UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>>  {

    public ThinningOp()
    {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RandomAccessibleInterval<T> compute(final RandomAccessibleInterval<T> input, final RandomAccessibleInterval<T> output) {
        final ImgFactory< BitType > imgFactory = new ArrayImgFactory< BitType >( );
        long[] inpSize = new long[input.numDimensions()];
        input.dimensions(inpSize);
        Img<BitType> current = imgFactory.create(inpSize, new BitType());
        Img<BitType> next = imgFactory.create(inpSize, new BitType());

        Cursor<BitType> inputCursor = (Cursor<BitType>)Views.flatIterable(input).cursor();
        Cursor<BitType> outputCursor = (Cursor<BitType>)Views.flatIterable(output).cursor();
        Cursor<BitType> currentCursor = Views.flatIterable(current).cursor();
        Cursor<BitType> nextCursor = Views.flatIterable(next).cursor();

        while (inputCursor.hasNext())
        {
            boolean val = inputCursor.next().get();
            currentCursor.next().set(val);
            nextCursor.next().set(val);
        }

        boolean changed = false;
        RandomAccess<BitType> currAccess = current.randomAccess();
        RandomAccess<BitType> nextAccess = next.randomAccess();
        while(!changed)
        {
            changed = false;
            for(int x = 1; x < current.dimension(0) - 1; ++x) {
                for(int y = 1; y < current.dimension(1) - 1; ++y)
                {
                    int neighbours = trueNeighbours(currAccess, x, y);
                    nextAccess.setPosition(new int[]{x,y});
                    if(neighbours > 2 && neighbours < 6)
                    {
                        nextAccess.get().set(false);
                        changed = true;
                    } else {
                        nextAccess.get().set(true);
                    }
                }
            }
            RandomAccess<BitType> temp = nextAccess;
            currAccess = temp;
            nextAccess = currAccess;
        }

        currentCursor.reset();
        while (outputCursor.hasNext())
        {
            boolean val = currentCursor.next().get();
            outputCursor.next().set(val);
        }

        return output;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> copy() {
        // TODO Auto-generated method stub
        return null;
    }

    private int trueNeighbours(final RandomAccess<BitType> access, final int x, final int y)
    {
        int result = 0;
        access.setPosition(new int[]{x-1, y-1});
        if(access.get().get()) {
            result++;
        }

        access.setPosition(new int[]{x, y-1});
        if(access.get().get()) {
            result++;
        }

        access.setPosition(new int[]{x+1, y-1});
        if(access.get().get()) {
            result++;
        }

        access.setPosition(new int[]{x-1, y});
        if(access.get().get()) {
            result++;
        }

        access.setPosition(new int[]{x+1, y});
        if(access.get().get()) {
            result++;
        }

        access.setPosition(new int[]{x-1, y+1});
        if(access.get().get()) {
            result++;
        }

        access.setPosition(new int[]{x, y+1});
        if(access.get().get()) {
            result++;
        }

        access.setPosition(new int[]{x+1, y+1});
        if(access.get().get()) {
            result++;
        }

        return result;
    }

}
