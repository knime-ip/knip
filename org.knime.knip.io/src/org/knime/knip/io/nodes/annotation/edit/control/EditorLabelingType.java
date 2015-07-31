package org.knime.knip.io.nodes.annotation.edit.control;

import java.util.Collection;
import java.util.Iterator;

import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.IntegerType;

public class EditorLabelingType extends LabelingType<String> {

	protected final LabelingType<String> m_labeling;
	private IntegerType<?> m_type;

	protected EditorLabelingType(final IntegerType<?> type,
			LabelingType<String> wrapped) {
		super(type, null, null);
		m_labeling = wrapped;
		m_type = type;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#add(java.lang.Object)
	 */
	@Override
	public boolean add(String label) {
		// TODO Auto-generated method stub
		return m_labeling.add(label);
	}

	/*
	 * arg0 (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#addAll(java.util.Collection)
	 */
	@Override
	public boolean addAll(Collection<? extends String> arg0) {
		// TODO Auto-generated method stub
		return m_labeling.addAll(arg0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#clear()
	 */
	@Override
	public void clear() {
		// TODO Auto-generated method stub
		m_labeling.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#contains(java.lang.Object)
	 */
	@Override
	public boolean contains(Object label) {
		// TODO Auto-generated method stub
		return m_labeling.contains(label);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.imglib2.roi.labeling.LabelingType#containsAll(java.util.Collection)
	 */
	@Override
	public boolean containsAll(Collection<?> labels) {
		// TODO Auto-generated method stub
		return m_labeling.containsAll(labels);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#copy()
	 */
	@Override
	public LabelingType<String> copy() {
		// TODO Auto-generated method stub
		return m_labeling.copy();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#createVariable()
	 */
	@Override
	public LabelingType<String> createVariable() {
		// TODO Auto-generated method stub
		return m_labeling.createVariable();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.imglib2.roi.labeling.LabelingType#createVariable(java.lang.Class)
	 */
	@Override
	public <L> LabelingType<L> createVariable(Class<? extends L> newType) {
		// TODO Auto-generated method stub
		return m_labeling.createVariable(newType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object arg0) {
		// TODO Auto-generated method stub
		return m_labeling.equals(arg0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#getGeneration()
	 */
	@Override
	public int getGeneration() {
		// TODO Auto-generated method stub
		return m_labeling.getGeneration();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#getIndex()
	 */
	@Override
	public IntegerType<?> getIndex() {
		// TODO Auto-generated method stub
		return m_labeling.getIndex();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#getMapping()
	 */
	@Override
	public LabelingMapping<String> getMapping() {
		// TODO Auto-generated method stub
		return m_labeling.getMapping();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#hashCode()
	 */
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return m_labeling.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return m_labeling.isEmpty();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#iterator()
	 */
	@Override
	public Iterator<String> iterator() {
		m_labeling.getIndex().setInteger(m_type.getInteger());
		return m_labeling.iterator();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#remove(java.lang.Object)
	 */
	@Override
	public boolean remove(Object label) {
		// TODO Auto-generated method stub
		return m_labeling.remove(label);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.imglib2.roi.labeling.LabelingType#removeAll(java.util.Collection)
	 */
	@Override
	public boolean removeAll(Collection<?> arg0) {
		// TODO Auto-generated method stub
		return m_labeling.removeAll(arg0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.imglib2.roi.labeling.LabelingType#retainAll(java.util.Collection)
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return m_labeling.retainAll(c);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#set(net.imglib2.roi.labeling.
	 * LabelingType)
	 */
	@Override
	public void set(LabelingType<String> c) {
		// TODO Auto-generated method stub
		m_labeling.set(c);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#size()
	 */
	@Override
	public int size() {
		// TODO Auto-generated method stub
		return m_labeling.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#toArray()
	 */
	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return m_labeling.toArray();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#toArray(java.lang.Object[])
	 */
	@Override
	public <T1> T1[] toArray(T1[] a) {
		// TODO Auto-generated method stub
		return m_labeling.toArray(a);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.LabelingType#toString()
	 */
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return m_labeling.toString();
	}

}