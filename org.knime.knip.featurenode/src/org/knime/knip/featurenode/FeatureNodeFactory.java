package org.knime.knip.featurenode;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.knip.featurenode.view.FeatureNodeDialogPane;

/**
 * <code>NodeFactory</code> for the "FeatureNode" Node.
 * 
 *
 * @author Daniel Seebacher
 */
public class FeatureNodeFactory<T extends RealType<T> & NativeType<T>, L extends Comparable<L>>
        extends NodeFactory<FeatureNodeModel<T, L>> {

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureNodeModel<T, L> createNodeModel() {
        return new FeatureNodeModel<T, L>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<FeatureNodeModel<T, L>> createNodeView(final int viewIndex,
            final FeatureNodeModel<T, L> nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new FeatureNodeDialogPane<T, L>();
    }

}
