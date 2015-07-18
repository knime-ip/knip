package org.knime.knip.core.labeling;

import net.imglib2.Cursor;
import net.imglib2.FlatIterationOrder;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.NativeImgFactory;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.view.IterableRandomAccessibleInterval;
import net.imglib2.view.Views;
import net.imglib2.view.iteration.SubIntervalIterable;

/**
 * NB: This is only a marker interface for serialization/deserialization and will be entirely removed
 *
 * @author Christian Dietz (dietzc85@googlemail.com)
 *
 * @param <L>
 */
@Deprecated
public class LabelingView<L> extends IterableRandomAccessibleInterval<LabelingType<L>> implements
        SubIntervalIterable<LabelingType<L>> {

    private final IterableInterval<LabelingType<L>> m_ii;

    private final NativeImgFactory<?> m_fac;

    /**
     * @param in the {@link RandomAccessibleInterval} to be wrapped
     * @param fac factory to create a new {@link Labeling}
     */
    public LabelingView(final RandomAccessibleInterval<LabelingType<L>> in, final NativeImgFactory<?> fac) {
        super(in);
        m_fac = fac;
        m_ii = Views.flatIterable(in);
    }

    @Override
    public Cursor<LabelingType<L>> cursor() {
        return m_ii.cursor();
    }

    @Override
    public Cursor<LabelingType<L>> localizingCursor() {
        return m_ii.localizingCursor();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean supportsOptimizedCursor(final Interval interval) {
        if (this.sourceInterval instanceof SubIntervalIterable) {
            return ((SubIntervalIterable<LabelingType<L>>)this.sourceInterval).supportsOptimizedCursor(interval);
        } else {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object subIntervalIterationOrder(final Interval interval) {
        if (this.sourceInterval instanceof SubIntervalIterable) {
            return ((SubIntervalIterable<LabelingType<L>>)this.sourceInterval).subIntervalIterationOrder(interval);
        } else {
            return new FlatIterationOrder(interval);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Cursor<LabelingType<L>> cursor(final Interval interval) {
        if (this.sourceInterval instanceof SubIntervalIterable) {
            return ((SubIntervalIterable<LabelingType<L>>)this.sourceInterval).cursor(interval);
        } else {
            return Views.interval(this.sourceInterval, interval).cursor();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Cursor<LabelingType<L>> localizingCursor(final Interval interval) {
        if (this.sourceInterval instanceof SubIntervalIterable) {
            return ((SubIntervalIterable<LabelingType<L>>)this.sourceInterval).localizingCursor(interval);
        } else {
            return Views.interval(this.sourceInterval, interval).localizingCursor();
        }
    }

    /**
     * @return
     */
    public RandomAccessibleInterval<LabelingType<L>> getDelegate() {
        return sourceInterval;
    }

    /**
     * @return the m_fac
     */
    public NativeImgFactory<?> getFac() {
        return m_fac;
    }
}
