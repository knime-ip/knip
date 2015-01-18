package org.knime.knip.tracking.data;

import java.util.Map;

import net.imagej.ImgPlus;
import net.imglib2.type.logic.BitType;
import fiji.plugin.trackmate.FeatureHolder;
import fiji.plugin.trackmate.tracking.AbstractTrackableObject;

public class TrackedNode<L extends Comparable<L>> extends
		AbstractTrackableObject<TrackedNode<L>> implements Comparable<TrackedNode<L>>,
		FeatureHolder {

	private final Map<String, Double> m_features;
	private final L m_label;
	private final ImgPlus<BitType> m_bitMask;
	private final long[] m_offset;

	private int m_frame;
	private boolean m_isVisible;

	public TrackedNode(final ImgPlus<BitType> bitMask, final double[] center,
			final long[] offset, final L label, final int timeIdx,
			final Map<String, Double> features) {
		super(position(center, timeIdx), label.toString(),
				(int) center[timeIdx]);
		m_features = features;
		m_label = label;
		m_bitMask = bitMask;
		m_offset = offset;
		m_frame = (int) center[timeIdx];
		m_isVisible = true;
	}

	private static double[] position(final double[] centroid, final int timeIdx) {

		final double[] positionWithoutTime = new double[centroid.length - 1];

		int offset = 0;
		for (int i = 0; i < centroid.length; i++) {
			if (timeIdx == i) {
				offset = 1;
				continue;
			}
			positionWithoutTime[i - offset] = centroid[i];
		}

		return positionWithoutTime;
	}

	@Override
	public int compareTo(final TrackedNode<L> o) {
		return m_label.compareTo(o.m_label);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof TrackedNode) {
			return m_label.equals(((TrackedNode<L>) obj).m_label);
		}
		return false;
	}

	// same as above
	@Override
	public int hashCode() {
		return m_label.hashCode();
	}

	@Override
	public int ID() {
		return m_label.hashCode();
	}

	public ImgPlus<BitType> bitMask() {
		return m_bitMask;
	}

	public L label() {
		return m_label;
	}

	public long offset(final int d) {
		return m_offset[d];
	}

	@Override
	public String getName() {
		return m_bitMask.toString();
	}

	@Override
	public double radius() {
		// TODO find better solution here
		return Math.max(m_bitMask.dimension(0), m_bitMask.dimension(1));
	}

	@Override
	public void setName(final String name) {
		throw new UnsupportedOperationException("can't set name");
	}

	@Override
	public Double getFeature(final String feature) {
		return m_features.get(feature);
	}

	@Override
	public Map<String, Double> getFeatures() {
		return m_features;
	}

	@Override
	public void putFeature(final String feature, final Double value) {
		m_features.put(feature, value);
	}

	// FIXME (should be moved in abstract super class)
	@Override
	public int frame() {
		return m_frame;
	}

	@Override
	public void setFrame(final int frame) {
		this.m_frame = frame;
	}

	@Override
	public boolean isVisible() {
		return m_isVisible;
	}

	@Override
	public void setVisible(final boolean isVisible) {
		this.m_isVisible = isVisible;
	}
}
