package org.knime.knip.featurenode;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "FeatureNode" Node.
 * 
 *
 * @author Daniel Seebacher
 */
public class FeatureNodeNodeFactory<T extends RealType<T> & NativeType<T>, L extends Comparable<L>>
        extends NodeFactory<FeatureNodeNodeModel<T, L>> {

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureNodeNodeModel<T, L> createNodeModel() {
        return new FeatureNodeNodeModel<T, L>();
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
    public NodeView<FeatureNodeNodeModel<T, L>> createNodeView(final int viewIndex,
            final FeatureNodeNodeModel<T, L> nodeModel) {
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
        return new FeatureNodeNodeDialog();
    }

}
