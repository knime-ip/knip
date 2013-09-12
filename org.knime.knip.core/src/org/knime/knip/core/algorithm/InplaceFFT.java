/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.knime.knip.core.algorithm;

import org.knime.knip.core.data.algebra.Complex;

/*************************************************************************
 * Compilation: javac InplaceFFT.java Execution: java InplaceFFT N Dependencies: Complex.java
 * 
 * Compute the FFT of a length N complex sequence in-place. Uses a non-recursive version of the Cooley-Tukey FFT. Runs
 * in O(N log N) time.
 * 
 * Reference: Algorithm 1.6.1 in Computational Frameworks for the Fast Fourier Transform by Charles Van Loan.
 * 
 * 
 * Limitations ----------- - assumes N is a power of 2
 * 
 * 
 *************************************************************************/

public class InplaceFFT {

    // compute the FFT of x[], assuming its length is a power of 2
    public static Complex[] fft(final Complex[] x) {

        // check that length is a power of 2
        final int N = x.length;
        if (Integer.highestOneBit(N) != N) {
            throw new RuntimeException("N is not a power of 2");
        }

        // bit reversal permutation
        final int shift = 1 + Integer.numberOfLeadingZeros(N);
        for (int k = 0; k < N; k++) {
            final int j = Integer.reverse(k) >>> shift;
            if (j > k) {
                final Complex temp = x[j];
                x[j] = x[k];
                x[k] = temp;
            }
        }

        // butterfly updates
        for (int L = 2; L <= N; L = L + L) {
            for (int k = 0; k < (L / 2); k++) {
                final double kth = (-2 * k * Math.PI) / L;
                final Complex w = new Complex(Math.cos(kth), Math.sin(kth));
                for (int j = 0; j < (N / L); j++) {
                    final Complex tao = w.times(x[(j * L) + k + (L / 2)]);
                    x[(j * L) + k + (L / 2)] = x[(j * L) + k].minus(tao);
                    x[(j * L) + k] = x[(j * L) + k].plus(tao);
                }
            }
        }

        return x;
    }

    // compute the inverse FFT of x[], assuming its length is a power of 2
    public static Complex[] ifft(final Complex[] x) {
        final int N = x.length;

        // take conjugate
        for (int i = 0; i < N; i++) {
            x[i] = x[i].conjugate();
        }

        // compute forward FFT
        fft(x);

        // take conjugate again
        for (int i = 0; i < N; i++) {
            x[i] = x[i].conjugate();
        }

        // divide by N
        for (int i = 0; i < N; i++) {
            x[i] = x[i].times(1.0 / N);
        }

        return x;

    }

}
