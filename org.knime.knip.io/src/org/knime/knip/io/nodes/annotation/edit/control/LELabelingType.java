package org.knime.knip.io.nodes.annotation.edit.control;

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

	}

	@Override
	public LabelingType<String> copy() {
		return new LELabelingType(service, type.copy(), mapping);
	}
}