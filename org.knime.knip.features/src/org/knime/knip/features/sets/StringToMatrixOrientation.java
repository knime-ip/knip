package org.knime.knip.features.sets;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConversionRequest;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.image.cooccurrencematrix.MatrixOrientation;
import net.imagej.ops.image.cooccurrencematrix.MatrixOrientation2D;
import net.imagej.ops.image.cooccurrencematrix.MatrixOrientation3D;

@Plugin(type = Converter.class)
public class StringToMatrixOrientation extends AbstractConverter<String, MatrixOrientation> {

	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(Object src, Class<T> dest) {
		if (((String) src).contains("2D")) {
			return (T) MatrixOrientation2D.valueOf(((String) src).replace(" 2D", ""));
		}
		// else is 3D
		return (T) MatrixOrientation3D.valueOf(((String) src).replace(" 3D", ""));
	}

	@Override
	public Class<MatrixOrientation> getOutputType() {
		return MatrixOrientation.class;
	}

	@Override
	public Class<String> getInputType() {
		return String.class;
	}

	@Override
	public boolean canConvert(final ConversionRequest request) {
		return canConvert(request.sourceObject(), request.destClass());
	}

	@Override
	public boolean canConvert(final Object src, Class<?> dest) {
		if (!(src instanceof String) || !(((String) src).contains("2D") || ((String) src).contains("3D"))) {
			return false;
		}

		final String toTest = (String) src;

		if (toTest.contains("2D")) {
			final String tmp = ((String) src).replace(" 2D", "");
			for (MatrixOrientation2D matrix : MatrixOrientation2D.values()) {
				if (matrix.toString().equals(tmp)) {
					return true;
				}
			}

		} else if (toTest.contains("3D")) {
			final String tmp = ((String) src).replace(" 3D", "");
			for (MatrixOrientation3D matrix : MatrixOrientation3D.values()) {
				if (matrix.toString().equals(tmp)) {
					return true;
				}
			}
		}

		return false;

	}
}