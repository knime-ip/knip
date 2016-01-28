package org.knime.knip.tracking.nodes.laptracker;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.knip.base.nodes.view.TableCellViewNodeView;

public class LAPTrackerNodeFactory extends NodeFactory<LAPTrackerNodeModel> {

    @Override
    public LAPTrackerNodeModel createNodeModel() {
        return new LAPTrackerNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 1;
    }

    @Override
    public NodeView<LAPTrackerNodeModel> createNodeView(int viewIndex,
            LAPTrackerNodeModel nodeModel) {
        return new TableCellViewNodeView<>(nodeModel);
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new LAPTrackerNodeDialog();
    }

}
