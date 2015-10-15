package org.knime.knip.io.nodes.annotation.edit.control;

import java.util.HashSet;
import java.util.Set;

import net.imglib2.converter.Converter;
import net.imglib2.roi.labeling.LabelingType;

public class LabelingEditorLabelingConverter<T> implements Converter<LabelingType<T>, LabelingType<String>> {

	private final LabelingEditorChangeTracker m_tracker;

	public LabelingEditorLabelingConverter(LabelingEditorChangeTracker tracker) {
		m_tracker = tracker;
	}

	@Override
	public void convert(LabelingType<T> input, LabelingType<String> output) {
		output.clear();

		Set<String> stringInput = new HashSet<String>();
		for (T in : input)
			stringInput.add(in.toString());

		Set<String> outputSet = m_tracker.get(stringInput);

		if (m_tracker.isFilteringEnabled()) {
			if (!input.isEmpty()) {
				outputSet.retainAll(m_tracker.getFilteredLabels());

				if (outputSet.isEmpty())
					outputSet.add("#");
			}
		}

		output.addAll(outputSet);

	}
}
