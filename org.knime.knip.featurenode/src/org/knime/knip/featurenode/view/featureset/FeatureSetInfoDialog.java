package org.knime.knip.featurenode.view.featureset;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.knime.workbench.repository.util.NodeFactoryHTMLCreator;

public class FeatureSetInfoDialog extends JDialog {

	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = 6643531687004421062L;

	private final Frame m_parent;
	private final String m_title;
	private final String m_description;

	public FeatureSetInfoDialog(final Frame parent, final String title,
			final String description) {
		super(parent, false);
		this.m_parent = parent;
		this.m_title = title;
		this.m_description = description;

		setPreferredSize(new Dimension(350, this.m_parent.getHeight()));

		// create a JEditorPane
		final JEditorPane jEditorPane = new JEditorPane();

		// make it read-only
		jEditorPane.setEditable(false);

		// add a HTMLEditorKit to the editor pane
		final HTMLEditorKit kit = new HTMLEditorKit();
		final StyleSheet css = new StyleSheet();
		css.addRule(NodeFactoryHTMLCreator.instance.getCss());
		kit.setStyleSheet(css);
		jEditorPane.setEditorKit(kit);

		jEditorPane.setText(createHTML());

		// now add it to a scroll pane
		final JScrollPane scrollPane = new JScrollPane(jEditorPane);

		add(scrollPane);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	private String createHTML() {
		final String htmlStart = "<html>\n"
		/** {@link NodeFactoryHTMLCreator} css has blue background color?! */
		+ "<body style=\"background-color:white\">\n"
				+ "<div id=\"group-description\">\n";

		final String title = "<h1>" + this.m_title + "</h1>\n";

		final String description = "<h2>Description</h2>\n" + "<p>"
				+ this.m_description + "</p>";

		final String htmlEnd = "</div>\n" + "</body>\n" + "</html>";

		return htmlStart + title + description + htmlEnd;
	}

	public static void openUserDialog(final Frame parent, final String title,
			final String description) {
		final FeatureSetInfoDialog infoDialog = new FeatureSetInfoDialog(
				parent, title, description);
		infoDialog.showDialog();
	}

	private void showDialog() {
		setTitle(this.m_title + " information dialog");
		pack();
		positionDialog();
		setVisible(true);
	}

	private void positionDialog() {
		final int x = this.m_parent.getLocationOnScreen().x;
		final int y = this.m_parent.getLocationOnScreen().y;
		final Point loc = new Point((int) (x
				+ this.m_parent.getSize().getWidth() + 20), (y));
		setLocation(loc);
	}
}
