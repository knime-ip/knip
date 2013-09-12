/*
 * Copyright 2010, 2011 Institut Pasteur.
 *
 * This file is part of ICY.
 *
 * ICY is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ICY is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ICY. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * edited by
 * Michael Zinsmaier, University of Konstanz
 *
 * I removed some methods that we didn't need. Thre remaining code is
 * the same as in the original implementation. The original file can be
 * found in the ICY release of the Spot Detection Plugin.
 */

package org.knime.knip.core.ops.spotdetection.icybased;

/**
 * 
 * @author Nicolas Chenouard, Fabrice de Chaumont
 * 
 */
public class B3SplineUDWT {

    /**
     * Compute the maximum feasible scale for a given 3D image size for as scale i, the minimum size of the image is 5
     * +4*(2^(i-1)-1) along each direction
     * 
     * @param width
     * @param height
     * @param depth
     * @return
     */
    int computeMaximumScale(final int width, final int height, final int depth) {
        // compute the smallest size along the 3 dimensions
        int minSize = width;
        if (height < minSize) {
            minSize = height;
        }
        if (depth < minSize) {
            minSize = depth;
        }
        if (minSize < 5) {
            return 0;
        }
        int maxScale = 1;
        while ((5 + ((Math.pow(2, (maxScale + 1) - 1) - 1) * 4)) < minSize) {
            // increase the scale until the bound exceeds the
            // smallest size
            maxScale++;
        }
        return maxScale;
    }

    /**
     * Compute the maximum feasible scale for a given 2D image size for as scale i, the minimum size of the image is 5
     * +4*(2^(i-1)-1) along each direction
     * 
     * @param width
     * @param height
     * @return
     */
    int computeMaximumScale2D(final int width, final int height) {
        // compute the smallest size along the 2 dimensions
        int minSize = width;
        if (height < minSize) {
            minSize = height;
        }
        if (minSize < 5) {
            return 0;
        }
        int maxScale = 1;
        while ((5 + ((Math.pow(2, (maxScale + 1) - 1)) * 4)) < minSize) {
            // increase the scale until the bound exceeds the
            // smallest size
            maxScale++;
        }
        return maxScale;
    }

    public boolean isNumberOfScaleOkForImage2D(final int width, final int height, final int numScales) {
        final int minSize = getMinSize(numScales); // 5+(int)(Math.pow(2,
        // numScales-1))*4;//compute
        // the minimum size for
        // numScales scales
        if ((width < minSize) || (height < minSize))// image size is too
        // small, return an
        // exception
        {
            return false;
        }
        return true;
    }

    public boolean isNumberOfScaleOkForImage3D(final int width, final int height, final int depth, final int numScales) {
        final int minSize = getMinSize(numScales); // 5+(int)(Math.pow(2,
        // numScales-1))*4;//compute
        // the minimum size for
        // numScales scales
        if ((width < minSize) || (height < minSize) || (depth < minSize))// image
        // size
        // is
        // too
        // small,
        // return
        // an
        // exception
        {
            return false;
        }
        return true;
    }

    public int getMinSize(final int numScales) {
        return 5 + ((int)(Math.pow(2, numScales - 1)) * 4);
    }

    /**
     * Check that the 3D image dimensions complies with the number of chosen scales
     * 
     * @param width
     * @param height
     * @param depth
     * @param numScales
     * @throws WaveletConfigException
     */
    void checkImageDimensions(final int width, final int height, final int depth, final int numScales)
            throws WaveletConfigException {
        final int minSize = getMinSize(numScales); // 5+(int)(Math.pow(2,
        // numScales-1))*4;//compute
        // the minimum size for
        // numScales scales
        if ((width < minSize) || (height < minSize) || (depth < minSize))// image
        // size
        // is
        // too
        // small,
        // return
        // an
        // exception
        {
            final String message =
                    "Number of scales too large for the size of the image. These settings require: width>"
                            + (minSize - 1) + ", height >" + (minSize - 1) + " and depth >" + (minSize - 1);
            throw new WaveletConfigException(message);
        }
    }

    /**
     * Check that the 2D image dimensions complies with the wavelet scale
     * 
     * @param width
     * @param height
     * @param numScales
     * @throws WaveletConfigException
     */
    void checkImageDimensions2D(final int width, final int height, final int numScales) throws WaveletConfigException {
        final int minSize = 5 + ((int)(Math.pow(2, numScales - 1) - 1) * 4);// compute
        // the
        // minimum
        // size
        // for
        // numScales
        // scales
        if ((width < minSize) || (height < minSize))// image size is too
        // small, return an
        // exception
        {
            final String message =
                    "Number of scales too large for the size of the image. These settings require: width>"
                            + (minSize - 1) + ", height >" + (minSize - 1);
            throw new WaveletConfigException(message);
        }
    }

