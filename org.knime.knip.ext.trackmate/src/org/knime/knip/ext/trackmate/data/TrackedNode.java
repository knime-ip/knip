package org.knime.knip.ext.trackmate.data;

import java.util.Map;

import net.imglib2.RealPoint;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.logic.BitType;
import fiji.plugin.trackmate.tracking.TrackableObject;

public class TrackedNode<L extends Comparable<L>> implements
		Comparable<TrackedNode<L>>, TrackableObject {

	private final RealPoint m_point;
	private final Map<String, Double> m_features;
	private final L m_label;
	private final int m_timeIdx;
	private final ImgPlus<BitType> m_bitMask;
	private final long[] m_offset;

	public TrackedNode(ImgPlus<BitType> bitMask, long[] offset, L label,
			int timeIdx, Map<String, Double> features) {
		m_features = features;
		m_label = label;
		m_timeIdx = timeIdx;
		m_bitMask = bitMask;
		m_offset = offset;
		
		double[] centroid = new BitMaskCentroid(offset).compute(bitMask,
				new double[bitMask.numDimensions()]);

		m_point = new RealPoint(centroid);
	}

	@Override
	public int compareTo(TrackedNode<L> o) {
		return m_label.compareTo(o.m_label);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
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
	public void localize(float[] position) {
		m_point.localize(position);
	}

	@Override
	public void localize(double[] position) {
		m_point.localize(position);
	}

	@Override
	public float getFloatPosition(int d) {
		return m_point.getFloatPosition(d);
	}

	@Override
	public double getDoublePosition(int d) {
		return m_point.getDoublePosition(d);
	}

	@Override
	public int numDimensions() {
		return m_point.numDimensions();
	}

	@Override
	public Double getFeature(String feature) {
		if (m_features.containsKey(feature)) {
			return m_features.get(feature);
		} else {
			throw new IllegalArgumentException("Can't find feature");
		}
	}

	@Override
	public void putFeature(String feature, Double value) {
		m_features.put(feature, value);
	}

	@Override
	public int frame() {
		return (int) m_point.getFloatPosition(m_timeIdx);
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

	public long offset(int d) {
		return m_offset[d];
	}
}
