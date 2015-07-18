package org.knime.knip.core.ops.labeling;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.AllConnectedComponents;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.ops.operation.BinaryObjectFactory;
import net.imglib2.ops.operation.BinaryOutputOperation;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * Watershed algorithms. The watershed algorithm segments and labels an image using an analogy to a landscape. The image
 * intensities are turned into the z-height of the landscape and the landscape is "filled with water" and the bodies of
 * water label the landscape's pixels. Here is the reference for the original paper:
 *
 * Lee Vincent, Pierre Soille, Watersheds in digital spaces: An efficient algorithm based on immersion simulations, IEEE
 * Trans. Pattern Anal. Machine Intell., 13(6) 583-598 (1991)
 *
 * Watersheds are often performed on the gradient of an intensity image or one where the edges of the object boundaries
 * have been enhanced. The resulting image has a depressed object interior and a ridge which constrains the watershed
 * boundary.
 *
 * This is a modification of the original implementation from Lee Kamentsky to be able to add the watersheds as well
 * (e.g. for region merging by means of the boundaries).
 *
 * @author Lee Kamentsky
 * @author Martin Horn
 * @author Jonathan Hale (University of Konstanz)
 *
 *
 * @param <T> Type of input RandomAccessibleInterval
 * @param <L> Type of seed Labeling
 *
 */