    /**
     * Compute the wavelet coefficients from wavelet scales
     * 
     * @param scaleCoefficients
     * @param originalImage
     * @param numScales
     * @param numPixels
     * @param depth
     * @return nbScale + 1 , z , computed coefficients
     */
    public float[][][] b3WaveletCoefficients3D(final float[][][] scaleCoefficients, final float[][] originalImage,
                                               final int numScales, final int numPixels, final int depth) {
        // numScales wavelet images to store, + one image for the low
        // pass residual
        final float[][][] waveletCoefficients = new float[numScales + 1][depth][];

        // compute wavelet coefficients as the difference between scale
        // coefficients of subsequent scales
        final float[][] iterPrev = originalImage;// the finest scale
        // coefficient is the
        // difference between the
        // original image and the
        // first scale.
        float[] iterCurrent;
        int j = 0;
        while (j < numScales) {
            for (int z = 0; z < depth; z++) {
                iterCurrent = scaleCoefficients[j][z];
                final float[] wCoefficients = new float[numPixels];
                for (int i = 0; i < numPixels; i++) {
                    wCoefficients[i] = iterPrev[z][i] - iterCurrent[i];
                }
                waveletCoefficients[j][z] = wCoefficients;
                iterPrev[z] = iterCurrent;
            }
            j++;
        }
        // residual low pass image is the last wavelet Scale
        for (int z = 0; z < depth; z++) {
            waveletCoefficients[numScales][z] = new float[numPixels];
            System.arraycopy(scaleCoefficients[numScales - 1][z], 0, waveletCoefficients[numScales][z], 0, numPixels);
        }
        return waveletCoefficients;
    }

    /**
     * Reconstruct an image from the wavelet coefficients and a low pass residual image
     * 
     * @param inputCoefficients
     * @param lowPassResidual
     * @param output
     * @param numScales
     * @param numPixels
     * @param depth
     */
    public void b3WaveletReconstruction3D(final double[][][] inputCoefficients, final double[][] lowPassResidual,
                                          final double[][] output, final int numScales, final int numPixels,
                                          final int depth) {
        for (int z = 0; z < depth; z++) {
            for (int i = 0; i < numPixels; i++) {
                {
                    double v = lowPassResidual[z][i];
                    for (int j = 0; j < numScales; j++) {
                        v += inputCoefficients[j][z][i];
                    }
                    output[z][i] = v;
                }
            }
        }
    }

    /**
     * Compute the wavelet coefficients from wavelet scales
     * 
     * @param scaleCoefficients
     * @param originalImage
     * @param numScales
     * @param numPixels
     * @return double[scale][1D Coefficient Data]
     */
    public float[][] b3WaveletCoefficients2D(final float[][] scaleCoefficients, final float[] originalImage,
                                             final int numScales, final int numPixels) {
        // numScales wavelet images to store, + one image for the low
        // pass residual
        final float[][] waveletCoefficients = new float[numScales + 1][];

        // compute wavelet coefficients as the difference between scale
        // coefficients of subsequent scales
        float[] iterPrev = originalImage;// the finest scale coefficient
        // is the difference between
        // the original image and the
        // first scale.
        float[] iterCurrent;

        int j = 0;
        while (j < numScales) {
            iterCurrent = scaleCoefficients[j];
            final float[] wCoefficients = new float[numPixels];
            for (int i = 0; i < numPixels; i++) {
                wCoefficients[i] = iterPrev[i] - iterCurrent[i];
            }
            waveletCoefficients[j] = wCoefficients;
            iterPrev = iterCurrent;
            j++;
        }
        // residual low pass image is the last wavelet Scale
        waveletCoefficients[numScales] = new float[numPixels];
        System.arraycopy(scaleCoefficients[numScales - 1], 0, waveletCoefficients[numScales], 0, numPixels);
        return waveletCoefficients;
    }

    /**
     * Reconstruct an image from the wavelet coefficients and a low pass residual image
     */
    public void b3WaveletReconstruction2D(final float[][] inputCoefficients, final float[] lowPassResidual,
                                          final float[] output, final int numScales, final int numVoxels) {
        for (int i = 0; i < numVoxels; i++) {
            float v = lowPassResidual[i];
            for (int j = 0; j < numScales; j++) {
                v += inputCoefficients[j][i];
            }
            output[i] = v;
        }
    }

