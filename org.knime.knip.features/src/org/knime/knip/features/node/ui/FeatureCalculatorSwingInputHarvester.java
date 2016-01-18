package org.knime.knip.features.node.ui;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.scijava.module.Module;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Plugin;
import org.scijava.ui.AbstractInputHarvesterPlugin;
import org.scijava.ui.swing.SwingDialog;
import org.scijava.ui.swing.SwingUI;
import org.scijava.widget.InputHarvester;
import org.scijava.widget.InputPanel;

@Plugin(type = PreprocessorPlugin.class, priority = InputHarvester.PRIORITY)
public class FeatureCalculatorSwingInputHarvester extends AbstractInputHarvesterPlugin<JPanel, JPanel> {

	@Override
	public FeatureCalculatorSwingInputPanel createInputPanel() {
		return new FeatureCalculatorSwingInputPanel();
	}

	@Override
	public boolean harvestInputs(final InputPanel<JPanel, JPanel> inputPanel, final Module module) {
		final JPanel pane = inputPanel.getComponent();

		// display input panel in a dialog
		final String title = module.getInfo().getTitle();
		final boolean modal = !module.getInfo().isInteractive();
		final boolean allowCancel = module.getInfo().canCancel();
		final int optionType, messageType;
		if (allowCancel)
			optionType = JOptionPane.OK_CANCEL_OPTION;
		else
			optionType = JOptionPane.DEFAULT_OPTION;
		if (inputPanel.isMessageOnly()) {
			if (allowCancel)
				messageType = JOptionPane.QUESTION_MESSAGE;
			else
				messageType = JOptionPane.INFORMATION_MESSAGE;
		} else
			messageType = JOptionPane.PLAIN_MESSAGE;
		final boolean doScrollBars = messageType == JOptionPane.PLAIN_MESSAGE;
		final SwingDialog dialog = new SwingDialog(pane, optionType, messageType, doScrollBars);
		dialog.setTitle(title);
		dialog.setModal(modal);
		final int rval = dialog.show();

		// verify return value of dialog
		return rval == JOptionPane.OK_OPTION;
	}

	// -- Internal methods --

	@Override
	protected String getUI() {
		return SwingUI.NAME;
	}

}
