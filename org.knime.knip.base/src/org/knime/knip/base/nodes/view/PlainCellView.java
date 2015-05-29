package org.knime.knip.base.nodes.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.knime.core.node.tableview.TableView;
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
        m_verticalSplit.setTopComponent(m_cellView);

        m_cellView.getBottomQuickViewButton().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                setTableViewVisible(!isTableViewVisible(TableDir.BOTTOM), TableDir.BOTTOM);

            }
        });

        m_cellView.getLeftQuickViewButton().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                setTableViewVisible(!isTableViewVisible(TableDir.LEFT), TableDir.LEFT);

            }
        });

    }

}
