package org.knime.knip.io.nodes.annotation.edit;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.converter.Converter;
import net.imglib2.labeling.LabelingType;

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
		List<String> res = new ArrayList<String>(input.getLabeling().size());

		for (int i = 0; i < input.getLabeling().size(); ++i)
			res.add(input.getLabeling().get(i).toString());

		output.setLabeling(res);

	}

}
