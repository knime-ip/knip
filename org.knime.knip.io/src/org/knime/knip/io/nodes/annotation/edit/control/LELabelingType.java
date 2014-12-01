package org.knime.knip.io.nodes.annotation.edit.control;

import java.util.List;

import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.LabelingType;
import net.imglib2.type.numeric.IntegerType;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.io.nodes.annotation.edit.events.LabelingEditorListChangedEvent;

public class LELabelingType extends LabelingType<String> {

	private final EventService service;

	public LELabelingType(EventService service) {
		super();

		this.service = service;
		service.subscribe(this);
	}

	protected LELabelingType(EventService service, IntegerType<?> type,
			LabelingMapping<String> mapping) {
		super(type, mapping);

		this.service = service;
		service.subscribe(this);
	}

	@EventListener
	public void onListEdited(LabelingEditorListChangedEvent e) {
		getMapping().intern(e.getList());
		synchronized (generation) {
			generation[0]++;
		}
	}

	/**
	 * Set the labeling at the current pixel
	 * 
	 * @param labeling
	 */
	@Override
	public void setLabeling(final List<String> labeling) {
		this.type.setInteger(mapping.indexOf(labeling));
	}

	@Override
	public LabelingType<String> copy() {
		return new LELabelingType(service, type.copy(), mapping);
	}
}