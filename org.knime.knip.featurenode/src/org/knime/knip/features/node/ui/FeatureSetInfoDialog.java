/*
 * ------------------------------------------------------------------------
 *
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
  ---------------------------------------------------------------------
 *
 */
package org.knime.knip.features.node.ui;

import java.awt.Dimension;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.text.html.HTMLEditorKit;

public class FeatureSetInfoDialog extends JFrame {

	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = 6643531687004421062L;

	private final String m_description;

	public FeatureSetInfoDialog(final String title, final String description) {
		this.m_description = description;

		setPreferredSize(new Dimension(350, 600));

		// create a JEditorPane
		final JEditorPane jEditorPane = new JEditorPane();

		// make it read-only
		jEditorPane.setEditable(false);

		// add a HTMLEditorKit to the editor pane
		final HTMLEditorKit kit = new HTMLEditorKit();
		// final StyleSheet css = new StyleSheet();
		// css.addRule(NodeFactoryHTMLCreator.instance.getCss());
		// kit.setStyleSheet(css);
		jEditorPane.setEditorKit(kit);

		jEditorPane.setText(createHTML());

		// now add it to a scroll pane
		final JScrollPane scrollPane = new JScrollPane(jEditorPane);

		add(scrollPane);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	private String createHTML() {
		// final String htmlStart = "<html>\n"
		// /**
		// * {@link NodeFactoryHTMLCreator} css has blue background
		// * color?!
		// */
		// + "<body style=\"background-color:white\">\n" + "<div
		// id=\"group-description\">\n";
		//
		// final String title = "<h1>" + this.m_title + "</h1>\n";
		//
		// final String description = "<h2>Description</h2>\n" + "<p>" +
		// this.m_description + "</p>";
		//
		// final String htmlEnd = "</div>\n" + "</body>\n" + "</html>";

		return new String("<html> <body> " + this.m_description + " </body> </html>");

		// return htmlStart + title + description + htmlEnd;
	}

	public static void openUserDialog(final String title, final String description) {
		final FeatureSetInfoDialog infoDialog = new FeatureSetInfoDialog(title, description);
		infoDialog.setTitle(title + " information dialog");
		infoDialog.pack();
		infoDialog.setLocationRelativeTo(null);
		infoDialog.setVisible(true);
	}
}
