/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.io.nodes.imgreader;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;

import org.knime.base.util.WildcardMatcher;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.events.FileChooserSelectedFilesChgEvent;
import org.knime.knip.io.node.dialog.ImagePreviewPanel;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class FileChooserPanel extends JPanel {

	class ApplyFileView extends FileView {
		Icon newicon = new ImageIcon(getClass().getResource("button_ok.png"));

		@Override
		public Icon getIcon(final File file) {
			if (file.isDirectory()) {
				return null;
			}
			Icon icon = null;
			if (m_selectedFileListModel.contains(file)) {
				icon = newicon;
			}
			return icon;

		}
	}

	private class FileListModel implements ListModel, Comparator<File> {

		private final ArrayList<ListDataListener> listener = new ArrayList<ListDataListener>();

		private final Vector<File> m_files = new Vector<File>();

		public void addFiles(final File[] files, final FileFilter fileFilter) {

			final LinkedList<File> directoryQueue = new LinkedList<File>();

			// init
			insertTopFiles(directoryQueue, fileFilter, files);

			// loop
			while (!directoryQueue.isEmpty()) {
				final File dir = directoryQueue.poll();
				insertTopFiles(directoryQueue, fileFilter, dir.listFiles());
			}

			notifyListDataListener();

		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void addListDataListener(final ListDataListener arg0) {
			listener.add(arg0);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int compare(final File o1, final File o2) {
			return o1.getName().compareToIgnoreCase(o2.getName());
		}

		public Boolean contains(final File f) {
			return m_files.contains(f);
		}

		public String getAbsolutePathAt(final int idx) {
			return m_files.get(idx).getAbsolutePath();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object getElementAt(final int arg0) {
			return m_files.get(arg0).getName();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getSize() {
			return m_files.size();
		}

		private void insertTopFiles(final LinkedList<File> directoryQueue,
				final FileFilter fileFilter, final File[] files) {
			for (final File f : files) {
				if (f.isDirectory()) {
					directoryQueue.add(f);
				} else {
					if (fileFilter.accept(f) && !m_files.contains(f)) {
						m_files.add(f);
					}
				}
			}
		}

		private void notifyListDataListener() {
			for (final ListDataListener l : listener) {
				l.contentsChanged(new ListDataEvent(this,
						ListDataEvent.CONTENTS_CHANGED, 0, getSize()));
			}

		}

		public void remove(final int[] indices) {
			for (int i = indices.length - 1; i >= 0; i--) {
				m_files.remove(indices[i]);
			}
			notifyListDataListener();
		}

		public void removeAll() {
			m_files.clear();
			notifyListDataListener();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void removeListDataListener(final ListDataListener arg0) {
			listener.remove(arg0);

		}

		public void removeMenu(final File[] files) {
			for (final File f : files) {
				m_files.remove(f);
			}

		}
	}

	// a class to show the image preview with only one click (File
	// Selection)
	class ImagePreviewListener implements PropertyChangeListener {
		ImagePreviewPanel m_imagePreviewPanel;

		public ImagePreviewListener(final ImagePreviewPanel imagePreviewPanel) {
			m_imagePreviewPanel = imagePreviewPanel;
		}

		@Override
		public void propertyChange(final PropertyChangeEvent e) {

			if (e.getNewValue() instanceof File) {
				final String propertyName = e.getPropertyName();
				final File selection = (File) e.getNewValue();
				if (propertyName
						.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {

					m_imagePreviewPanel.setImage(selection.toString());
				}
			}
		}

	}

	class MacHackedFileChooserPanel extends JFileChooser {

		/**
         *
         */
		private static final long serialVersionUID = 1L;

		@Override
		protected void firePropertyChange(final String propertyName,
				final Object oldValue, final Object newValue) {
			try {
				super.firePropertyChange(propertyName, oldValue, newValue);
			} catch (final Exception e) { // This is a hack to avoid
				// stupid mac behaviour
			}

		}
	}

	static final long serialVersionUID = 1;

	/**
	 * Shows a modal dialog to select some files
	 * 
	 * @param file_list
	 *            default files in the selected file list
	 * @return the file list returns <code>null</code> if the dialog was closed
	 *         with cancel, or ESC or X
	 */

	public static String[] showFileChooserDialog(final FileFilter filter,
			final String[] file_list) {

		final JDialog dlg = new JDialog();
		dlg.setAlwaysOnTop(true);
		dlg.setModalityType(ModalityType.APPLICATION_MODAL);
		final int[] cancelFlag = new int[] { 0 };

		final FileChooserPanel panel = new FileChooserPanel(filter);
		if (file_list != null) {
			panel.update(file_list);
		}

		final JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				cancelFlag[0] = -1;
				dlg.dispose();

			}
		});
		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				dlg.dispose();
			}
		});
		final JPanel buttons = new JPanel();
		buttons.add(ok);
		buttons.add(cancel);
		dlg.getContentPane().setLayout(new BorderLayout());
		dlg.getContentPane().add(panel, BorderLayout.CENTER);
		dlg.getContentPane().add(buttons, BorderLayout.SOUTH);

		dlg.pack();
		dlg.setVisible(true);

		if (cancelFlag[0] == -1) {
			return panel.getSelectedFiles();
		} else {
			return null;
		}
	}

	private final File m_defDir;

	private final FileFilter m_fileFilter;

	private final Preferences m_filePref = Preferences
			.userNodeForPackage(getClass());

	private final JButton m_addAllButton;

	// buttons to selected/remove files
	private final JButton m_addButton;

	private EventService m_eventService;

	private final MacHackedFileChooserPanel m_fileChooser;

	private final ImagePreviewPanel m_imagePreviewPanel;

	private List<ChangeListener> m_listeners;

	private final ImagePreviewListener m_previewListener;

	private final JButton m_remAllButton;

	// /**
	// * Creates an new file chooser panel with no files filtered.
	// */
	// public FileChooserPanel2() {
	// this(null);
	// }

	private final JButton m_remButton;

	/*
	 * action for the add button
	 */

	// list and its model keeping the selected files
	private final JList m_selectedFileList;

	/*
	 * 
	 * action add all button
	 */

	private final TitledBorder m_selectedFileListBorder;

	private final FileListModel m_selectedFileListModel;

	private final JPopupMenu popup = new JPopupMenu();

	/**
	 * Creates a new file chooser panel
	 * 
	 * @param fileFilter
	 *            available file name extension filters
	 */
	public FileChooserPanel(final FileFilter fileFilter) {
		final String prefDir = m_filePref.get("Path", "null");
		this.m_fileFilter = fileFilter;

		m_defDir = new File(prefDir);
		// System.out.println(defDir.toString());
		// create instances
		m_fileChooser = new MacHackedFileChooserPanel();

		m_addButton = new JButton("Add Selected", new ImageIcon(getClass()
				.getResource("button_ok.png")));
		m_addAllButton = new JButton("Add all visible files", new ImageIcon(
				getClass().getResource("edit_add.png")));
		m_remButton = new JButton("Remove selected", new ImageIcon(getClass()
				.getResource("editdelete.png")));
		m_remAllButton = new JButton("Remove all Files", new ImageIcon(
				getClass().getResource("edit_remove.png")));
		m_selectedFileList = new JList();
		m_selectedFileListModel = new FileListModel();
		m_selectedFileListBorder = BorderFactory
				.createTitledBorder("Selected files");
		final JScrollPane jspSelFileList = new JScrollPane(m_selectedFileList);

		m_selectedFileList.setModel(m_selectedFileListModel);

		if (m_selectedFileListModel.getSize() > 0) {
			m_fileChooser.setCurrentDirectory(new File(m_selectedFileListModel
					.getAbsolutePathAt(0)));
		}
		// arrange the components
		final JPanel left = new JPanel();
		left.setBorder(BorderFactory.createTitledBorder("File browser"));
		final JPanel right = new JPanel();
		right.setBorder(m_selectedFileListBorder);
		final JPanel center = new JPanel();
		center.setBorder(BorderFactory.createTitledBorder("Selection"));

		left.setLayout(new BorderLayout());
		right.setLayout(new BorderLayout());
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

		final JPanel buttonPan = new JPanel();
		buttonPan.setLayout(new BoxLayout(buttonPan, BoxLayout.X_AXIS));
		buttonPan.add(Box.createVerticalStrut(20));
		final JPanel delButtonPan = new JPanel();
		delButtonPan.setLayout(new BoxLayout(delButtonPan, BoxLayout.X_AXIS));
		delButtonPan.add(Box.createVerticalStrut(20));
		m_addButton.setMaximumSize(new Dimension(150, 25));
		m_addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPan.add(m_addButton);
		buttonPan.add(Box.createVerticalStrut(20));
		m_addAllButton.setMaximumSize(new Dimension(150, 25));
		m_addAllButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPan.add(m_addAllButton);
		buttonPan.add(Box.createVerticalStrut(20));
		m_remButton.setMaximumSize(new Dimension(150, 25));
		m_remButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		delButtonPan.add(m_remButton);
		delButtonPan.add(Box.createVerticalStrut(20));
		m_remAllButton.setMaximumSize(new Dimension(150, 25));
		m_remAllButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		delButtonPan.add(m_remAllButton);
		delButtonPan.add(Box.createVerticalStrut(20));

		m_imagePreviewPanel = new ImagePreviewPanel();

		final ApplyFileView view = new ApplyFileView();
		m_fileChooser.setFileView(view);

		// buttonPan.add(m_imagePreviewPanel);
		// buttonPan.add(Box.createGlue());
		m_fileChooser.setPreferredSize(new Dimension(300, 100));
		final JPanel browsePane = new JPanel();
		browsePane.setLayout(new BoxLayout(browsePane, BoxLayout.Y_AXIS));

		browsePane.add(m_fileChooser);

		if (fileFilter != null) {
			// m_fileChooser.setFileFilter(fileFilter);
			m_fileChooser.setFileFilter(fileFilter);

		}

		final JTabbedPane rightTab = new JTabbedPane();
		m_fileChooser.setMultiSelectionEnabled(true);
		m_fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		m_fileChooser.setControlButtonsAreShown(false);
		m_fileChooser.setPreferredSize(new Dimension(450, 340));
		// center.add(buttonPan);
		// rightTab.setPreferredSize(new Dimension(400, 300));
		// browsePane.setPreferredSize(new Dimension(600, 500));
		enterHack(m_fileChooser.getComponents());
		right.add(rightTab);
		left.add(browsePane);
		browsePane.add(buttonPan);

		final JPanel selectedPane = new JPanel();
		selectedPane.setLayout(new BoxLayout(selectedPane, BoxLayout.Y_AXIS));
		selectedPane.add(jspSelFileList);
		selectedPane.add(delButtonPan);

		// browsePane.add(m_addAllButton);
		rightTab.add("Selected Files", selectedPane);
		rightTab.add("Preview/Meta-Data", m_imagePreviewPanel);

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(left);
		// add(center);
		add(right);
		m_fileChooser.setComponentPopupMenu(popup);

		final JMenuItem add = new JMenuItem("Add selected File", new ImageIcon(
				getClass().getResource("button_ok.png")));
		add.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				onAdd();
				fireSelectionChangedEvent();
			}

		});

		final JMenuItem addAll = new JMenuItem("Add all visible files",
				new ImageIcon(getClass().getResource("edit_add.png")));
		addAll.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				onAddAllTopLevelFiles();
				fireSelectionChangedEvent();
			}

		});

		final JMenuItem clearSelection = new JMenuItem("Remove All",
				new ImageIcon(getClass().getResource("edit_remove.png")));
		clearSelection.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				m_selectedFileListModel.removeAll();
				fireSelectionChangedEvent();
			}

		});

		final JMenuItem remove = new JMenuItem("Remove Selected",
				new ImageIcon(getClass().getResource("editdelete.png")));
		remove.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				m_selectedFileListModel.removeMenu(m_fileChooser
						.getSelectedFiles());

				fireSelectionChangedEvent();
			}
		});
		final JSeparator sep = new JSeparator();

		popup.add(sep);
		popup.add(add);
		popup.add(addAll);
		popup.add(sep);
		popup.add(remove);
		popup.add(clearSelection);

		// Show preview and metadata from the selected file
		m_previewListener = new ImagePreviewListener(m_imagePreviewPanel);
		m_fileChooser.addPropertyChangeListener(m_previewListener);

		m_addButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				onAdd();
			}
		});

		m_addAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				onAddAllTopLevelFiles();
			}
		});

		m_remButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {

				onRemove();
			}

		});

		m_remAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				onRemoveAll();
			}
		});

		m_fileChooser.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				onAdd();
			}
		});

		m_selectedFileList.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(final MouseEvent arg0) {
				if (arg0.getClickCount() == 2) {

					final File dir = new File(m_selectedFileListModel
							.getAbsolutePathAt(m_selectedFileList
									.getSelectedIndices()[0]));
					m_fileChooser.setCurrentDirectory(dir);
					m_selectedFileList.clearSelection();

				}

			}

			@Override
			public void mouseEntered(final MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseExited(final MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(final MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseReleased(final MouseEvent arg0) {
				// TODO Auto-generated method stub

			}
		});

		m_fileChooser.setCurrentDirectory(m_defDir);
		// m_fileChooser.setVisible(false);
		// m_fileChooser.setVisible(true);
	}

	public void enterHack(final Component[] comp) {
		for (int x = 0; x < comp.length; x++) {
			if (comp[x] instanceof JPanel) {
				enterHack(((JPanel) comp[x]).getComponents());
			} else if (comp[x] instanceof JTextField) {
				final JTextField fl = ((JTextField) comp[x]);
				((JTextField) comp[x]).addKeyListener(new KeyListener() {
					@Override
					public void keyPressed(final KeyEvent event) {

						if (event.getKeyCode() == KeyEvent.VK_ENTER) {
							enterOnTextField(event, fl.getText());
						}

					}

					@Override
					public void keyReleased(final KeyEvent arg0) {

					}

					@Override
					public void keyTyped(final KeyEvent arg0) {

					}
				});
				return;
			}
		}
	}

	private void enterOnTextField(final KeyEvent event, final String text) {

		if (text.isEmpty()) {
			m_fileChooser.setFileFilter(m_fileChooser.getAcceptAllFileFilter());
			return;
		}

		final File[] dirFiles = m_fileChooser.getCurrentDirectory().listFiles();
		final ArrayList<String> textFiles = new ArrayList<String>();
		final ArrayList<File> selectedFiles = new ArrayList<File>();

		// split at " and ' ' most probably a list of files
		final StringTokenizer tk = new StringTokenizer(text, "\"");

		while (tk.hasMoreTokens()) {
			String next = tk.nextToken();
			next = next.trim();
			if (next.length() > 0) {
				textFiles.add(next);
			}
		}

		// search
		boolean foundAllFiles = true;

		// maybe optimize this
		for (final String candidate : textFiles) {
			boolean match = false;
			for (final File file : dirFiles) {
				if (file.getName().equals(candidate)) {
					match = true;
					selectedFiles.add(file);
					break;
				}
			}

			if (!match) {
				foundAllFiles = false;
				break;
			}
		}

		if (foundAllFiles) {
			// ok it was a filelist add them
			m_fileChooser
					.setSelectedFiles(selectedFiles.toArray(new File[] {}));
			onAdd();
		} else {
			// make a new file filter
			final FileFilter ff = new FileFilter() {

				String filterString = WildcardMatcher.wildcardToRegex(text);

				@Override
				public boolean accept(final File f) {
					return f.getName().matches(filterString);
				}

				@Override
				public String getDescription() {
					return text;
				}
			};

			FileFilter containedFilter = null;

			for (final FileFilter filter : m_fileChooser
					.getChoosableFileFilters()) {

				if (filter.getDescription().equals(ff.getDescription())) {
					containedFilter = filter;
					break;
				}
			}

			if (containedFilter == null) {
				m_fileChooser.setFileFilter(ff);
			} else {
				m_fileChooser.setFileFilter(containedFilter);
			}
		}

	}

	private void fireSelectionChangedEvent() {
		if (m_listeners != null) {
			for (final ChangeListener listener : m_listeners) {
				listener.stateChanged(new ChangeEvent(this));
			}
		}
		m_selectedFileListBorder.setTitle("Selected files ("
				+ m_selectedFileListModel.getSize() + ")");
		repaint();

		if (m_eventService != null) {
			m_eventService.publish(new FileChooserSelectedFilesChgEvent(
					getSelectedFiles()));
		}

	}

	public FileFilter getFileFilter() {
		return m_fileFilter;
	}

	/**
	 * The list of the selected files
	 * 
	 * @return files
	 * 
	 */
	public String[] getSelectedFiles() {
		final String[] values = new String[m_selectedFileListModel.getSize()];
		for (int i = 0; i < values.length; i++) {
			values[i] = m_selectedFileListModel.getAbsolutePathAt(i);
		}
		return values;
	}

	public void onAdd() {
		final FileFilter ff = m_fileChooser.getFileFilter();
		if (m_fileChooser.getSelectedFiles().length == 0) {

			JOptionPane
					.showMessageDialog(
							this,
							"No files selected. Please select at least one file or directory.",
							"Warning", JOptionPane.ERROR_MESSAGE);

			return;
		}

		m_selectedFileListModel.addFiles(m_fileChooser.getSelectedFiles(), ff);

		m_filePref.put("Path", m_fileChooser.getCurrentDirectory().toString());
		fireSelectionChangedEvent();

	}

	private void onAddAllTopLevelFiles() {
		final File[] files = m_fileChooser.getCurrentDirectory().listFiles();
		final ArrayList<File> topLevelFiles = new ArrayList<File>();

		// remove directories
		for (final File f : files) {
			if (!f.isDirectory() && m_fileChooser.getFileFilter().accept(f)) {
				topLevelFiles.add(f);
			}
		}

		m_selectedFileListModel.addFiles(topLevelFiles.toArray(new File[] {}),
				m_fileFilter);
		m_filePref.put("Path", m_fileChooser.getCurrentDirectory().toString());
		fireSelectionChangedEvent();

	}

	/*
	 * when the remove button was pressed
	 */
	private void onRemove() {
		if (m_selectedFileListModel.getSize() == 0) {

			JOptionPane.showMessageDialog(this, "No files in list to Remove",
					"Error", JOptionPane.ERROR_MESSAGE);

			return;
		}
		m_selectedFileListModel.remove(m_selectedFileList.getSelectedIndices());
		m_selectedFileList.clearSelection();
		fireSelectionChangedEvent();
	}

	/*
	 * when the remove all button was pressed
	 */
	private void onRemoveAll() {
		m_selectedFileListModel.removeAll();
		fireSelectionChangedEvent();
	}

	/**
	 * Updates the selected files list after removing or adding files
	 * 
	 * @param selectedFiles
	 */

	public void update(final String[] selectedFiles) {
		// applying the model settings to the components
		if (selectedFiles.length > 0) {
			final File[] files = new File[selectedFiles.length];
			for (int i = 0; i < selectedFiles.length; i++) {
				files[i] = new File(selectedFiles[i]);
			}
			m_selectedFileListModel.removeAll();
			m_selectedFileListModel.addFiles(files, m_fileFilter);

		}

		if (m_eventService != null) {
			m_eventService.publish(new FileChooserSelectedFilesChgEvent(
					getSelectedFiles()));
		}
	}

}
