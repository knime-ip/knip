/**
 * <p>
 * Copyright (C) 2008 Roy Liu, The Regents of the University of California <br />
 * All rights reserved.
 * </p>
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * </p>
 * <ul>
 * <li>Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.</li>
 * <li>Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.</li>
 * <li>Neither the name of the author nor the names of any contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.</li>
 * </ul>
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * </p>
 */

package org.knime.knip.core.algorithm.convolvers.filter.linear;

import net.imglib2.Cursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.DoubleAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Fraction;

/**
 * An implementation of LoG filters.
 *
 * @author Laurant Abouchar
 */
public class LaplacianOfGaussian extends ArrayImg<DoubleType, DoubleAccess> {

    /**
     * Default constructor.
     *
     * @param supportRadius the support radius.
     * @param sigma
     * @param nrDimensions
     * @return N-D Laplacian of Gaussian (N = nrDimensions) with standard deviation sigma, centered in the (hyper)-cube of edge (supportRadius*2+1).
     */
    public static LaplacianOfGaussian create(final int supportRadius, final double sigma, final int nrDimensions) {
        // In order to call super(), the dimensions of the nd-array have to be determined first, hence this code followed by the function create().
        final int size = Math.max(3, (2 * (int)(supportRadius + 0.5) + 1));
        long[] dims = new long[nrDimensions];
        for (int d = 0; d < nrDimensions; d++) {
            dims[d] = size;
        }
        return new LaplacianOfGaussian(supportRadius, sigma, dims);
    }

    private LaplacianOfGaussian(final int supportRadius, final double sigma, final long[] dims) {
        super(new DoubleArray(ArrayImgFactory.numEntitiesRangeCheck(dims, new Fraction(1, 1))), dims,
                new Fraction(1, 1));
        //
        // IMPLEMENTATION
        // An n-dimensional Laplacian of Gaussian can be expressed as the product of 4 factors:
        //  1) 1/(sigma^4); constant term independent of dimension or position.
        //  2) 1/(sqrt(2*pi)*sigma)^d; where d is the number of dimensions.
        //  3) sum_i (x_i^2 - sigma^2); a sum of quadratic terms over dimensions, hence position and dimension dependent.
        //  4) prod_i exp( - [x_i^2 / (2*sigma^2) ] ); a product of exponentials over dimensions, hence position and dimension dependent.
        //
        // Term 1 is constant, and is trivial to compute. Similarly, term 2 only needs the knowledge of the number of dimensions to be computed and is trivial.
        // Since term 2 can be expressed as a product over dimensions, it is computed jointly with term 4.
        // For terms 3 and 4, a cursor iterating over all positions is used. At every position, a loop over dimensions is used to compose terms 3 and 4 separately.
        // The sum from term 3 is computed directly in this loop.
        // The factors from the product of term 4 and term 2 are pre-computed in the variable 'kernel' that is a function of position. They are looked up and multiplied inside the loop.

        int nrDimensions = dims.length;

        // Create kernel containing possible factors for term 2 and 4.
        double[] kernel = createPartOfLaplacianOfGaussianKernel1DDouble(supportRadius, sigma);

        // create a Type that is linked to the container
        final DoubleType linkedType = new DoubleType(this);

        // pass it to the native container
        setLinkedType(linkedType);

        // Initialize values that are reused in the loop.
        double sigma_sqd = sigma*sigma;
        double term1 = 1.0f/(sigma_sqd*sigma_sqd); // Term 1.
        int center = supportRadius; // Location of the center of kernel.

        Cursor<DoubleType> cursor = localizingCursor();
        while (cursor.hasNext()) {
            cursor.fwd();
            double result = term1; // Term 1
            double sum_squares = 0;
            for (int d = 0; d < nrDimensions; d++) {
                int pos = cursor.getIntPosition(d);
                sum_squares += ((pos-center)*(pos-center) - sigma_sqd); // Term 3
                result *= kernel[pos]; // result is [ Term1 * (d-1 first factors of term 2 & 4) ] and multiplied with factor #d of terms 2 & 4.
            }
            result *= sum_squares; // Multiply term 3 with all the others.
            cursor.get().set(result);
        }

    }

    /**
     * @param supportRadius : Radius of the support for the final kernel. The kernel will have support 2*supportRadius+1.
     * @param sigma : Standard deviation of the Gaussian.
     * @return : 1D kernel of size 2*supportSize+1 containing the values for a Laplacian of Gaussian of std sigma with origin at position supportSize.
     */
    public static double[] createPartOfLaplacianOfGaussianKernel1DDouble( final int supportRadius, final double sigma )
    {
        // Note that this is only the sub-part of the LoG kernel which's outer product along each dimension needs to be multiplied by 1 over sigma^4 and a sum of quadratic term in each dimension.
        int size = 3; // Minimum width of the kernel.
        final double[] kernelLoG;

        if ( sigma <= 0 )
        {
            kernelLoG = new double[ 3 ];
            kernelLoG[ 1 ] = 1;
        }
        else
        {

            size = Math.max( 3, ( 2 * ( int )( supportRadius + 0.5) + 1 ) );

            final double two_sq_sigma = 2 * sigma * sigma;
            final double normalizing_const = Math.sqrt(2 * Math.PI) * sigma; // Term 2 from above.
            kernelLoG = new double[ size ];

            for ( int x = size / 2; x >= 0; --x )
            {
                final double val = Math.exp( -( x * x ) / two_sq_sigma )/normalizing_const;

                kernelLoG[ size / 2 - x ] = val;
                kernelLoG[ size / 2 + x ] = val;
            }
        }


        return kernelLoG;
    }
}