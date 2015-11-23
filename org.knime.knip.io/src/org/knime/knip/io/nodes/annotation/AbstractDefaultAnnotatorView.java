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
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ImgViewer;
import org.knime.knip.core.ui.imgviewer.annotator.RowColKey;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorResetEvent;

public abstract class AbstractDefaultAnnotatorView<A> implements AnnotatorView<A>, ListSelectionListener {

	protected final JPanel m_mainPanel = new JPanel();

	protected TableContentView m_tableContentView;

	private TableContentModel m_tableModel;

	private boolean m_clearSelection;

	protected int m_currentRow = -1;
	protected int m_currentCol = -1;

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

		m_tableContentView.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_tableContentView.getSelectionModel().addListSelectionListener(this);
		TableView tableView = new TableView(m_tableContentView);
		PlainCellView p = new PlainCellView(tableView, (ImgViewer) createAnnotatorComponent());

		m_mainPanel.setLayout(new BorderLayout());
		m_mainPanel.add(p, BorderLayout.CENTER);
	}

	protected TableContentView createTableContentModel() {
		return new TableContentView() {
			@Override
			public void clearSelection() {
				super.clearSelection();
				m_currentCol = -1;
				m_currentRow = -1;
			}
		};
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) {
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
		System.out.println("test");
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
}
