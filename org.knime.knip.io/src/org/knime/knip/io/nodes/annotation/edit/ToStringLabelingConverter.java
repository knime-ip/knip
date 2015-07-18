package org.knime.knip.io.nodes.annotation.edit;

import net.imglib2.converter.Converter;
import net.imglib2.roi.labeling.LabelingType;

/**
 * Converts any labeling to a String labeling.
 * 
 * @author Andreas Burger, University of Konstanz
 * 
 * @param <T>
 */
public class ToStringLabelingConverter<T extends Comparable<T>> implements
		Converter<LabelingType<T>, LabelingType<String>> {

	@Override
	public void convert(LabelingType<T> input, LabelingType<String> output) {
		output.clear();
		for (T label : input)
			output.add(label.toString());
	}

}
