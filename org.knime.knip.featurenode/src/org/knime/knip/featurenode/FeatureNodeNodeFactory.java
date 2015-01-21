package org.knime.knip.featurenode;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "FeatureNode" Node.
 * 
 *
 * @author Daniel Seebacher
 */
public class FeatureNodeNodeFactory 
        extends NodeFactory<FeatureNodeNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureNodeNodeModel createNodeModel() {
        return new FeatureNodeNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<FeatureNodeNodeModel> createNodeView(final int viewIndex,
            final FeatureNodeNodeModel nodeModel) {
        return new FeatureNodeNodeView(nodeModel);
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

