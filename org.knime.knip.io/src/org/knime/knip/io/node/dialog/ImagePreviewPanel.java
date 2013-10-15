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
package org.knime.knip.io.node.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.gui.BufferedImageReader;

/**
 * A JPanel to provide a preview of the image itself or the meta data.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class ImagePreviewPanel extends JPanel implements Runnable {

	/**
     *
     */
	private static final long serialVersionUID = 1L;

	private final BufferedImageReader m_reader;

	private JLabel m_iconLabel;

	private String m_openedFile = null;

	private String[][] m_metadata;

	private JTable m_metadataTable;

	private final JPanel m_image = new JPanel();

	private final JPanel m_table = new JPanel();

	private static final String[] CORE_METADATA = new String[] { "SizeX",
			"SizeY", "SizeZ", "SizeT", "SizeC", "IsRGB", "PixelType",
			"LittleEndian", "DimensionsOrder", "IsInterleaved" };

	/**
     *
     */
	public ImagePreviewPanel() {
		super();
		m_table.setLayout(new BoxLayout(m_table, BoxLayout.Y_AXIS));
		m_reader = new BufferedImageReader(new ImageReader());
		setPreferredSize(new Dimension(300, 250));
		setFont(new Font(getFont().getName(), Font.PLAIN, 20));

		m_iconLabel = new JLabel();
		m_iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		m_image.add("Image", m_iconLabel);
		m_metadata = new String[][] { { "", "" } };
		m_metadataTable = new JTable(new AbstractTableModel() {
			/**
             *
             */
			private static final long serialVersionUID = 1L;

			@Override
			public int getColumnCount() {
				return m_metadata[0].length;
			}

			@Override
			public int getRowCount() {

				return m_metadata != null ? m_metadata.length : 0;
			}

			@Override
			public Object getValueAt(final int rowIndex, final int columnIndex) {
				return m_metadata[rowIndex][columnIndex];
			}

			@Override
			public String getColumnName(final int index) {
				if (index == 0) {
					return "key";
				}

				return "value";
			}
		});
		final JScrollPane sp = new JScrollPane(m_metadataTable);
		sp.setPreferredSize(new Dimension(300, 100));
		m_table.add("Metadata", sp);
		add(m_image);
		add(m_table);

	}

	/**
	 * Sets the preview to the specified file. This methods opens the file and
	 * updates the preview panel.
	 * 
	 * @param filename
	 */
	public void setImage(final String filename) {

		if (filename.equals(m_openedFile)) {
			return;
		}
		m_openedFile = filename;
		final Thread loader = new Thread(this);
		loader.start();

	}

	/**
	 * Thumbnail loading routine.
	 */
	@Override
	public void run() {
		try { // catch-all for unanticipated exceptions
			try {

				m_reader.setId(m_openedFile);
				final ImageIcon ico = new ImageIcon();

				// get image Size
				final int x = m_reader.getSizeX();
				final int y = m_reader.getSizeY();
				// Calculate the Size of the Preview
				final int scx = (int) (x * (290.0 / new Double(x)));
				final int scy = (int) (y * (240 / new Double(y)));
				// Workaround for bigger ImageIcon
				ico.setImage(m_reader.openThumbImage(0).getScaledInstance(scx,
						scy, Image.SCALE_DEFAULT));

				m_iconLabel.setIcon(ico);
				m_metadata = new String[][] { { "", "" } };
				final Hashtable<String, Object> gm = m_reader
						.getGlobalMetadata();
				m_metadata = new String[CORE_METADATA.length + gm.size()][2];
				for (int i = 0; i < CORE_METADATA.length; i++) {
					m_metadata[i][0] = CORE_METADATA[i];
				}

				m_metadata[0][1] = "" + x;
				m_metadata[1][1] = "" + y;
				m_metadata[2][1] = "" + m_reader.getSizeZ();
				m_metadata[3][1] = "" + m_reader.getSizeT();
				m_metadata[4][1] = "" + m_reader.getEffectiveSizeC();
				m_metadata[5][1] = "" + m_reader.isRGB();
				m_metadata[6][1] = "" + m_reader.getPixelType();
				m_metadata[7][1] = "" + m_reader.isLittleEndian();
				m_metadata[8][1] = "" + m_reader.getDimensionOrder();
				m_metadata[9][1] = "" + m_reader.isInterleaved();

				final Set<String> keys = gm.keySet();
				int i = 0;
				for (final Object o : keys) {
					m_metadata[(CORE_METADATA.length + (i++)) - 1][0] = o
							.toString();
				}
				final Collection<Object> values = gm.values();
				i = 0;
				for (final Object o : values) {
					m_metadata[(CORE_METADATA.length + (i++)) - 1][1] = o
							.toString();
				}

				m_metadataTable.tableChanged(new TableModelEvent(
						m_metadataTable.getModel()));

			} catch (final FormatException e) {
				m_iconLabel.setIcon(new ImageIcon(
						makeImage("unsupported format")));

			} catch (final IOException e) {
				m_iconLabel.setIcon(new ImageIcon(makeImage("failed")));
				m_metadata = null;
			}

		} catch (final Exception exc) {
			m_iconLabel.setIcon(new ImageIcon(makeImage("error")));
		}

	}

	/**
	 * Creates a blank image with the given message painted on top (e.g., a
	 * loading or error message), matching the size of the active reader's
	 * thumbnails.
	 */
	private BufferedImage makeImage(final String message) {
		int w = getSize().width, h = getSize().height;
		if (w < 128) {
			w = 128;
		}
		if (h < 32) {
			h = 32;
		}
		final BufferedImage image = new BufferedImage(w, h,
				BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		final Rectangle2D.Float r = (Rectangle2D.Float) g.getFont()
				.getStringBounds(message, g.getFontRenderContext());
		g.drawString(message, (w - r.width) / 2, ((h - r.height) / 2)
				+ r.height);
		g.dispose();
		return image;
	}

}
