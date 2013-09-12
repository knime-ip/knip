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
package org.knime.knip.core.algorithm.logtrackmate;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.RandomAccess;
import net.imglib2.algorithm.fft.FourierConvolution;
import net.imglib2.algorithm.math.PickImagePeaks;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;

import org.knime.knip.core.algorithm.logtrackmate.SubPixelLocalization.LocationType;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LoGDetectorOp<T extends RealType<T> & NativeType<T>> implements
        UnaryOutputOperation<ImgPlus<T>, ImgPlus<BitType>> {

    private String baseErrorMessage = "LogDetector: ";

    private double radius;

    private double threshold;

    private boolean doSubPixelLocalization;

    protected List<Spot> spots = new ArrayList<Spot>();

    private int numDimensions;

    public LoGDetectorOp(final double radius, final double threshold, final boolean doSubPixelLocalization) {
        this.radius = radius;
        this.threshold = threshold;
        this.doSubPixelLocalization = doSubPixelLocalization;
    }

    @Override
    public ImgPlus<BitType> compute(final ImgPlus<T> input, final ImgPlus<BitType> output) {

        numDimensions = input.numDimensions();

        Img<T> temp = input;

        double sigma = radius / Math.sqrt(numDimensions);
        // Turn it in pixel coordinates
        final double[] calibration = new double[input.numDimensions()];
        input.calibration(calibration);

        double[] sigmas = new double[numDimensions];
        for (int i = 0; i < sigmas.length; i++) {
            sigmas[i] = sigma / calibration[i];
        }

        ImgFactory<FloatType> factory = new ArrayImgFactory<FloatType>();
        Img<FloatType> gaussianKernel = FourierConvolution.createGaussianKernel(factory, sigmas);
        FourierConvolution<T, FloatType> fConvGauss;
        fConvGauss =
                new FourierConvolution<T, FloatType>(temp, gaussianKernel, new ArrayImgFactory<ComplexFloatType>());
        if (!fConvGauss.checkInput() || !fConvGauss.process()) {
            baseErrorMessage += "Fourier convolution with Gaussian failed:\n" + fConvGauss.getErrorMessage();
        }
        temp = fConvGauss.getResult();

        Img<FloatType> laplacianKernel = createLaplacianKernel();
        FourierConvolution<T, FloatType> fConvLaplacian;
        fConvLaplacian =
                new FourierConvolution<T, FloatType>(temp, laplacianKernel, new ArrayImgFactory<ComplexFloatType>());
        if (!fConvLaplacian.checkInput() || !fConvLaplacian.process()) {
            baseErrorMessage += "Fourier Convolution with Laplacian failed:\n" + fConvLaplacian.getErrorMessage();
            return null;
        }
        temp = fConvLaplacian.getResult();

        PickImagePeaks<T> peakPicker = new PickImagePeaks<T>(temp);
        double[] suppressionRadiuses = new double[input.numDimensions()];
        for (int i = 0; i < numDimensions; i++) {
            suppressionRadiuses[i] = radius / calibration[i];
        }
        peakPicker.setSuppression(suppressionRadiuses); // in pixels
        peakPicker.setAllowBorderPeak(true);

        if (!peakPicker.checkInput() || !peakPicker.process()) {
            baseErrorMessage += "Could not run the peak picker algorithm:\n" + peakPicker.getErrorMessage();
            return null;
        }

        // Get peaks location and values
        final ArrayList<long[]> centers = peakPicker.getPeakList();
        final RandomAccess<T> cursor = temp.randomAccess();
        // Prune values lower than threshold
        List<SubPixelLocalization<T>> peaks = new ArrayList<SubPixelLocalization<T>>();
        final List<T> pruned_values = new ArrayList<T>();
        final LocationType specialPoint = LocationType.MAX;
        for (int i = 0; i < centers.size(); i++) {
            long[] center = centers.get(i);
            cursor.setPosition(center);
            T value = cursor.get().copy();
            if (value.getRealDouble() < threshold) {
                break; // because peaks are sorted, we can exit loop here
            }
            SubPixelLocalization<T> peak = new SubPixelLocalization<T>(center, value, specialPoint);
            peaks.add(peak);
            pruned_values.add(value);
        }

        // Do sub-pixel localization
        if (doSubPixelLocalization) {
            // Create localizer and apply it to the list. The list object will
            // be updated
            final QuadraticSubpixelLocalization<T> locator = new QuadraticSubpixelLocalization<T>(temp, peaks);
            locator.setNumThreads(1); // Since the calls to a detector are
                                      // already multi-threaded.
            locator.setCanMoveOutside(true);
            if (!locator.checkInput() || !locator.process()) {
                baseErrorMessage += locator.getErrorMessage();
                return null;
            }
        }

        // Create spots
        spots.clear();
        RandomAccess<BitType> randomAccess = output.randomAccess();
        for (int j = 0; j < peaks.size(); j++) {

            SubPixelLocalization<T> peak = peaks.get(j);
            double[] coords = new double[numDimensions];
            for (int i = 0; i < numDimensions; i++) {
                coords[i] = peak.getDoublePosition(i) * calibration[i];
                randomAccess.setPosition((long)coords[i], i);
            }

            randomAccess.get().set(true);

        }

        return output;

    }

    @Override
    public UnaryObjectFactory<ImgPlus<T>, ImgPlus<BitType>> bufferFactory() {
        return new UnaryObjectFactory<ImgPlus<T>, ImgPlus<BitType>>() {

            @Override
            public ImgPlus<BitType> instantiate(final ImgPlus<T> img) {
                try {
                    return new ImgPlus<BitType>(img.factory().imgFactory(new BitType()).create(img, new BitType()), img);
                } catch (IncompatibleTypeException e) {
                    return new ImgPlus<BitType>(new ArrayImgFactory<BitType>().create(img, new BitType()), img);
                }

            }
        };
    }

    /*
     * PRIVATE METHODS
     */
    private Img<FloatType> createLaplacianKernel() {
        final ImgFactory<FloatType> factory = new ArrayImgFactory<FloatType>();
        int numDim = numDimensions;
        Img<FloatType> laplacianKernel = null;
        if (numDim == 3) {
            final float laplacianArray[][][] =
                    new float[][][]{{{0, -1 / 18, 0}, {-1 / 18, -1 / 18, -1 / 18}, {0, -1 / 18, 0}},
                            {{-1 / 18, -1 / 18, -1 / 18}, {-1 / 18, 1, -1 / 18}, {-1 / 18, -1 / 18, -1 / 18}},
                            {{0, -1 / 18, 0}, {-1 / 18, -1 / 18, -1 / 18}, {0, -1 / 18, 0}}}; // laplace kernel found here:
                                                                                              // http://en.wikipedia.org/wiki/Discrete_Laplace_operator
            laplacianKernel = factory.create(new int[]{3, 3, 3}, new FloatType());
            quickKernel3D(laplacianArray, laplacianKernel);
        } else if (numDim == 2) {
            final float laplacianArray[][] =
                    new float[][]{{-1 / 8, -1 / 8, -1 / 8}, {-1 / 8, 1, -1 / 8}, {-1 / 8, -1 / 8, -1 / 8}}; // laplace kernel found here:
                                                                                                            // http://en.wikipedia.org/wiki/Discrete_Laplace_operator
            laplacianKernel = factory.create(new int[]{3, 3}, new FloatType());
            quickKernel2D(laplacianArray, laplacianKernel);
        }
        return laplacianKernel;
    }

    private static void quickKernel2D(final float[][] vals, final Img<FloatType> kern) {
        final RandomAccess<FloatType> cursor = kern.randomAccess();
        final int[] pos = new int[2];

        for (int i = 0; i < vals.length; ++i) {
            for (int j = 0; j < vals[i].length; ++j) {
                pos[0] = i;
                pos[1] = j;
                cursor.setPosition(pos);
                cursor.get().set(vals[i][j]);
            }
        }
    }

    private static void quickKernel3D(final float[][][] vals, final Img<FloatType> kern) {
        final RandomAccess<FloatType> cursor = kern.randomAccess();
        final int[] pos = new int[3];

        for (int i = 0; i < vals.length; ++i) {
            for (int j = 0; j < vals[i].length; ++j) {
                for (int k = 0; k < vals[j].length; ++k) {
                    pos[0] = i;
                    pos[1] = j;
                    pos[2] = k;
                    cursor.setPosition(pos);
                    cursor.get().set(vals[i][j][k]);
                }
            }
        }
    }

    public List<Spot> getSpots() {
        return spots;
    }

    @Override
    public UnaryOutputOperation<ImgPlus<T>, ImgPlus<BitType>> copy() {
        return new LoGDetectorOp<T>(radius, threshold, doSubPixelLocalization);
    }

}
