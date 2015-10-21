package org.knime.knip.base.nodes.view;

import org.knime.core.node.tableview.TableView;
import org.knime.knip.core.ui.event.KNIPEvent;
import org.knime.knip.core.ui.imgviewer.ImgViewer;

/**
 * This Class represents the Cell-side part of the TableCellViewer - plus some additional convenience features.
 *
 * @author Andreas Burger, University of Constance
 */
public class PlainCellView extends AbstractCellView {

    protected ImgViewer m_cellView;

    public PlainCellView(final TableView tableView, final ImgViewer cellView) {
        super(tableView);

        m_cellView = cellView;
        m_cellPanel.add(cellView);
        this.subscribe(cellView);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcastEvent(final KNIPEvent e) {
        m_cellView.getEventService().publish(e);
    }



}
