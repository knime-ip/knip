package org.knime.knip.io.nodes.annotation;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTable;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.core.node.tableview.TableContentView;
import org.knime.core.node.tableview.TableView;
import org.knime.knip.base.nodes.view.PlainCellView;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.annotator.RowColKey;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorResetEvent;
import org.knime.knip.core.ui.imgviewer.events.TableOverviewDisableEvent;
import org.knime.knip.core.ui.imgviewer.panels.ViewerControlEvent;
import org.knime.knip.core.ui.imgviewer.panels.ViewerScrollEvent;
import org.knime.knip.core.ui.imgviewer.panels.ViewerScrollEvent.Direction;

public abstract class AbstractDefaultAnnotatorView<A> implements AnnotatorView<A>, ListSelectionListener {

	protected final JPanel m_mainPanel = new JPanel();

	protected TableContentView m_tableContentView;

	protected TableView m_tableView;

	private TableContentModel m_tableModel;

	protected PlainCellView m_view;

	protected int m_currentRow = -1;
	protected int m_currentCol = -1;

	protected boolean isViewActive;

	protected abstract JComponent createAnnotatorComponent();

	@Override
	public JPanel getAnnotatorPanel() {
		return m_mainPanel;
	}

	@Override
	public void reset() {
		getEventService().publish(new AnnotatorResetEvent());
		m_currentRow = -1;
		m_currentCol = -1;
	}

	@Override
	public void setInputTable(DataTable inputTable) {
		m_tableModel = new TableContentModel(inputTable);
		m_tableContentView.setModel(m_tableModel);
		// Scale to thumbnail size
		m_tableContentView.validate();
		m_tableContentView.repaint();
		m_tableContentView.changeSelection(0, 0, false, false);
	}

	/*
	 * This method has to be called in the constructor!
	 */
	protected void createAnnotator() {
		// table viewer
		m_tableContentView = createTableContentModel();
		m_tableView = new TableView(m_tableContentView);
		m_tableContentView.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_tableContentView.getSelectionModel().addListSelectionListener(this);
		m_tableContentView.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_tableContentView.getColumnModel().getSelectionModel().addListSelectionListener(this);
		m_view = new PlainCellView(m_tableView, (ImgViewer) createAnnotatorComponent());
		m_view.setEventService(getEventService());
		getEventService().publish(new TableOverviewDisableEvent(false, true));
		m_mainPanel.setLayout(new BorderLayout());
		m_mainPanel.add(m_view, BorderLayout.CENTER);
		isViewActive = true;
	}

	protected TableContentView createTableContentModel() {
		TableContentView v = new TableContentView() {
			@Override
			public void clearSelection() {
				super.clearSelection();
				m_currentCol = -1;
				m_currentRow = -1;
			}
		};

		return v;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		// Ensures that the listener fires exactly once per selection
		if (m_tableContentView.getSelectionModel().getValueIsAdjusting()
				&& m_tableContentView.getColumnModel().getSelectionModel().getValueIsAdjusting()) {
			return;
		}

		final int row = m_tableContentView.getSelectionModel().getLeadSelectionIndex();
		final int col = m_tableContentView.getColumnModel().getSelectionModel().getLeadSelectionIndex();

		rowSelectionChanged(row, col);

	}

	protected void rowSelectionChanged(int row, int col) {

		if (row == -1 || col == -1 || (m_currentRow == row && m_currentCol == col)) {
			return;
		}
		if (row < m_tableContentView.getRowCount() && col < m_tableContentView.getColumnCount() && row >= 0
				&& col >= 0) {
			m_currentRow = row;
			m_currentCol = col;
		}

		try {
			int nrCols = m_tableContentView.getContentModel().getColumnCount();
			final DataCell[] currentCells = new DataCell[nrCols];

			for (int i = 0; i < currentCells.length; i++) {
				currentCells[i] = m_tableContentView.getContentModel().getValueAt(m_currentRow, i);
			}

			String colName = m_tableContentView.getColumnName(m_currentCol);
			String rowName = m_tableModel.getRowKey(m_currentRow).getString();

			currentSelectionChanged(currentCells, m_currentCol, new RowColKey(rowName, colName));
		} catch (final IndexOutOfBoundsException e2) {
			return;
		}
	}

	protected abstract void currentSelectionChanged(DataCell[] currentRow, int currentColNr, RowColKey key);

	protected abstract EventService getEventService();

	@EventListener
	public void onViewerScrollEvent(final ViewerScrollEvent e) {

		if (e.getDirection() == Direction.NORTH) {
			rowSelectionChanged(m_currentRow - 1, m_currentCol);
		}
		if(e.getDirection() == Direction.EAST){
			rowSelectionChanged(m_currentRow, m_currentCol+1);
		}
		if (e.getDirection() == Direction.SOUTH) {
			rowSelectionChanged(m_currentRow + 1, m_currentCol);
		}
		if(e.getDirection() == Direction.WEST){
			rowSelectionChanged(m_currentRow, m_currentCol-1);
		}

	}

	@EventListener
	public void onOverviewToggle(final ViewerControlEvent e) {
		if (isViewActive) {
			if (m_view.isTableViewVisible()) {
				m_view.hideTableView();
			}
			m_tableContentView.clearSelection();
			m_mainPanel.removeAll();
			m_mainPanel.add(m_tableView, BorderLayout.CENTER);
			m_mainPanel.revalidate();
			isViewActive = false;
		}

	}
}
