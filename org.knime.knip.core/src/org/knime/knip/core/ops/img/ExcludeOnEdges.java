/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */
package org.knime.knip.core.ops.img;

import java.util.HashSet;
import java.util.Set;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.view.Views;

/**
 * @author Slawek Mazur (University of Konstanz)
 *
 * @param <L>
 */
public class ExcludeOnEdges<L> implements
        UnaryOperation<RandomAccessibleInterval<LabelingType<L>>, RandomAccessibleInterval<LabelingType<L>>> {

    @Override
    public RandomAccessibleInterval<LabelingType<L>>
            compute(final RandomAccessibleInterval<LabelingType<L>> inLabeling,
                    final RandomAccessibleInterval<LabelingType<L>> outLabeling) {

        if (inLabeling.numDimensions() != 2) {
            throw new IllegalArgumentException("Exclude on edges works only on two dimensional images");
        }

        long[] dims = new long[inLabeling.numDimensions()];
        inLabeling.dimensions(dims);

        HashSet<Set<L>> indices = new HashSet<Set<L>>();

        RandomAccess<LabelingType<L>> outRndAccess = outLabeling.randomAccess();
        RandomAccess<LabelingType<L>> inRndAccess = inLabeling.randomAccess();

        Cursor<LabelingType<L>> cur = Views.iterable(inLabeling).cursor();

        long[] pos = new long[inLabeling.numDimensions()];

        for (int d = 0; d < dims.length; d++) {

            for (int i = 0; i < Math.pow(2, dims.length - 1); i++) {

                int offset = 0;
                for (int dd = 0; dd < dims.length; dd++) {
                    if (dd == d) {
                        offset++;
                        continue;
                    }
                    pos[dd] = (i % Math.pow(2, dd - offset + 1) == 0) ? 0 : dims[dd] - 1;
                }

                pos[d] = 0;
                for (int k = 0; k < dims[d]; k++) {
                    pos[d] = k;
                    inRndAccess.setPosition(pos);

                    if (0 != inRndAccess.get().size()) {
                        indices.add(inRndAccess.get());
                    }
                }
            }
        }

        while (cur.hasNext()) {
            cur.fwd();
            if (!indices.contains(cur.get())) {
                cur.localize(pos);
                outRndAccess.setPosition(pos);
                outRndAccess.get().clear();
                outRndAccess.get().addAll(cur.get());
            }
        }
        return outLabeling;

    }

    @Override
    public UnaryOperation<RandomAccessibleInterval<LabelingType<L>>, RandomAccessibleInterval<LabelingType<L>>> copy() {
        return new ExcludeOnEdges<L>();
    }

}
