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
import net.imglib2.ops.operation.complex.unary.ComplexExp;
import net.imglib2.ops.operation.real.binary.CombineToComplex;
import net.imglib2.ops.operation.real.binary.RealAdd;
import net.imglib2.ops.operation.real.binary.RealMultiply;
import net.imglib2.ops.operation.real.binary.RealPower;
import net.imglib2.ops.operation.real.unary.RealCopy;
import net.imglib2.type.numeric.complex.ComplexDoubleType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Fraction;

/**
 * An implementation of gabor filters.
 *
 * @author Roy Liu, hornm
 */
public class Gabor extends ArrayImg<ComplexDoubleType, DoubleAccess> {

    public Gabor(final int supportRadius, //
                 final double theta, final double scale, //
                 final double frequency, final double elongation) {
        super(new DoubleArray(ArrayImgFactory.numEntitiesRangeCheck(new long[]{supportRadius * 2 + 1,
                supportRadius * 2 + 1}, new Fraction(2, 1))), new long[]{supportRadius * 2 + 1, supportRadius * 2 + 1}, new Fraction(2, 1));

        // create a Type that is linked to the container
        final ComplexDoubleType linkedType = new ComplexDoubleType(this);

        // pass it to the NativeContainer
        setLinkedType(linkedType);

        // create rotated point matrices
        Img<DoubleType> ptsMatrix =
                Operations.compute(new MatMul<DoubleType>(), FilterTools.createRotationMatrix(theta),
                                   FilterTools.createPointSupport(supportRadius));

        Img<DoubleType> ptsY =
                FilterTools.reshapeMatrix(supportRadius * 2 + 1, FilterTools.getVector(ptsMatrix, new int[]{0, 0}, 1));
        Img<DoubleType> ptsX =
                FilterTools.reshapeMatrix(supportRadius * 2 + 1, FilterTools.getVector(ptsMatrix, new int[]{1, 0}, 1));

        final float k = (float)(scale * supportRadius / elongation);

        final float a = -4.0f / (k * k);

        new UnaryConstantRightAssignment<DoubleType, DoubleType, DoubleType>(
                new RealPower<DoubleType, DoubleType, DoubleType>()).compute(ptsX, new DoubleType(2.0d), ptsX);

        new UnaryConstantRightAssignment<DoubleType, DoubleType, DoubleType>(
                new RealMultiply<DoubleType, DoubleType, DoubleType>()).compute(ptsX, new DoubleType(
                (1.0d / (elongation * elongation))), ptsX);

        Img<DoubleType> tmp = ptsY.factory().create(ptsY, new DoubleType());

        new UnaryOperationAssignment<DoubleType, DoubleType>(new RealCopy<DoubleType, DoubleType>()).compute(ptsY, tmp);

        new UnaryConstantRightAssignment<DoubleType, DoubleType, DoubleType>(
                new RealPower<DoubleType, DoubleType, DoubleType>()).compute(tmp, new DoubleType(2.0d), tmp);

        new BinaryOperationAssignment<DoubleType, DoubleType, DoubleType>(
                new RealAdd<DoubleType, DoubleType, DoubleType>()).compute(tmp, ptsX, ptsX);

        new UnaryConstantRightAssignment<DoubleType, DoubleType, DoubleType>(
                new RealMultiply<DoubleType, DoubleType, DoubleType>()).compute(ptsX, new DoubleType(a), ptsX);

        final double b = (float)(2 * Math.PI * frequency / k);

        new UnaryConstantRightAssignment<DoubleType, DoubleType, DoubleType>(
                new RealMultiply<DoubleType, DoubleType, DoubleType>()).compute(ptsY, new DoubleType(b), ptsY);

        new BinaryOperationAssignment<DoubleType, DoubleType, ComplexDoubleType>(
                new CombineToComplex<DoubleType, DoubleType, ComplexDoubleType>()).compute(ptsX, ptsY, this);

        new UnaryOperationAssignment<ComplexDoubleType, ComplexDoubleType>(
                new ComplexExp<ComplexDoubleType, ComplexDoubleType>()).compute(this, this);
    }

}
