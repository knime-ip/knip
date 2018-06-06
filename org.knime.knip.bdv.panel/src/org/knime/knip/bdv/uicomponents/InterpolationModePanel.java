package org.knime.knip.bdv.uicomponents;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.knime.knip.bdv.events.InterpolationModeChangeEvent;
import org.scijava.event.EventService;

import bdv.viewer.Interpolation;
import bdv.viewer.InterpolationModeListener;
import bdv.viewer.ViewerPanel;
import net.miginfocom.swing.MigLayout;

/**
 * 
 * This panel holds the {@link Interpolation} mode selection.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class InterpolationModePanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/**
	 * Showing the different {@link Interpolation} modes.
	 * 
	 * Note: This dialog component listens to changes comming from the {@link ViewerPanel}.
	 * 
	 * @param es the event-service
	 * @param viewer the {@link ViewerPanel}
	 */
	public InterpolationModePanel(final EventService es, final ViewerPanel viewer) {
		JComboBox<String> interpolationModes = new JComboBox<>();
		for (Interpolation i : Interpolation.values()) {
			interpolationModes.addItem(i.toString());
		}
		interpolationModes.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ev) {
				if (ev.getSource() == interpolationModes) {
					es.publish(new InterpolationModeChangeEvent(
							Interpolation.valueOf((String) interpolationModes.getSelectedItem())));
				}
			}
		});
		
		this.setBackground(Color.WHITE);
		this.setBorder(new TitledBorder("Interpolation Mode"));
		this.setLayout(new MigLayout("fillx", "[grow]", ""));
		this.add(interpolationModes, "growx");
		
		viewer.addInterpolationModeListener(new InterpolationModeListener() {
			
			@Override
			public void interpolationModeChanged(Interpolation mode) {
				interpolationModes.setSelectedItem(mode.toString());
			}
		});
		
		// Default-Value
		interpolationModes.setSelectedItem(Interpolation.NEARESTNEIGHBOR.toString());
	}
}
