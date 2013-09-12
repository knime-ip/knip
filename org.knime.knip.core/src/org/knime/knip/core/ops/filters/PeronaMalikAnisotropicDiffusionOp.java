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
package org.knime.knip.core.ops.filters;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.pde.PeronaMalikAnisotropicDiffusion;
import net.imglib2.algorithm.pde.PeronaMalikAnisotropicDiffusion.DiffusionFunction;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.iterableinterval.unary.IterableIntervalCopy;
import net.imglib2.ops.operation.subset.views.ImgView;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class PeronaMalikAnisotropicDiffusionOp<T extends RealType<T> & NativeType<T>, I extends RandomAccessibleInterval<T>>
        implements UnaryOperation<I, I> {

    private final double m_deltat;

    // Iterations
    private final int m_n;

    // used Difussion Function
    private final DiffusionFunction m_fun;

    // number of threads
    private final int m_numThreads;

    /**
     * 
     * Constructs a wrapping operation to execute the (elsewhere implemented) Perona & Malik Anisotropic Diffusion
     * scheme. See {@link PeronaMalikAnisotropicDiffusion}.
     * 
     * @param deltat the integration constant for the numerical integration scheme. Typically less that 1.
     * @param n the number of Iterations
     * @param fun the diffusion function to be used
     * @param threads The number of the threads to be used. Usually 1.
     */
    public PeronaMalikAnisotropicDiffusionOp(final double deltat, final int n, final DiffusionFunction fun,
                                             final int threads) {
        this.m_deltat = deltat;
        this.m_n = n;
        this.m_fun = fun;
        this.m_numThreads = threads;
    }

    @Override
    public I compute(final I input, final I output) {

        // this is ugly and a hack but needed as the implementation of
        // this
        // algorithms doesn't accept the input img
        final ImgView<T> out = new ImgView<T>(output, new ArrayImgFactory<T>());

        new IterableIntervalCopy<T>().compute(Views.iterable(input), out);

        // build a new diffusion scheme
        final PeronaMalikAnisotropicDiffusion<T> diff =
                new PeronaMalikAnisotropicDiffusion<T>(out, this.m_deltat, this.m_fun);

        // set threads //TODO: noch ne "auto"-funktion einbauen, das das
        // autmatisch passiert? bis der fehler gefunden ist...
        diff.setNumThreads(this.m_numThreads);

        // do the process n times -> see {@link
        // PeronaMalikAnisotropicDiffusion}
        for (int i = 0; i < this.m_n; i++) {
            diff.process();
        }

        return output;
    }

    @Override
    public UnaryOperation<I, I> copy() {
        return new PeronaMalikAnisotropicDiffusionOp<T, I>(this.m_deltat, this.m_n, this.m_fun, this.m_numThreads);
    }

}
