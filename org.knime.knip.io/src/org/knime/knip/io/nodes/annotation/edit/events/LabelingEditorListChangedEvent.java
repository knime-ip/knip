//package org.knime.knip.io.nodes.annotation.edit.events;
//
//import java.util.List;
//
//import org.knime.knip.core.ui.event.KNIPEvent;
//import org.knime.knip.io.nodes.annotation.edit.control.LELabelingType;
//
///**
// * Used to notify the {@link LELabelingType} of changes to lists.
// * 
// * @author Andreas Burger, University of Konstanz
// * 
// */
//public class LabelingEditorListChangedEvent implements KNIPEvent {
//
//	private final List<String> m_list;
//
//	public LabelingEditorListChangedEvent(List<String> list) {
//		m_list = list;
//	}
//
//	public List<String> getList() {
//		return m_list;
//	}
//
//	@Override
//	public ExecutionPriority getExecutionOrder() {
//		return ExecutionPriority.NORMAL;
//	}
//
//	@Override
//	public <E extends KNIPEvent> boolean isRedundant(E thatEvent) {
//		return this.equals(thatEvent);
//	}
//
//}
