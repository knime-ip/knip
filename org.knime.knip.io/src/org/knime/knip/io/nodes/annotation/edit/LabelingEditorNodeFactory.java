//package org.knime.knip.io.nodes.annotation.edit;
//
//import net.imglib2.type.NativeType;
//import net.imglib2.type.numeric.RealType;
//
//import org.knime.core.node.NodeDialogPane;
//import org.knime.core.node.NodeFactory;
//import org.knime.core.node.NodeView;
//import org.knime.knip.base.nodes.view.TableCellViewNodeView;
//
///**
// * NodeFactory of the InteractiveLabelingEditor Node.
// * @author Andreas Burger, University of Konstanz
// *
// * @param <T>
// * @param <L>
// */
//public class LabelingEditorNodeFactory<T extends RealType<T> & NativeType<T>, L extends Comparable<L>>
//		extends NodeFactory<LabelingEditorNodeModel<T>> {
//
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public LabelingEditorNodeModel<T> createNodeModel() {
//		return new LabelingEditorNodeModel<T>();
//	}
//
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public int getNrNodeViews() {
//		return 1;
//	}
//
//	/**
//	 * {@inheritDoc}
//	 */
//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	@Override
//	public NodeView<LabelingEditorNodeModel<T>> createNodeView(final int i,
//			final LabelingEditorNodeModel<T> nodeModel) {
//		return new TableCellViewNodeView(nodeModel);
//	}
//
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public NodeDialogPane createNodeDialogPane() {
//		return new LabelingEditorNodeDialog<T, L>();
//	}
//
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public boolean hasDialog() {
//		return true;
//	}
//}
