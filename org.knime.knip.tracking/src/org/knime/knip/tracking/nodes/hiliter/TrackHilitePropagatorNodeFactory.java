package org.knime.knip.tracking.nodes.hiliter;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class TrackHilitePropagatorNodeFactory extends
        NodeFactory<TrackHilitePropagatorNodeModel> {

    @Override
    public TrackHilitePropagatorNodeModel createNodeModel() {
        return new TrackHilitePropagatorNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<TrackHilitePropagatorNodeModel> createNodeView(
            final int viewIndex, final TrackHilitePropagatorNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new TrackHilitePropagatorNodeDialog();
    }

}
