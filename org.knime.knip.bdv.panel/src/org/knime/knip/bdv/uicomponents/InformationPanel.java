package org.knime.knip.bdv.uicomponents;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.knip.bdv.events.AddSourceEvent;
import org.knime.knip.bdv.events.SourceSelectionChangeEvent;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;

import net.imglib2.type.numeric.NumericType;
import net.miginfocom.swing.MigLayout;

/**
 * 
 * A panel displaying information about the currently selected source.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class InformationPanel<T extends NumericType<T>> extends JPanel {

	private static final long serialVersionUID = 1L;

	/**
	 * Display type information.
	 */
	private final JLabel type;

	/**
	 * Display dimensionality.
	 */
	private final JLabel dimensions;

	/**
	 * An information panel displaying information about the currently selected
	 * source.
	 * 
	 * @param es the event-service
	 */
	public InformationPanel(final EventService es) {

		es.subscribe(this);

		this.setLayout(new MigLayout("fillx", "[][grow]", ""));
		this.setBackground(Color.white);
		this.add(new JLabel("Type:"));

		type = new JLabel("type");
		type.setBackground(Color.WHITE);
		this.add(type, "growx, wrap");

		this.add(new JLabel("Dimensions:"));

		dimensions = new JLabel("dimensions");
		dimensions.setBackground(Color.WHITE);
		this.add(dimensions, "growx");
	}

	@EventHandler
	public void currentSelectionChanged(final SourceSelectionChangeEvent<T> e) {
		if (e.getSource() != null) {
			type.setText(e.getSource().getSourceType());
			dimensions.setText(e.getSource().getDims());
		}
	}
	
	@EventHandler
	public void addSourceEvent(final AddSourceEvent<T> e) {
		type.setText(e.getType());
		dimensions.setText(e.getDims());
	}
}