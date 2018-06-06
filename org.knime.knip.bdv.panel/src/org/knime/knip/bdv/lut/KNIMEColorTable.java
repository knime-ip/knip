package org.knime.knip.bdv.lut;

import java.util.Set;

import org.knime.knip.core.awt.labelingcolortable.RandomMissingColorHandler;
import org.scijava.event.EventService;

import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;

public class KNIMEColorTable<T extends NumericType<T>, L, I extends IntegerType<I>>
		extends SegmentsColorTable<T, L, I> {

	private RandomMissingColorHandler colorHandler;

	public KNIMEColorTable(LabelingMapping<L> mapping, ColorTableConverter<L> converter,
			RandomMissingColorHandler colorHandler, EventService es) {
		super(mapping, converter, es);
		this.colorHandler = colorHandler;
	}

	@Override
	public void fillLut() {
		for (int i = 0; i < labelingMapping.numSets(); i++) {
			final Set<L> labelSet = labelingMapping.labelsAtIndex(i);
			for (L l : labelSet) {
				final int color = colorHandler.getColor(l);
				lut[i] = ARGBType.rgba(ARGBType.red(color), ARGBType.green(color), ARGBType.blue(color), alpha);
			}
		}
	}
	
	@Override
	public void newColors() {
		RandomMissingColorHandler.resetColorMap();
		fillLut();
		super.update();
	}

}
