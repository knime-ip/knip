package org.knime.knip.io.nodes.annotation;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTable;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.core.node.tableview.TableContentView;
import org.knime.core.node.tableview.TableView;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.annotator.RowColKey;
import org.knime.knip.core.ui.imgviewer.annotator.events.AnnotatorResetEvent;

public abstract class AbstractDefaultAnnotatorView<A> implements
		AnnotatorView<A>, ListSelectionListener {

	private final JPanel m_mainPanel = new JPanel();

	private TableContentView m_tableContentView;

	private TableContentModel m_tableModel;
	
	private int m_currentRow = -1;
	private int m_currentCol = -1;

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
	}

	/*
	 * This method has to be called in the constructor!
	 */
	protected void createAnnotator() {
		// table viewer
		m_tableContentView = new TableContentView();
		m_tableContentView.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);
		m_tableContentView.getSelectionModel().addListSelectionListener(this);
		m_tableContentView.getColumnModel().getSelectionModel()
				.addListSelectionListener(this);
		TableView tableView = new TableView(m_tableContentView);

		// split pane
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.add(tableView);
		splitPane.add(createAnnotatorComponent());
		splitPane.setDividerLocation(300);

		m_mainPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;

		m_mainPanel.add(splitPane, gbc);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		final int row = m_tableContentView.getSelectionModel()
				.getLeadSelectionIndex();
		final int col = m_tableContentView.getColumnModel().getSelectionModel()
				.getLeadSelectionIndex();

		if ((row == m_currentRow && col == m_currentCol)
				|| e.getValueIsAdjusting()) {
			return;
		}

		m_currentRow = row;
		m_currentCol = col;

		try {
			int nrCols = m_tableContentView.getContentModel().getColumnCount();
			final DataCell[] currentCells = new DataCell[nrCols];
			
			for (int i = 0; i < currentCells.length; i++) {
				currentCells[i] = m_tableContentView.getContentModel()
						.getValueAt(m_currentRow, i);
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
