package org.knime.knip.io.nodes.annotation.edit.control;

import java.util.List;
import java.util.Set;

import net.imglib2.roi.labeling.LabelingMapping;

public final class LabelingEditorLabelingAccess {

	private LabelingEditorLabelingAccess() {
	}

	public static List<Set<?>> getLabels(final LabelingMapping obj) {

		final LabelingMappingAccess access = new LabelingMappingAccess(obj);

		return access.get();

	}

	@SuppressWarnings({ "unchecked" })
	static class LabelingMappingAccess extends LabelingMapping.SerialisationAccess {

		private LabelingMapping mapping;

		/**
		 * @param mapping
		 */
		protected LabelingMappingAccess(@SuppressWarnings("hiding") final LabelingMapping mapping) {
			super(mapping);
			this.mapping = mapping;

		}

		public void set(final List<Set<?>> labels) {
			super.setLabelSets(labels);
		}

		public List<Set<?>> get() {
			return super.getLabelSets();
		}

		public LabelingMapping getMapping() {
			return mapping;
		}
	}

}