public class WatershedWithSheds<T extends RealType<T>, L>
        implements
        BinaryOutputOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<LabelingType<L>>, RandomAccessibleInterval<LabelingType<String>>> {

    /**
     *
     * @param <U> Pixel Type
     */
    protected static class PixelIntensity<U extends Comparable<U>> implements Comparable<PixelIntensity<U>> {
        /**
         * Index
         */
        protected final long m_index;

        /**
         * Age
         */
        protected final long m_age;

        /**
         * Intensity
         */
        protected final double m_intensity;

        /**
         * Labeling
         */
        protected final Set<U> m_labeling;

        /**
         * Constructor of PixelIntensity
         *
         * @param position
         * @param dimensions
         * @param intensity
         * @param age
         * @param labeling
         */
        public PixelIntensity(final long[] position, final long[] dimensions, final double intensity, final long age,
                              final Set<U> labeling) {
            long index = position[0];
            long multiplier = dimensions[0];
            for (int i = 1; i < dimensions.length; i++) {
                index += position[i] * multiplier;
                multiplier *= dimensions[i];
            }

            this.m_index = index;
            this.m_intensity = intensity;
            this.m_labeling = new HashSet<>(labeling);
            this.m_age = age;
        }

        @Override
        public int compareTo(final PixelIntensity<U> other) {
            int result = Double.compare(m_intensity, other.m_intensity);
            if (result == 0) {
                result = Double.compare(m_age, other.m_age);
            }
            return result;
        }

        void getPosition(final long[] position, final long[] dimensions) {
            long idx = m_index;
            for (int i = 0; i < dimensions.length; i++) {
                position[i] = (int)(idx % dimensions[i]);
                idx /= dimensions[i];
            }
        }

        Set<U> getLabeling() {
            return m_labeling;
        }
    }

    /**
     * Structuring Element
     */
    protected long[][] m_structuringElement;

    /**
     * ImgFactory for the result
     */
    protected ImgFactory<IntType> m_factory;

    /**
     * Constructor of WatershedsWithSheds Uses a default ArrayImgFactory<IntType>() as result factory.
     *
     * @param structuringElement Structuring Element
     */
    public WatershedWithSheds(final long[][] structuringElement) {
        this(structuringElement, new ArrayImgFactory<IntType>());
    }

    /**
     * Constructor of WatershedsWithSheds
     *
     * @param structuringElement Structuring Element
     * @param factory Factory for result
     */
    public WatershedWithSheds(final long[][] structuringElement, final ImgFactory<IntType> factory) {
        m_structuringElement = structuringElement;
        m_factory = factory;
    }

    /**
     * Set the structuring element that defines the connectivity
     *
     * @param structuringElement an array of offsets where each element of the array gives the offset of a connected
     *            pixel from a pixel of interest. You can use AllConnectedComponents.getStructuringElement to get an
     *            8-connected (or N-dimensional equivalent) structuring element (all adjacent pixels + diagonals).
     */
    public void setStructuringElement(final long[][] structuringElement) {
        m_structuringElement = structuringElement;
    }

    /**
     * Sets the ImgFactory used for output Img.
     *
     * @param factory
     */
    public void setResultFactory(final ImgFactory<IntType> factory) {
        m_factory = factory;
    }

    /**
     * Check if input is valid
     *
     * @param image
     * @param seeds
     * @param output
     */
    public void checkInput(final RandomAccessibleInterval<T> image,
                           final RandomAccessibleInterval<LabelingType<L>> seeds,
                           final RandomAccessibleInterval<LabelingType<String>> output) {
        if (seeds.numDimensions() != image.numDimensions()) {
            throw new IllegalArgumentException(
                    String.format("The dimensionality of the seed labeling (%dD) does not match that of the intensity image (%dD)",
                                  seeds.numDimensions(), image.numDimensions()));
        }
        if (seeds.numDimensions() != output.numDimensions()) {
            throw new IllegalArgumentException(
                    String.format("The dimensionality of the seed labeling (%dD) does not match that of the output labeling (%dD)",
                                  seeds.numDimensions(), output.numDimensions()));
        }
        for (int i = 0; i < m_structuringElement.length; i++) {
            if (m_structuringElement[i].length != seeds.numDimensions()) {
                throw new IllegalArgumentException(
                        "Some or all of the structuring element offsets do not have the same number of dimensions as the image");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RandomAccessibleInterval<LabelingType<String>>
            compute(final RandomAccessibleInterval<T> input, final RandomAccessibleInterval<LabelingType<L>> seeds,
                    final RandomAccessibleInterval<LabelingType<String>> output) {
        checkInput(input, seeds, output);

        if (m_structuringElement == null) {
            m_structuringElement = AllConnectedComponents.getStructuringElement(input.numDimensions());
        }

        IterableInterval<T> iterableInput = Views.iterable(input);

        // if (m_structuringElement == null)
        // m_structuringElement = AbstractRegionGrowing
        // .get4ConStructuringElement(input1.numDimensions());
        /*
         * Make an OutOfBounds for the labels that returns empty labels if out
         * of bounds. Make an OutOfBounds for the intensities that returns the
         * maximum intensity if out of bounds so that in-bounds will be in a
         * deep valley.
         */
        final RandomAccess<LabelingType<String>> outputAccess = output.randomAccess();

        final T maxVal = iterableInput.firstElement().createVariable();
        maxVal.setReal(maxVal.getMaxValue());

        final RandomAccess<T> imageAccess = Views.extendValue(input, maxVal).randomAccess();

        /*
         * Start by loading up a priority queue with the seeded pixels
         */
        PriorityQueue<PixelIntensity<String>> pq = new PriorityQueue<PixelIntensity<String>>();
        Cursor<LabelingType<L>> c = Views.iterable(seeds).localizingCursor();

        long[] dimensions = new long[input.numDimensions()];
        output.dimensions(dimensions);
        long[] position = new long[input.numDimensions()];
        long[] destPosition = new long[input.numDimensions()];
        long age = 0;

        /*
         * carries over the seeding points to the new label and adds them to the
         * pixel priority queue
         */
        while (c.hasNext()) {
            Set<L> l = c.next();
            if (l.isEmpty()) {
                continue;
            }

            c.localize(position);
            imageAccess.setPosition(position);
            if (!Intervals.contains(input, imageAccess)) {
                continue;
            }
            outputAccess.setPosition(position);
            if (!Intervals.contains(output, outputAccess)) {
                continue;
            }
            final LabelingType<String> tDest = outputAccess.get();
            tDest.clear();
            tDest.add(l.iterator().next().toString());
            double intensity = imageAccess.get().getRealDouble();
            pq.add(new PixelIntensity<String>(position, dimensions, intensity, age++, tDest));
        }
        /*
         * Rework the structuring element into a series of consecutive offsets
         * so we can use Positionable.move to scan the image array.
         */
        long[][] strelMoves = new long[m_structuringElement.length][];
        long[] currentOffset = new long[input.numDimensions()];
        for (int i = 0; i < m_structuringElement.length; i++) {
            strelMoves[i] = new long[input.numDimensions()];
            for (int j = 0; j < input.numDimensions(); j++) {
                strelMoves[i][j] = m_structuringElement[i][j] - currentOffset[j];
                if (i > 0) {
                    currentOffset[j] += m_structuringElement[i][j] - m_structuringElement[i - 1][j];
                } else {
                    currentOffset[j] += m_structuringElement[i][j];
                }
            }
        }
        /*
         * Pop the head of the priority queue, label and push all unlabeled
         * connected pixels.
         */

        // label marking the boundaries
        Set<String> boundaries = new HashSet<>();
        boundaries.add("Watershed");

        Set<String> dummy = new HashSet<>();
        dummy.add("dummy");

        // dummy to mark nodes as visited
        while (!pq.isEmpty()) {
            PixelIntensity<String> currentPI = pq.remove();
            Set<String> l = currentPI.getLabeling();
            currentPI.getPosition(position, dimensions);
            outputAccess.setPosition(position);
            imageAccess.setPosition(position);
            for (long[] offset : strelMoves) {
                outputAccess.move(offset);
                imageAccess.move(offset);
                final LabelingType<String> outputLabelingType = outputAccess.get();
                if (!Intervals.contains(output, outputAccess)) {
                    l = boundaries;
                    break;
                }

                if (outputLabelingType.isEmpty()) {
                    double intensity = imageAccess.get().getRealDouble();
                    outputAccess.localize(destPosition);
                    pq.add(new PixelIntensity<String>(destPosition, dimensions, intensity, age++, currentPI
                            .getLabeling()));

                    // dummy to mark positions as visited
                    outputLabelingType.clear();
                    outputLabelingType.addAll(dummy);

                } else if (!outputLabelingType.equals(l) && !outputLabelingType.equals(dummy)
                        && !outputLabelingType.equals(boundaries)) {
                    l = boundaries;
                }

            }
            outputAccess.setPosition(position);
            LabelingType<String> outputLabelingType = outputAccess.get();
            outputLabelingType.clear();
            outputLabelingType.addAll(l);
        }

        return output;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public
            BinaryObjectFactory<RandomAccessibleInterval<T>, RandomAccessibleInterval<LabelingType<L>>, RandomAccessibleInterval<LabelingType<String>>>
            bufferFactory() {
        return new BinaryObjectFactory<RandomAccessibleInterval<T>, RandomAccessibleInterval<LabelingType<L>>, RandomAccessibleInterval<LabelingType<String>>>() {

            @Override
            public RandomAccessibleInterval<LabelingType<String>>
                    instantiate(final RandomAccessibleInterval<T> inputA,
                                final RandomAccessibleInterval<LabelingType<L>> inputB) {
                return new ImgLabeling<String, IntType>(m_factory.create(inputA, new IntType()));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public
            BinaryOutputOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<LabelingType<L>>, RandomAccessibleInterval<LabelingType<String>>>
            copy() {
        return new WatershedWithSheds<T, L>(m_structuringElement.clone());
    }

}
