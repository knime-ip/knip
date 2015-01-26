package org.knime.knip.featurenode;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.knime.core.node.KNIMEConstants;
import org.knime.knip.featurenode.ui.FeatureSetInputPanel;
import org.knime.workbench.repository.util.NodeFactoryHTMLCreator;

public class FeatureSetInfoDialog extends JDialog {
	
	private FeatureSetInputPanel m_feature;
	private Frame m_parent;

	public FeatureSetInfoDialog(final Frame parent, final FeatureSetInputPanel feature) {
		super(parent, false);
		m_parent = parent;
		m_feature = feature;
		setPreferredSize(new Dimension(350, m_parent.getHeight()));
		
		// create a JEditorPane
		JEditorPane jEditorPane = new JEditorPane();

		// make it read-only
		jEditorPane.setEditable(false);

		// add a HTMLEditorKit to the editor pane
		HTMLEditorKit kit = new HTMLEditorKit();
		StyleSheet css = new StyleSheet();
		css.addRule(NodeFactoryHTMLCreator.instance.getCss());
		kit.setStyleSheet(css);
		jEditorPane.setEditorKit(kit);
		
		jEditorPane.setText(createHTML());

		// now add it to a scroll pane
		JScrollPane scrollPane = new JScrollPane(jEditorPane);
		
		add(scrollPane);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	private String createHTML() {
		
		
		String htmlStart = 
				"<html>\n"
				/** {@link NodeFactoryHTMLCreator} css has blue background color?!  */
				+ "<body style=\"background-color:white\">\n"
				+ "<div id=\"group-description\">\n";
		
		String title =
				"<h1>" + m_feature.getPluginInfo().getTitle() + "</h1>\n";
		
		String description = 
				"<h2>Description</h2>\n"
				+ "<p>" + 
				//m_feature.getPluginInfo().getDescription() 
						"Lorem ipsum dolor sit amet, "
						+ "consetetur sadipscing elitr, "
						+ "sed diam nonumy eirmod tempor "
						+ "invidunt ut labore et dolore "
						+ "magna aliquyam erat, sed diam "
						+ "voluptua. At vero eos et accusam "
						+ "et justo duo dolores et ea rebum. "
						+ "Stet clita kasd gubergren, no sea "
						+ "takimata sanctus est Lorem ipsum dolor "
						+ "sit amet. Lorem ipsum dolor sit amet, "
						+ "consetetur sadipscing elitr, sed diam "
						+ "nonumy eirmod tempor invidunt ut labore "
						+ "et dolore magna aliquyam erat, sed diam "
						+ "voluptua. At vero eos et accusam et justo "
						+ "duo dolores et ea rebum. Stet clita kasd "
						+ "gubergren, no sea takimata sanctus est "
						+ "Lorem ipsum dolor sit amet."
				+ "</p>";
		
		String htmlEnd = 
				"</div>\n"
				+ "</body>\n"
				+ "</html>";
		
		return htmlStart + title + description + htmlEnd;
	}
	
    public static void openUserDialog(final Frame parent, FeatureSetInputPanel feature) {
            FeatureSetInfoDialog infoDialog = new FeatureSetInfoDialog(parent, feature);
            infoDialog.showDialog();
    }
    
    private void showDialog() {
    	setTitle(m_feature.getPluginInfo().getTitle() + " information dialog");
    	pack();
    	positionDialog();
    	setVisible(true);
    }


	private void positionDialog() {
		
		int x = m_parent.getLocationOnScreen().x;
		int y = m_parent.getLocationOnScreen().y;
		Point loc = new Point((int) (x + m_parent.getSize().getWidth() + 20), (int) (y ));
		setLocation(loc);
		
	}
}
