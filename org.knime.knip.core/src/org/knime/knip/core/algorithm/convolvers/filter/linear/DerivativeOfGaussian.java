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

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.DoubleAccess;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.ops.img.BinaryOperationAssignment;
import net.imglib2.ops.img.UnaryConstantRightAssignment;
import net.imglib2.ops.img.UnaryOperationAssignment;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.iterable.unary.Mean;
import net.imglib2.ops.operation.iterable.unary.Sum;
import net.imglib2.ops.operation.real.binary.RealAdd;
import net.imglib2.ops.operation.real.binary.RealMultiply;
import net.imglib2.ops.operation.real.unary.RealExp;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Fraction;

/**
 * An implementation of dog filters.
 *
 * @author Roy Liu, hornm
 */
public class DerivativeOfGaussian extends ArrayImg<DoubleType, DoubleAccess> {

    public DerivativeOfGaussian(final int supportRadius, final double theta, final double scale, final int ord) {
        super(new DoubleArray(ArrayImgFactory.numEntitiesRangeCheck(new long[]{supportRadius * 2 + 1,
                supportRadius * 2 + 1}, new Fraction(1, 1))), new long[]{supportRadius * 2 + 1, supportRadius * 2 + 1},
                new Fraction(1, 1));

        // create a Type that is linked to the container
        final DoubleType linkedType = new DoubleType(this);

        // pass it to the NativeContainer
        setLinkedType(linkedType);
        double sigma = (scale * supportRadius) / 3.0d;

        // create rotated point matrices
        Img<DoubleType> ptsMatrix =
                Operations.compute(new MatMul<DoubleType>(), FilterTools.createRotationMatrix(theta),
                                   FilterTools.createPointSupport(supportRadius));

        Img<DoubleType> ptsY =
                FilterTools.reshapeMatrix(supportRadius * 2 + 1, FilterTools.getVector(ptsMatrix, new int[]{0, 0}, 1));
        Img<DoubleType> ptsX =
                FilterTools.reshapeMatrix(supportRadius * 2 + 1, FilterTools.getVector(ptsMatrix, new int[]{1, 0}, 1));

        Img<DoubleType> gX = derivative(sigma, ptsX, ord);
        Img<DoubleType> gY = derivative(sigma, ptsY, 0);

        new BinaryOperationAssignment<DoubleType, DoubleType, DoubleType>(
                new RealMultiply<DoubleType, DoubleType, DoubleType>()).compute(gX, gY, this);

        // gX.eMul(gY).map(this, 0, 0, size(0), 0, 0, size(1));

        switch (ord) {

            case 0:
                double val =
                        1.0f / new Sum<DoubleType, DoubleType>().compute(this.cursor(), new DoubleType())
                                .getRealDouble();

                new UnaryConstantRightAssignment<DoubleType, DoubleType, DoubleType>(
                        new RealMultiply<DoubleType, DoubleType, DoubleType>())
                        .compute(this, new DoubleType(val), this);

                // uMul(1.0 / aSum());
                break;

            case 1:
                new UnaryConstantRightAssignment<DoubleType, DoubleType, DoubleType>(
                        new RealAdd<DoubleType, DoubleType, DoubleType>()).compute(this,
                                                                                   new Mean<DoubleType, DoubleType>()
                                                                                           .compute(this.cursor(),
                                                                                                    new DoubleType()),
                                                                                   this);
                // uAdd(-aMean());
                break;
        }

    }

    /**
     * Calculates a Gaussian derivative to the specified order.
     */
    final protected static Img<DoubleType>
            derivative(final double sigma, final Img<DoubleType> support, final int order) {

        float variance = (float)(sigma * sigma);

        // Get a working copy.

        Img<DoubleType> g = support.factory().create(support, new DoubleType());

        new BinaryOperationAssignment<DoubleType, DoubleType, DoubleType>(
                new RealMultiply<DoubleType, DoubleType, DoubleType>()).compute(support, support, g);

        new UnaryConstantRightAssignment<DoubleType, DoubleType, DoubleType>(
                new RealMultiply<DoubleType, DoubleType, DoubleType>()).compute(g, new DoubleType(-1.0f
                / (2.0f * variance)), g);

        new UnaryOperationAssignment<DoubleType, DoubleType>(new RealExp<DoubleType, DoubleType>()).compute(g, g);

        switch (order) {

            case 0:
                return g;

            case 1:
                Img<DoubleType> tmp = support.factory().create(support, new DoubleType());

                new UnaryConstantRightAssignment<DoubleType, DoubleType, DoubleType>(
                        new RealMultiply<DoubleType, DoubleType, DoubleType>()).compute(support, new DoubleType(-1.0f
                        / variance), tmp);

                new BinaryOperationAssignment<DoubleType, DoubleType, DoubleType>(
                        new RealMultiply<DoubleType, DoubleType, DoubleType>()).compute(tmp, g, g);

                return g;
                // return g.eMul(support.clone().uMul(-1.0d /
                // variance));

            default:
                throw new IllegalArgumentException("Order of derivative can only be zero or one");
        }
    }
    // public static void main(String[] args) {
    // Img<DoubleType> img = new DerivativeOfGaussian(20, 0, 1, 0);
    // new ImgNormalize<DoubleType>(0).manipulate(img);
    // AWTImageTools.showInFrame(img, "derivative of gaussian", 5);
    // }
}
