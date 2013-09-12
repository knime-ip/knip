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
package org.knime.knip.io.view;

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   8 Feb 2010 (hornm): created
 */

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;

import loci.formats.gui.XMLCellRenderer;

import org.knime.core.data.DataValue;
import org.knime.core.data.xml.XMLValue;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.knip.base.nodes.view.TableCellView;
import org.knime.knip.base.nodes.view.TableCellViewFactory;
import org.w3c.dom.Document;

/**
 * Displays a XML-String in a tree.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class XMLCellViewFactory implements TableCellViewFactory {

	@Override
	public TableCellView[] createTableCellViews() {
		return new TableCellView[] { new TableCellView() {

			// private Document m_doc;
			private JPanel m_panel;

			{
				m_panel = new JPanel(new BorderLayout());
			}

			// -- XMLPanel methods --

			// /** Displays XML from the given string. */
			// public void setXML(final String xml)
			// throws ParserConfigurationException, SAXException,
			// IOException {
			//
			// setDocument(null);
			// // parse XML from string into DOM structure
			// DocumentBuilderFactory docFact =
			// DocumentBuilderFactory
			// .newInstance();
			// DocumentBuilder db = docFact.newDocumentBuilder();
			// ByteArrayInputStream is = new ByteArrayInputStream(
			// xml.getBytes());
			// Document doc = db.parse(is);
			// is.close();
			//
			// setDocument(doc);
			// }

			/**
			 * {@inheritDoc}
			 */
			@Override
			public String getDescription() {
				return "XML tree";
			}

			// /**
			// * Gets the XML document currently being displayed
			// within the
			// * window.
			// */
			// public Document getDocument() {
			// return m_doc;
			// }

			@Override
			public String getName() {
				return "XML";
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public Component getViewComponent() {
				return m_panel;
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void loadConfigurationFrom(final ConfigRO config) {
				//

			}

			@Override
			public void onClose() {
				//
			}

			@Override
			public void onReset() {

			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void saveConfigurationTo(final ConfigWO config) {
				//

			}

			/** Displays XML from the given document. */
			public void setDocument(final Document doc) {
				// m_doc = doc;
				m_panel.removeAll();
				// populate metadata window and size
				// intelligently
				final JTree tree = XMLCellRenderer.makeJTree(doc);
				for (int i = 0; i < tree.getRowCount(); i++) {
					tree.expandRow(i);
				}

				m_panel.add(new JScrollPane(tree), BorderLayout.CENTER);
				// JFrame test = new JFrame("test");
				// test.add(tree);
				// test.pack();
				// test.setVisible(true);
				// setVisible(true);

			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void updateComponent(final DataValue valueToView) {
				setDocument(((XMLValue) valueToView).getDocument());
				m_panel.repaint();
				return;

			}
		} };
	}

	@Override
	public Class<? extends DataValue> getDataValueClass() {
		return XMLValue.class;
	}

}