    /**
     * filter a 2D image with the B3 spline wavelet scale kernel in the x direction when using the a trous algorithm,
     * and swap dimensions
     * 
     * @param arrayIn
     * @param arrayOut
     * @param width
     * @param height
     * @param stepS
     */
    void filterAndSwap2D(final float[] arrayIn, final float[] arrayOut, final int width, final int height,
                         final int stepS) {
        // B3 spline wavelet configuration
        // the convolution kernel is {1/16, 1/4, 3/8, 1/4, 1/16}
        // with some zeros values inserted between the coefficients,
        // depending on the scale
        final float w2 = ((float)1) / 16;
        final float w1 = ((float)1) / 4;
        final float w0 = ((float)3) / 8;

        int w0idx;
        int w1idx1;
        int w2idx1;
        int w1idx2;
        int w2idx2;
        int arrayOutIter;

        int cntX;
        w0idx = 0;

        for (int y = 0; y < height; y++)// loop over the second
        // dimension
        {
            // manage the left border with mirror symmetry
            arrayOutIter = 0 + y;// output pointer initialization,
            // we swap dimensions at this point
            w1idx1 = (w0idx + stepS) - 1;
            w2idx1 = w1idx1 + stepS;
            w1idx2 = w0idx + stepS;
            w2idx2 = w1idx2 + stepS;

            cntX = 0;
            while (cntX < stepS) {
                arrayOut[arrayOutIter] =
                        (w2 * ((arrayIn[w2idx1]) + (arrayIn[w2idx2]))) + (w1 * ((arrayIn[w1idx1]) + (arrayIn[w1idx2])))
                                + (w0 * (arrayIn[w0idx]));
                w1idx1--;
                w2idx1--;
                w1idx2++;
                w2idx2++;
                w0idx++;
                arrayOutIter += height;
                cntX++;
            }
            w1idx1++;
            while (cntX < (2 * stepS)) {
                arrayOut[arrayOutIter] =
                        (w2 * ((arrayIn[w2idx1]) + (arrayIn[w2idx2]))) + (w1 * ((arrayIn[w1idx1]) + (arrayIn[w1idx2])))
                                + (w0 * (arrayIn[w0idx]));
                w1idx1++;
                w2idx1--;
                w1idx2++;
                w2idx2++;
                w0idx++;
                arrayOutIter += height;
                cntX++;
            }
            w2idx1++;
            // filter the center area of the image (no border issue)
            while (cntX < (width - (2 * stepS))) {
                arrayOut[arrayOutIter] =
                        (w2 * ((arrayIn[w2idx1]) + (arrayIn[w2idx2]))) + (w1 * ((arrayIn[w1idx1]) + (arrayIn[w1idx2])))
                                + (w0 * (arrayIn[w0idx]));
                w1idx1++;
                w2idx1++;
                w1idx2++;
                w2idx2++;
                w0idx++;
                arrayOutIter += height;
                cntX++;
            }
            w2idx2--;
            // manage the right border with mirror symmetry
            while (cntX < (width - stepS)) {
                arrayOut[arrayOutIter] =
                        (w2 * ((arrayIn[w2idx1]) + (arrayIn[w2idx2]))) + (w1 * ((arrayIn[w1idx1]) + (arrayIn[w1idx2])))
                                + (w0 * (arrayIn[w0idx]));
                w1idx1++;
                w2idx1++;
                w1idx2++;
                w2idx2--;
                w0idx++;
                arrayOutIter += height;
                cntX++;
            }
            w1idx2--;
            while (cntX < width) {
                arrayOut[arrayOutIter] =
                        (w2 * ((arrayIn[w2idx1]) + (arrayIn[w2idx2]))) + (w1 * ((arrayIn[w1idx1]) + (arrayIn[w1idx2])))
                                + (w0 * (arrayIn[w0idx]));
                w1idx1++;
                w2idx1++;
                w1idx2--;
                w2idx2--;
                w0idx++;
                arrayOutIter += height;
                cntX++;
            }
        }
    }

