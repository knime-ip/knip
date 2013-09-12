package org.knime.knip.ops;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.converter.Converter;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.type.Type;

public class PersistentConverter<T extends Type<T>, V extends Type<V>>
		implements UnaryOperation<IterableInterval<T>, IterableInterval<V>> {

	private Converter<T, V> m_converter;

	public PersistentConverter(Converter<T, V> converter) {
		m_converter = converter;
	}

	@Override
	public IterableInterval<V> compute(IterableInterval<T> input,
			IterableInterval<V> output) {
		if (!input.iterationOrder().equals(output.iterationOrder()))
			throw new IllegalStateException("Iteration order not the same.");

		Cursor<T> inCursor = input.cursor();
		Cursor<V> outCursor = output.cursor();

		while (inCursor.hasNext()) {
			m_converter.convert(inCursor.next(), outCursor.next());
		}

		return output;
	}

	@Override
	public UnaryOperation<IterableInterval<T>, IterableInterval<V>> copy() {
		
		// TODO do we need to copy the converter or just synchronize convert?
		return new PersistentConverter<T, V>(m_converter);
	}

}
