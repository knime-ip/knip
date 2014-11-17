package org.knime.knip.tracking.nodes.trackmate;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.knip.base.nodes.view.TableCellViewNodeView;

/**
 * Node Factory for the Trackmate Tracker Node.
 *
 * @author gabriel
 * @author christian
 *
 */
public class TrackmateTrackerNodeFactory extends
        NodeFactory<TrackmateTrackerNodeModel> {

    @Override
    public TrackmateTrackerNodeModel createNodeModel() {
        return new TrackmateTrackerNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 1;
    }

    @Override
    public NodeView<TrackmateTrackerNodeModel> createNodeView(
            final int viewIndex, final TrackmateTrackerNodeModel nodeModel) {
        return new TableCellViewNodeView<TrackmateTrackerNodeModel>(nodeModel);
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new TrackmateTrackerNodeDialog();
    }

}
