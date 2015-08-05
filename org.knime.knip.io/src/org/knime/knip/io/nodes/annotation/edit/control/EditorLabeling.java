package org.knime.knip.io.nodes.annotation.edit.control;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.converter.AbstractConvertedCursor;
import net.imglib2.converter.AbstractConvertedRandomAccess;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;

public class EditorLabeling extends ImgLabeling<String, IntType> {

	private final LabelingEditorConvertedRandomAccess m_ra;

	private final LabelingType<String> m_lt;

	public <T> EditorLabeling(ImgLabeling<T, IntType> originalLabeling) {

		super(originalLabeling.getIndexImg());
		m_lt = super.randomAccess().get();
		m_ra = (LabelingEditorConvertedRandomAccess)this.randomAccess();
		EditorLabelingAccess access = new EditorLabelingAccess(getMapping());
		LabelingMapping<T> oc = originalLabeling.getMapping();
		List<Set<String>> sets = new LinkedList<Set<String>>();
		for (int i = 0; i < oc.numSets(); ++i) {
			HashSet<String> s = new HashSet<String>();
			for (T elem : oc.labelsAtIndex(i))
				s.add(elem.toString());
			sets.add(s);
		}
		access.set(sets);
	}

	class LabelingEditorConvertedRandomAccess extends
			AbstractConvertedRandomAccess<IntType, LabelingType<String>> {
		private final EditorLabelingType type;

		public LabelingEditorConvertedRandomAccess(
				final RandomAccess<IntType> source) {
			super(source);
			this.type = new EditorLabelingType(source.get(), m_lt);
		}
		
		public void insert(int index, String label){
			type.insert(index, label);
		}

		@Override
		public LabelingType<String> get() {
			return type;
		}

		@Override
		public LabelingEditorConvertedRandomAccess copy() {
			return new LabelingEditorConvertedRandomAccess(
					source.copyRandomAccess());
		}
	}

	class LabelingEditorConvertedCursor extends
			AbstractConvertedCursor<IntType, LabelingType<String>> {
		private final LabelingType<String> type;

		public LabelingEditorConvertedCursor(final Cursor<IntType> source) {
			super(source);
			this.type = new EditorLabelingType(source.get(), m_lt);
		}

		@Override
		public LabelingType<String> get() {
			return type;
		}

		@Override
		public LabelingEditorConvertedCursor copy() {
			return new LabelingEditorConvertedCursor(source.copyCursor());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.imglib2.roi.labeling.ImgLabeling#randomAccess()
	 */
	@Override
	public RandomAccess<LabelingType<String>> randomAccess() {
		// TODO Auto-generated method stub
		return new LabelingEditorConvertedRandomAccess(getIndexImg()
				.randomAccess());
	}

	@Override
	public RandomAccess<LabelingType<String>> randomAccess(
			final Interval interval) {
		return new LabelingEditorConvertedRandomAccess(getIndexImg()
				.randomAccess(interval));
	}

	@Override
	public Cursor<LabelingType<String>> cursor() {
		return new LabelingEditorConvertedCursor(Views.iterable(getIndexImg())
				.cursor());
	}

	@Override
	public Cursor<LabelingType<String>> localizingCursor() {
		return new LabelingEditorConvertedCursor(Views.iterable(getIndexImg())
				.localizingCursor());
	}
	
	public void insert(int index, String label){
		m_ra.insert(index, label);
	}

	static class EditorLabelingAccess extends
			LabelingMapping.SerialisationAccess<String> {

		protected EditorLabelingAccess(
				@SuppressWarnings("hiding") final LabelingMapping<String> mapping) {
			super(mapping);

		}

		public void set(final List<Set<String>> labels) {
			super.setLabelSets(labels);
		}

		public List<Set<String>> get() {
			return super.getLabelSets();
		}

	}

}