    /**
     * Compute the wavelet scale images for a 2D image
     * 
     * @param dataIn
     * @param width
     * @param height
     * @param numScales
     * @return
     * @throws WaveletConfigException
     */
    public float[][] b3WaveletScales2D(final float[] dataIn, final int width, final int height, final int numScales)
            throws WaveletConfigException {
        if (numScales < 1)// at least on scale is required
        {
            throw new WaveletConfigException(
                    "Invalid number of wavelet scales. Number of scales should be an integer >=1");
        }
        // check that image dimensions complies with the number of
        // chosen scales
        try {
            checkImageDimensions2D(width, height, numScales);
        } catch (final WaveletConfigException e) {
            throw (e);
        }

        int s;// scale
        int stepS;// step between non zero coefficients of the
        // convolution kernel, depends on the scale
        final int wh = width * height;
        final float[][] resArray = new float[numScales][];// store wavelet
        // scales in a new
        // 2d double array
        float[] prevArray = dataIn; // array to filter, original data
        // for the first scale
        float[] currentArray = new float[wh]; // filtered array

        for (s = 1; s <= numScales; s++)// for each scale
        {
            stepS = (int)Math.pow(2, s - 1);// compute the step
            // between non zero
            // coefficients of the
            // convolution kernel =
            // 2^(scale-1)
            // convolve along the x direction and swap dimensions
            filterAndSwap2D(prevArray, currentArray, width, height, stepS);
            // swap current and previous array pointers
            if (s == 1) {
                prevArray = currentArray;// the filtered array
                // becomes the array to
                // filter
                currentArray = new float[wh];// allocate memory
                // for the next
                // dimension
                // filtering
                // (preserve
                // original data)
            } else {
                final float[] tmp = currentArray;
                currentArray = prevArray;// the filtered array
                // becomes the array to
                // filter
                prevArray = tmp;// the filtered array becomes
                // the array to filter
            }
            // convolve along the y direction and swap dimensions
            filterAndSwap2D(prevArray, currentArray, height, width, stepS);// swap size of dimensions
            // swap current and previous array pointers
            final float[] tmp = currentArray;
            currentArray = prevArray;
            prevArray = tmp;

            resArray[s - 1] = new float[wh]; // allocate memory to
            // store the filtered
            // array
            System.arraycopy(prevArray, 0, resArray[s - 1], 0, wh);
        }
        return resArray;
    }

    /**
     * 
     * @param arrayIn
     * @param arrayOut
     * @param width
     * @param height
     * @param depth
     * @param stepS
     */
    void filterZdirection(final float[][] arrayIn, final float[][] arrayOut, final int width, final int height,
                          final int depth, final int stepS) {
        // B3 spline wavelet configuration
        // the convolution kernel is {1/16, 1/4, 3/8, 1/4, 1/16}
        // with some zeros values inserted between the coefficients,
        // depending on the scale
        final float w2 = ((float)1) / 16;
        final float w1 = ((float)1) / 4;
        final float w0 = ((float)3) / 8;

        final float[] bufferArrayIn = new float[depth]; // create a buffer for
        // each 2D location i
        final float[] bufferArrayOut = new float[depth];// create an output
        // buffer for each 2D
        // location i
        for (int i = 0; i < (width * height); i++) // loop in 2D over the
        // pixels of the
        // slices, can be done
        // in a parallel manner
        {
            for (int z = 0; z < depth; z++) {
                bufferArrayIn[z] = arrayIn[z][i];
            }
            // then filter the buffer
            int cntZ = 0;
            int arrayOutIter = 0;
            int w0idx = 0;
            int w1idx1 = (w0idx + stepS) - 1;
            int w2idx1 = w1idx1 + stepS;
            int w1idx2 = w0idx + stepS;
            int w2idx2 = w1idx2 + stepS;

            while (cntZ < stepS) {
                bufferArrayOut[arrayOutIter] =
                        (w2 * ((bufferArrayIn[w2idx1]) + (bufferArrayIn[w2idx2])))
                                + (w1 * ((bufferArrayIn[w1idx1]) + (bufferArrayIn[w1idx2])))
                                + (w0 * (bufferArrayIn[w0idx]));
                w1idx1--;
                w2idx1--;
                w1idx2++;
                w2idx2++;
                w0idx++;
                arrayOutIter++;
                cntZ++;
            }
            w1idx1++;
            while (cntZ < (2 * stepS)) {
                bufferArrayOut[arrayOutIter] =
                        (w2 * ((bufferArrayIn[w2idx1]) + (bufferArrayIn[w2idx2])))
                                + (w1 * ((bufferArrayIn[w1idx1]) + (bufferArrayIn[w1idx2])))
                                + (w0 * (bufferArrayIn[w0idx]));
                w1idx1++;
                w2idx1--;
                w1idx2++;
                w2idx2++;
                w0idx++;
                arrayOutIter++;
                cntZ++;
            }
            w2idx1++;
            // filter the center area of the image (no border issue)
            while (cntZ < (depth - (2 * stepS))) {
                bufferArrayOut[arrayOutIter] =
                        (w2 * ((bufferArrayIn[w2idx1]) + (bufferArrayIn[w2idx2])))
                                + (w1 * ((bufferArrayIn[w1idx1]) + (bufferArrayIn[w1idx2])))
                                + (w0 * (bufferArrayIn[w0idx]));
                w1idx1++;
                w2idx1++;
                w1idx2++;
                w2idx2++;
                w0idx++;
                arrayOutIter++;
                cntZ++;
            }
            w2idx2--;
            // manage the right border with mirror symmetry
            while (cntZ < (depth - stepS)) {
                bufferArrayOut[arrayOutIter] =
                        (w2 * ((bufferArrayIn[w2idx1]) + (bufferArrayIn[w2idx2])))
                                + (w1 * ((bufferArrayIn[w1idx1]) + (bufferArrayIn[w1idx2])))
                                + (w0 * (bufferArrayIn[w0idx]));
                w1idx1++;
                w2idx1++;
                w1idx2++;
                w2idx2--;
                w0idx++;
                arrayOutIter++;
                cntZ++;
            }
            w1idx2--;
            while (cntZ < depth) {
                bufferArrayOut[arrayOutIter] =
                        (w2 * ((bufferArrayIn[w2idx1]) + (bufferArrayIn[w2idx2])))
                                + (w1 * ((bufferArrayIn[w1idx1]) + (bufferArrayIn[w1idx2])))
                                + (w0 * (bufferArrayIn[w0idx]));
                w1idx1++;
                w2idx1++;
                w1idx2--;
                w2idx2--;
                w0idx++;
                arrayOutIter++;
                cntZ++;
            }
            // copy the buffer to the original structure
            for (int z = 0; z < depth; z++) {
                arrayOut[z][i] = bufferArrayOut[z];
            }
        }
    }

