package org.knime.knip.core.ops.img.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.Cursor;
import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.fft.FourierTransform;
import net.imglib2.algorithm.fft.FourierTransform.Rearrangement;
import net.imglib2.algorithm.fft.InverseFourierTransform;
import net.imglib2.algorithm.region.localneighborhood.old.LocalNeighborhoodCursor;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.roi.RectangleRegionOfInterest;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;

class PhaseCorrelation<T extends RealType<T>, S extends RealType<S>>
		implements MultiThreaded, Algorithm, Benchmark {
	final int numDimensions;
	boolean computeFFTinParalell = true;
	Img<T> image1;
	Img<S> image2;
	int numPeaks;
	int[] minOverlapPx;
	float normalizationThreshold;
	boolean verifyWithCrossCorrelation;
	ArrayList<PhaseCorrelationPeak> phaseCorrelationPeaks;
	String errorMessage = "";
	int numThreads;
	long processingTime;

	public PhaseCorrelation(final Img<T> image1, final Img<S> image2,
			final int numPeaks, final boolean verifyWithCrossCorrelation) {
		this.image1 = image1;
		this.image2 = image2;
		this.numPeaks = numPeaks;
		this.verifyWithCrossCorrelation = verifyWithCrossCorrelation;
		this.numDimensions = image1.numDimensions();
		this.normalizationThreshold = 1E-5f;

		this.minOverlapPx = new int[numDimensions];
		setMinimalPixelOverlap(3);

		setNumThreads();
		processingTime = -1;
	}

	public PhaseCorrelation(final Img<T> image1, final Img<S> image2) {
		this(image1, image2, 5, true);
	}

	public void setComputeFFTinParalell(final boolean computeFFTinParalell) {
		this.computeFFTinParalell = computeFFTinParalell;
	}

	public void setInvestigateNumPeaks(final int numPeaks) {
		this.numPeaks = numPeaks;
	}

	public void setNormalizationThreshold(final int normalizationThreshold) {
		this.normalizationThreshold = normalizationThreshold;
	}

	public void setVerifyWithCrossCorrelation(
			final boolean verifyWithCrossCorrelation) {
		this.verifyWithCrossCorrelation = verifyWithCrossCorrelation;
	}

	public void setMinimalPixelOverlap(final int[] minOverlapPx) {
		this.minOverlapPx = minOverlapPx.clone();
	}

	public void setMinimalPixelOverlap(final int minOverlapPx) {
		for (int d = 0; d < numDimensions; ++d) {
            this.minOverlapPx[d] = minOverlapPx;
        }
	}

	public boolean getComputeFFTinParalell() {
		return computeFFTinParalell;
	}

	public int getInvestigateNumPeaks() {
		return numPeaks;
	}

	public float getNormalizationThreshold() {
		return normalizationThreshold;
	}

	public boolean getVerifyWithCrossCorrelation() {
		return verifyWithCrossCorrelation;
	}

	public int[] getMinimalPixelOverlap() {
		return minOverlapPx.clone();
	}

	public PhaseCorrelationPeak getShift() {
		return phaseCorrelationPeaks.get(phaseCorrelationPeaks.size() - 1);
	}

	public ArrayList<PhaseCorrelationPeak> getAllShifts() {
		return phaseCorrelationPeaks;
	}

	@Override
	public boolean process() {
		// get the maximal dimensions of both images
		final int[] maxDim = getMaxDim(image1, image2);

		// compute fourier transforms
		FourierTransform<T, ComplexFloatType> fft1;
		FourierTransform<S, ComplexFloatType> fft2;
		try {
			fft1 = new FourierTransform<T, ComplexFloatType>(
					image1, new ComplexFloatType());
			fft2 = new FourierTransform<S, ComplexFloatType>(
					image2, new ComplexFloatType());
		} catch (IncompatibleTypeException e1) {
			return false;
		}
		fft1.setRelativeImageExtension(0.1f);
		fft2.setRelativeImageExtension(0.1f);
		fft1.setRelativeFadeOutDistance(0.1f);
		fft2.setRelativeFadeOutDistance(0.1f);
		fft1.setRearrangement(Rearrangement.UNCHANGED);
		fft2.setRearrangement(Rearrangement.UNCHANGED);

		boolean sizeFound = false;

		// check if the size was enough ( there is a minimum extension )
		do {
			sizeFound = true;
			fft1.setExtendedOriginalImageSize(maxDim);
			fft2.setExtendedOriginalImageSize(maxDim);

			for (int d = 0; d < numDimensions; ++d) {
				final int diff = Math.abs(fft1.getExtendedSize()[d]
						- fft2.getExtendedSize()[d]);

				if (diff > 0) {
					maxDim[d] += diff;
					sizeFound = false;
				}
			}
		} while (!sizeFound);

		if (!fft1.checkInput()) {
			errorMessage = "Fourier Transform of first image failed: "
					+ fft1.getErrorMessage();
			return false;
		}

		if (!fft2.checkInput()) {
			errorMessage = "Fourier Transform of second image failed: "
					+ fft2.getErrorMessage();
			return false;
		}

		//
		// compute the fft's
		//
		if (!computeFFT(fft1, fft2)) {
			errorMessage = "Fourier Transform of failed: fft1="
					+ fft1.getErrorMessage() + " fft2="
					+ fft2.getErrorMessage();
			return false;
		}

		final Img<ComplexFloatType> fftImage1 = fft1.getResult();
		final Img<ComplexFloatType> fftImage2 = fft2.getResult();
		//
		// normalize and compute complex conjugate of fftImage2
		//
		normalizeAndConjugate(fftImage1, fftImage2);

		//
		// multiply fftImage1 and fftImage2 which yields the phase correlation
		// spectrum
		//
		multiplyInPlace(fftImage1, fftImage2);

		//
		// invert fftImage1 which contains the phase correlation spectrum
		//
		InverseFourierTransform<FloatType, ComplexFloatType> invFFT;
		try {
			invFFT = new InverseFourierTransform<FloatType, ComplexFloatType>(
					fftImage1, fft1, new FloatType());
		} catch (IncompatibleTypeException e) {
			return false;
		}
		invFFT.setCropBackToOriginalSize(false);

		if (!invFFT.checkInput() || !invFFT.process()) {
			errorMessage = "Inverse Fourier Transform of failed: "
					+ invFFT.getErrorMessage();
			return false;
		}

		final Img<FloatType> invPCM = invFFT.getResult();

		/*
		 * invPCM.getDisplay().setMinMax(); invPCM.setName("invPCM");
		 * ImageJFunctions.copyToImagePlus( invPCM ).show();
		 */

		//
		// extract the peaks
		//
		phaseCorrelationPeaks = extractPhaseCorrelationPeaks(invPCM, numPeaks,
				fft1, fft2);

		if (!verifyWithCrossCorrelation) {
            return true;
        }

		long[] dims = new long[invPCM.numDimensions()];
		invPCM.dimensions(dims);

		verifyWithCrossCorrelation(phaseCorrelationPeaks, dims, image1, image2);

		return true;
	}

	protected void verifyWithCrossCorrelation(
			final ArrayList<PhaseCorrelationPeak> peakList,
			final long[] dimInvPCM, final Img<T> image1, final Img<S> image2) {
		final boolean[][] coordinates = MathLib
				.getRecursiveCoordinates(numDimensions);

		final ArrayList<PhaseCorrelationPeak> newPeakList = new ArrayList<PhaseCorrelationPeak>();

		//
		// get all the different possiblities
		//
		for (final PhaseCorrelationPeak peak : peakList) {
			for (int i = 0; i < coordinates.length; ++i) {
				final boolean[] currentPossiblity = coordinates[i];

				final long[] peakPosition = peak.getPosition();

				for (int d = 0; d < currentPossiblity.length; ++d) {
					if (currentPossiblity[d]) {
						if (peakPosition[d] < 0) {
                            peakPosition[d] += dimInvPCM[d];
                        } else {
                            peakPosition[d] -= dimInvPCM[d];
                        }
					}
				}
				newPeakList.add(new PhaseCorrelationPeak(peakPosition, peak
						.getPhaseCorrelationPeak()));
			}
		}

		//
		// test them multithreaded
		//
		final AtomicInteger ai = new AtomicInteger(0);
		Thread[] threads = SimpleMultiThreading.newThreads(getNumThreads());
		final int numThreads = threads.length;

		for (int ithread = 0; ithread < threads.length; ++ithread) {
            threads[ithread] = new Thread(new Runnable() {
				public void run() {
					final int myNumber = ai.getAndIncrement();

					for (int i = 0; i < newPeakList.size(); ++i) {
                        if (i % numThreads == myNumber) {
							final PhaseCorrelationPeak peak = newPeakList
									.get(i);
							peak.setCrossCorrelationPeak((float) testCrossCorrelation(
									peak.getPosition(), image1, image2,
									minOverlapPx));

							// sort by cross correlation peak
							peak.setSortPhaseCorrelation(false);
						}
                    }

				}
			});
        }

		SimpleMultiThreading.startAndJoin(threads);

		// update old list and sort
		peakList.clear();
		peakList.addAll(newPeakList);
		Collections.sort(peakList);
	}

	public static <T extends RealType<T>, S extends RealType<S>> double testCrossCorrelation(
			final long[] shift, final Img<T> image1, final Img<S> image2) {
		return testCrossCorrelation(shift, image1, image2,
				Util.getArrayFromValue(5, image1.numDimensions()));
	}

	public static <T extends RealType<T>, S extends RealType<S>> double testCrossCorrelation(
			final long[] shift, final Img<T> image1, final Img<S> image2,
			final int minOverlapPx) {
		return testCrossCorrelation(shift, image1, image2,
				Util.getArrayFromValue(minOverlapPx, image1.numDimensions()));
	}

	public static <T extends RealType<T>, S extends RealType<S>> double testCrossCorrelation(
			final long[] shift, final Img<T> image1, final Img<S> image2,
			final int[] minOverlapPx) {
		final int numDimensions = image1.numDimensions();
		double correlationCoefficient = 0;

		final double[] overlapSize = new double[numDimensions];
		final double[] offsetImage1 = new double[numDimensions];
		final double[] offsetImage2 = new double[numDimensions];

		int numPixels = 1;

		for (int d = 0; d < numDimensions; ++d) {
			if (shift[d] >= 0) {
				// two possiblities
				//
				// shift=start end
				// | |
				// A: Image 1 ------------------------------
				// Image 2 ----------------------------------
				//
				// shift=start end
				// | |
				// B: Image 1 ------------------------------
				// Image 2 -------------------

				// they are not overlapping ( this might happen due to fft
				// zeropadding and extension
				if (shift[d] >= image1.dimension(d)) {
                    return 0;
                }

				offsetImage1[d] = shift[d];
				offsetImage2[d] = 0;
				overlapSize[d] = Math.min(image1.dimension(d) - shift[d],
						image2.dimension(d));
			} else {
				// two possiblities
				//
				// shift start end
				// | | ` |
				// A: Image 1 ------------------------------
				// Image 2 ------------------------------
				//
				// shift start end
				// | | |
				// B: Image 1 ------------
				// Image 2 -------------------

				// they are not overlapping ( this might happen due to fft
				// zeropadding and extension
				if (shift[d] >= image2.dimension(d)) {
                    return 0;
                }
				offsetImage1[d] = 0;
				offsetImage2[d] = -shift[d];
				overlapSize[d] = Math.min(image2.dimension(d) + shift[d],
						image1.dimension(d));
			}

			numPixels *= overlapSize[d];

			if (overlapSize[d] < minOverlapPx[d]) {
                return 0;
            }
		}

		final RectangleRegionOfInterest roi1 = new RectangleRegionOfInterest(offsetImage1, overlapSize);
		final RectangleRegionOfInterest roi2 = new RectangleRegionOfInterest(offsetImage2, overlapSize);

		Cursor<T> roiCursor1 = roi1.getIterableIntervalOverROI(image1).localizingCursor();
		Cursor<S> roiCursor2 = roi2.getIterableIntervalOverROI(image2).localizingCursor();
		//
		// compute average
		//
		double avg1 = 0;
		double avg2 = 0;

		while (roiCursor1.hasNext()) {
			avg1 += roiCursor1.next().getRealDouble();
			avg2 += roiCursor2.next().getRealDouble();
		}
		avg1 /= numPixels;
		avg2 /= numPixels;

		//
		// compute cross correlation
		//
		roiCursor1.reset();
		roiCursor2.reset();

		double var1 = 0, var2 = 0;
		double coVar = 0;

		while (roiCursor1.hasNext()) {
			final double pixel1 = roiCursor1.next().getRealDouble();
			final double pixel2 = roiCursor2.next().getRealDouble();

			final double dist1 = pixel1 - avg1;
			final double dist2 = pixel2 - avg2;
			coVar += dist1 * dist2;
			var1 += dist1 * dist1;
			var2 += dist2 * dist2;
		}

		var1 /= numPixels;
		var2 /= numPixels;
		coVar /= numPixels;
		double stDev1 = Math.sqrt(var1);
		double stDev2 = Math.sqrt(var2);
		// all pixels had the same color....
		if (stDev1 == 0 || stDev2 == 0) {
			if (stDev1 == stDev2 && avg1 == avg2) {
                return 1;
            } else {
                return 0;
            }
		}
		// compute correlation coeffienct
		correlationCoefficient = coVar / (stDev1 * stDev2);

		return correlationCoefficient;
	}

	protected ArrayList<PhaseCorrelationPeak> extractPhaseCorrelationPeaks(
			final Img<FloatType> invPCM, final int numPeaks,
			final FourierTransform<T, ComplexFloatType> fft1,
			final FourierTransform<S, ComplexFloatType> fft2) {
		final ArrayList<PhaseCorrelationPeak> peakList = new ArrayList<PhaseCorrelationPeak>();

		for (int i = 0; i < numPeaks; ++i) {
            peakList.add(new PhaseCorrelationPeak(new long[numDimensions],
					Float.MIN_VALUE));
        }
		final Cursor<FloatType> cursor = invPCM.localizingCursor();//new OutsideStrategyPeriodicFactory<FloatType>());

		final long[] THE_CENTER = new long[cursor.numDimensions()];
		for (int i = 0; i < THE_CENTER.length; ++i) {
			THE_CENTER[i] = 1;
		}

		LocalNeighborhoodCursor<FloatType> localCursor = new LocalNeighborhoodCursor<FloatType>(invPCM, THE_CENTER);

		final int[] originalOffset1 = fft1.getOriginalOffset();
		final int[] originalOffset2 = fft2.getOriginalOffset();
		final int[] offset = new int[numDimensions];

		for (int d = 0; d < numDimensions; ++d) {
            offset[d] = originalOffset2[d] - originalOffset1[d];
        }

		final long[] imgSize = new long[invPCM.numDimensions()];
		invPCM.dimensions(imgSize);

		while (cursor.hasNext()) {
			cursor.fwd();

			// set the local cursor to the current position of the mother cursor
			localCursor.updateCenter(cursor);

			// the value we are checking for if it is a maximum
			final float value = cursor.get().get();
			boolean isMax = true;

			// iterate over local environment while value is still the maximum
			while (localCursor.hasNext() && isMax) {
				localCursor.fwd();
				isMax = (cursor.get().get() <= value);
			}

			// reset the mothercursor and this cursor
			localCursor.reset();
			if (isMax) {
				float lowestValue = Float.MAX_VALUE;
				int lowestValueIndex = -1;

				for (int i = 0; i < numPeaks; ++i) {
					final float v = peakList.get(i).getPhaseCorrelationPeak();

					if (v < lowestValue) {
						lowestValue = v;
						lowestValueIndex = i;
					}
				}

				// if this value is bigger than the lowest entry we replace it
				if (value > lowestValue) {
					// remove lowest entry
					peakList.remove(lowestValueIndex);
					// add new peak
					final long[] position = new long[cursor.numDimensions()];
					cursor.localize(position);

					for (int d = 0; d < numDimensions; ++d) {
						position[d] = (position[d] + offset[d]) % imgSize[d];

						if (position[d] > imgSize[d] / 2) {
                            position[d] = position[d] - imgSize[d];
                        }
					}
					final PhaseCorrelationPeak pcp = new PhaseCorrelationPeak(
							position, value);
					cursor.localize(position);
					pcp.setOriginalInvPCMPosition(position);
					peakList.add(pcp);
				}
			}
		}

		// sort list
		Collections.sort(peakList);

		return peakList;
	}

	protected static int[] getMaxDim(final Img<?> image1, final Img<?> image2) {
		final int[] maxDim = new int[image1.numDimensions()];

		for (int d = 0; d < image1.numDimensions(); ++d) {
            maxDim[d] = (int) Math
					.max(image1.dimension(d), image2.dimension(d));
        }

		return maxDim;
	}

	protected void multiplyInPlace(final Img<ComplexFloatType> fftImage1,
			final Img<ComplexFloatType> fftImage2) {
		final Cursor<ComplexFloatType> cursor1 = fftImage1.cursor();
		final Cursor<ComplexFloatType> cursor2 = fftImage2.cursor();

		while (cursor1.hasNext()) {
			cursor1.fwd();
			cursor2.fwd();

			cursor1.get().mul(cursor2.get());
		}
	}

	protected void normalizeAndConjugate(final Img<ComplexFloatType> fftImage1,
			final Img<ComplexFloatType> fftImage2) {
		final AtomicInteger ai = new AtomicInteger(0);
		Thread[] threads = SimpleMultiThreading.newThreads(Math.min(2,
				numThreads));
		final int numThreads = threads.length;

		for (int ithread = 0; ithread < threads.length; ++ithread) {
            threads[ithread] = new Thread(new Runnable() {
				public void run() {
					final int myNumber = ai.getAndIncrement();

					if (numThreads == 1) {
						normalizeComplexImage(fftImage1, normalizationThreshold);
						normalizeAndConjugateComplexImage(fftImage2,
								normalizationThreshold);
					} else {
						if (myNumber == 0) {
                            normalizeComplexImage(fftImage1,
									normalizationThreshold);
                        } else {
                            normalizeAndConjugateComplexImage(fftImage2,
									normalizationThreshold);
                        }
					}
				}
			});
        }

		SimpleMultiThreading.startAndJoin(threads);
	}

	private static final void normalizeComplexImage(
			final Img<ComplexFloatType> fftImage,
			final float normalizationThreshold) {
		final Cursor<ComplexFloatType> cursor = fftImage.cursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			normalizeLength(cursor.get(), normalizationThreshold);
		}
	}

	private static void normalizeLength( final ComplexFloatType t, final float threshold )
    {
            final float real = t.getRealFloat();
            final float complex = t.getImaginaryFloat();

            final float length = (float)Math.sqrt( real*real + complex*complex );

            if ( length < threshold )
            {
                    t.set(0, 0);
            }
            else
            {
            	t.set(real / length, complex / length);
            }
    }

	private static final void normalizeAndConjugateComplexImage(
			final Img<ComplexFloatType> fftImage,
			final float normalizationThreshold) {
		final Cursor<ComplexFloatType> cursor = fftImage.cursor();

		while (cursor.hasNext()) {
			cursor.fwd();

			normalizeLength(cursor.get(), normalizationThreshold);
			cursor.get().complexConjugate();
		}
	}

	protected boolean computeFFT(
			final FourierTransform<T, ComplexFloatType> fft1,
			final FourierTransform<S, ComplexFloatType> fft2) {
		// use two threads in paralell if wanted
		final int minThreads = computeFFTinParalell ? 2 : 1;

		final AtomicInteger ai = new AtomicInteger(0);
		Thread[] threads = SimpleMultiThreading.newThreads(Math.min(minThreads,
				numThreads));
		final int numThreads = threads.length;

		final boolean[] sucess = new boolean[2];

		for (int ithread = 0; ithread < threads.length; ++ithread) {
            threads[ithread] = new Thread(new Runnable() {
				public void run() {
					final int myNumber = ai.getAndIncrement();

					if (numThreads == 1) {
						fft1.setNumThreads(getNumThreads());
						fft2.setNumThreads(getNumThreads());
						sucess[0] = fft1.process();
						sucess[1] = fft2.process();
					} else {
						if (myNumber == 0) {
							fft1.setNumThreads(getNumThreads() / 2);
							sucess[0] = fft1.process();
						} else {
							fft2.setNumThreads(getNumThreads() / 2);
							sucess[1] = fft2.process();
						}
					}
				}
			});
        }

		SimpleMultiThreading.startAndJoin(threads);

		return sucess[0] && sucess[1];
	}

	@Override
	public long getProcessingTime() {
		return processingTime;
	}

	@Override
	public void setNumThreads() {
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads(final int numThreads) {
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads() {
		return numThreads;
	}

	@Override
	public boolean checkInput() {
		if (errorMessage.length() > 0) {
			return false;
		}

		if (image1 == null || image2 == null) {
			errorMessage = "One of the input images is null";
			return false;
		}

		if (image1.numDimensions() != image2.numDimensions()) {
			errorMessage = "Dimensionality of images is not the same";
			return false;
		}

		return true;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

}