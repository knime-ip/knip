package org.knime.knip.cellviewer.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;

public class CellViewerHelpDialog extends JDialog implements ActionListener {

	public CellViewerHelpDialog() {
		setLayout(new BorderLayout());

		JEditorPane ep = new JEditorPane();
		ep.setContentType("text/html");
		ep.setEditable(false);
		ep.setText("<html>" + "<head></head>" + "<body style='font-family=\"sans-serif\"'>"
				+ "<h1> About the Cell Viewer </h1>"
				+ "The cell viewer provides the user with an interface to browse tables and view their content."
				+ "<br>"
				+ " In order to navigate through the table, you can either click on any image in the tabular overview"
				+ " directly, or use the buttons at the bottom of the window to move step-by-step through the table.<br>"
				+ "Furthermore, it is possible to select multiple images simultaneously if there are any viewers registered that can handle multiple "
				+ "images." + " <br><br>" + "<h1>Useful Keyboard Shortcuts</h1>"
				+ "<i> In order to facilitate navigation through the table, the viewer provides hotkeys that control the selection. </i>"
				+ "<br>" + "<table style=\"width:100%\">" + "<tr>" + "<td>Move selection up</td>" + "<td>[ w ]</td>"
				+ "</tr>" + "<tr>" + "<td>Move selection down</td>" + "<td>[ s ]</td>" + "</tr>" + "<tr>"
				+ "<td>Move selection left</td>" + "<td>[ a ]</td>" + "</tr>" + "<tr>" + "<td>Move selection right</td>"
				+ "<td>[ d ]</td>" + "</tr>" + "</table>" + "</body>" + "</html>");

		add(ep, BorderLayout.CENTER);
		JButton close = new JButton("OK");
		ep.setFont(close.getFont());
		close.addActionListener(this);
		add(close, BorderLayout.SOUTH);
		setPreferredSize(new Dimension(400, 500));
		validate();
		setVisible(true);

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		setVisible(false);
		dispose();

	}
}