    /**
     * Compute the scale images for a 3D image
     * 
     * @param dataIn
     * @param width
     * @param height
     * @param depth
     * @param numScales
     * @return
     * @throws WaveletConfigException
     */
    public float[][][] b3WaveletScales3D(final float[][] dataIn, final int width, final int height, final int depth,
                                         final int numScales) throws WaveletConfigException {
        if (numScales < 1)// at least on scale is required
        {
            throw new WaveletConfigException(
                    "Invalid number of wavelet scales. Number of scales should be an integer >=1");
        }
        // check that image dimensions complies with the number of
        // chosen scales
        try {
            checkImageDimensions(width, height, depth, numScales);
        } catch (final WaveletConfigException e) {
            throw (e);
        }

        int s;// scale
        int stepS;// step between non zero coefficients of the
        // convolution kernel, depends on the scale
        final int wh = width * height;

        final float[][][] resArray = new float[numScales][][];// store wavelet
        // scales in a
        // new 2d double
        // array
        float[][] currentArray3D = dataIn;// array to be filtered

        for (s = 1; s <= numScales; s++)// for each scale
        {
            final float[][] prevArray = new float[depth][]; // array to
            // filter in
            // 2D,
            // original
            // data for
            // the first
            // scale
            stepS = (int)Math.pow(2, s - 1);// compute the step
            // between non zero
            // coefficients of the
            // convolution kernel =
            // 2^(scale-1)

            // process each slice of the stack, can be done in a
            // parallel manner
            for (int z = 0; z < depth; z++) {
                float[] currentArray;// filtered array
                prevArray[z] = currentArray3D[z]; // array to
                // filter,
                // original
                // data for
                // the first
                // scale
                currentArray = new float[wh];// filtered array

                // convolve along the x direction and swap
                // dimensions
                filterAndSwap2D(prevArray[z], currentArray, width, height, stepS);
                {
                    prevArray[z] = currentArray; // the
                    // filtered
                    // array
                    // becomes
                    // the
                    // array to
                    // filter
                    currentArray = new float[wh]; // allocate
                    // memory
                    // for the
                    // next
                    // dimension
                    // filtering
                    // (preserve
                    // original
                    // data)
                }
                // convolve along the y direction and swap
                // dimensions
                filterAndSwap2D(prevArray[z], currentArray, height, width, stepS);// swap
                // size of
                // dimensions
                prevArray[z] = currentArray;
            }

            currentArray3D = new float[depth][wh];
            // convolve along the z direction
            filterZdirection(prevArray, currentArray3D, width, height, depth, stepS);
            resArray[s - 1] = currentArray3D;

        }
        return resArray;
    }

}
