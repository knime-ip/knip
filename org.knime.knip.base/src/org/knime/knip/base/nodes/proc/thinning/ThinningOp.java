/*
* ------------------------------------------------------------------------
*
* Copyright (C) 2003 - 2013
* University of Konstanz, Germany and
* KNIME GmbH, Konstanz, Germany
* Website: http://www.knime.org; Email: contact@knime.org
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License, Version 3, as
* published by the Free Software Foundation.
*
* This program is distributed in the hope that it will be useful, but
* WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see <http://www.gnu.org/licenses>.
*
* Additional permission under GNU GPL version 3 section 7:
*
* KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
* Hence, KNIME and ECLIPSE are both independent programs and are not
* derived from each other. Should, however, the interpretation of the
* GNU GPL Version 3 ("License") under any applicable laws result in
* KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
* you the additional permission to use and propagate KNIME together with
* ECLIPSE with only the license terms in place for ECLIPSE applying to
* ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
* license terms of ECLIPSE themselves allow for the respective use and
* propagation of ECLIPSE together with KNIME.
*
* Additional permission relating to nodes for KNIME that extend the Node
* Extension (and in particular that are based on subclasses of NodeModel,
* NodeDialog, and NodeView) and that only interoperate with KNIME through
* standard APIs ("Nodes"):
* Nodes are deemed to be separate and independent programs and to not be
* covered works. Notwithstanding anything to the contrary in the
* License, the License does not apply to Nodes, you are not required to
* license Nodes under the License, and you are granted a license to
* prepare and propagate Nodes, in each case even if such Nodes are
* propagated with or for interoperation with KNIME. The owner of a Node
* may freely choose the license terms applicable to such Node, including
* when such Node is propagated with or for interoperation with KNIME.
* ---------------------------------------------------------------------
*
* Created on 07.11.2013 by Daniel
*/
package org.knime.knip.base.nodes.proc.thinning;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.knime.knip.base.nodes.proc.thinning.strategies.ThinningStrategy;

/**
 *
 * @param <T> extends RealType<T>
 * @author Andreas Burger, University of Konstanz
 */
public class ThinningOp<T extends RealType<T>> implements
        UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> {

    private boolean m_Foreground = true;

    private boolean m_Background = false;

    private ThinningStrategy m_Strategy;

    /**
     * Instanciate a new ThinningOp using the given strategy and considering the given boolean value as foreground.
     *
     * @param strategy The Strategy to use
     * @param foreground Boolean value of foreground pixels.
     */
    public ThinningOp(final ThinningStrategy strategy, final boolean foreground) {
        m_Strategy = strategy;
        m_Foreground = foreground;
        m_Background = !foreground;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RandomAccessibleInterval<T> compute(final RandomAccessibleInterval<T> input,
                                               final RandomAccessibleInterval<T> output) {

        // NOTE: DO NOT USE NON-BITTYPE IMAGES!
        // In the thinning node, this is taken care of by the factory.

        ImgFactory<BitType> fac = new ArrayImgFactory<BitType>();

        // Create new images to store the thinning image in each iteration.
        // These images are swapped each iteration.
        Img<BitType> img1 = fac.create(input, new BitType());
        Img<BitType> img2 = fac.create(input, new BitType());

        IterableInterval<BitType> it1 = Views.iterable(img1);
        // Initially, we need to copy the input to the first Image.
        copy((RandomAccessibleInterval<BitType>)input, it1);

        // Extend the images in order to be able to iterate care-free later.
        RandomAccessible<BitType> ra1 = Views.extendBorder(img1);
        Cursor<BitType> firstCursor = it1.localizingCursor();

        RandomAccessible<BitType> ra2 = Views.extendBorder(img2);
        IterableInterval<BitType> it2 = Views.iterable(img2);
        Cursor<BitType> secondCursor = it2.localizingCursor();

        // Create pointers to the current and next cursor and set them to Image 1 and 2 respectively.
        Cursor<BitType> currentCursor, nextCursor;
        currentCursor = firstCursor;
        RandomAccessible<BitType> currRa = ra1;
        nextCursor = secondCursor;

        // The main loop.
        boolean changes = true;
        int i = 0;
        // Until no more changes, do:
        while (changes) {
            changes = false;
            // This For-Loop makes sure, that iterations only end on full cycles (as defined by the strategies).
            for (int j = 0; j < m_Strategy.getIterationsPerCycle(); ++j) {
                // For each pixel in the image.
                while (currentCursor.hasNext()) {
                    // Move both cursors
                    currentCursor.fwd();
                    nextCursor.fwd();
                    // Get the position of the current cursor.
                    long[] coordinates = new long[currentCursor.numDimensions()];
                    currentCursor.localize(coordinates);

                    // Copy the value of the image currently operated upon.
                    boolean curr = currentCursor.get().get();
                    nextCursor.get().set(curr);

                    // Only foreground pixels may be thinned
                    if (curr == m_Foreground) {

                        // Ask the strategy whether to flip the foreground pixel or not.
                        boolean flip = m_Strategy.removePixel(coordinates, currRa);

                        // If yes - change and keep track of the change.
                        if (flip) {
                            nextCursor.get().set(m_Background);
                            changes = true;
                        }
                    }
                }
                // One step of the cycle is finished, notify the strategy.
                m_Strategy.afterCycle();

                // Reset the cursors to the beginning and assign pointers for the next iteration.
                currentCursor.reset();
                nextCursor.reset();
                Cursor<BitType> temp = currentCursor;

                // Keep track of the most recent image. Needed for output.
                if (currRa == ra1) {
                    currRa = ra2;
                } else {
                    currRa = ra1;
                }
                currentCursor = nextCursor;
                nextCursor = temp;

                // Keep track of iterations.
                ++i;
            }
        }

        // Depending on the iteration count, the final image is either in ra1 or ra2. Copy it to output.
        if (i % 2 == 1) {
            copy(ra2, Views.iterable((RandomAccessibleInterval<BitType>)output));
        } else {
            copy(ra1, Views.iterable((RandomAccessibleInterval<BitType>)output));
        }

        return output;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> copy() {
        return new ThinningOp<T>(m_Strategy, m_Foreground);
    }

    private void copy(final RandomAccessible<BitType> source, final IterableInterval<BitType> target) {
        Cursor<BitType> targetCursor = target.localizingCursor();
        RandomAccess<BitType> sourceAccess = source.randomAccess();
        while (targetCursor.hasNext()) {
            targetCursor.fwd();
            sourceAccess.setPosition(targetCursor);
            targetCursor.get().set(sourceAccess.get());
        }
    }

}