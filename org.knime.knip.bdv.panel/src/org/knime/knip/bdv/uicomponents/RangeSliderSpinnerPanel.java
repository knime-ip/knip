package org.knime.knip.bdv.uicomponents;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.knip.bdv.control.BDVHandlePanel;
import org.knime.knip.bdv.events.AddSourceEvent;
import org.knime.knip.bdv.events.DisplayRangeChangedEvent;
import org.knime.knip.bdv.events.RemoveSourceEvent;
import org.knime.knip.bdv.events.SourceSelectionChangeEvent;
import org.knime.knip.bdv.uicomponents.rangeslider.RangeSlider;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;

import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.VisibilityAndGrouping;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.miginfocom.swing.MigLayout;

/**
 * 
 * A panel holding a two-knob range slider with a lower- and upper-value
 * spinner.
 * 
 * The bounds can be dynamically changed by either entering smaller/larger
 * values into the spinner or resizing the range-slider to the current positions
 * with a resize-button.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class RangeSliderSpinnerPanel<I extends IntegerType<I>, T extends NumericType<T>, L> extends JPanel {

	private static final long serialVersionUID = 1L;

	/**
	 * Upper bound of the range slider.
	 */
	private static final int RS_UPPER_BOUND = 1000;

	/**
	 * Setup assignments of the viewer.
	 */
	private final SetupAssignments setupAssignments;

	/**
	 * Visibility and Grouping of the viewer.
	 */
	private final VisibilityAndGrouping visibilityAndGrouping;

	/**
	 * The range slider.
	 */
	private final RangeSlider rs;

	/**
	 * Block component updates.
	 */
	private boolean block;

	/**
	 * Range slider number of steps.
	 */
	final double numberOfSteps = 1001.0;

	/**
	 * Display range upper bound.
	 */
	private double upperBound = 1;

	/**
	 * Display range lower bound.
	 */
	private double lowerBound = 0;

	/**
	 * Display range upper value. The currently selected upper value.
	 */
	private double upperValue = 1;

	/**
	 * Display range lower value. The currently selected lower value.
	 */
	private double lowerValue = 0;

	/**
	 * Store the lower bound for every source.
	 */
	private final HashMap<Integer, Double> lowerBoundLookup = new HashMap<>();

	/**
	 * Store the upper bound for every source.
	 */
	private final HashMap<Integer, Double> upperBoundLookup = new HashMap<>();

	/**
	 * The minimum spinner.
	 */
	private final JSpinner currentMinSpinner;

	/**
	 * The maximum spinner.
	 */
	private final JSpinner currentMaxSpinner;

	/**
	 * Index of the currently selected source.
	 */
	private int currentSourceIdx;

	private EventService es;

	private boolean isLabeling;

	private HashMap<Integer, Pair<Double, Double>> labelingMinMax;

	/**
	 * A range slider panel with two knobs and min/max spinners.
	 * 
	 * @param es
	 *            the event-service
	 * @param bdvHandlePanel
	 */
	public RangeSliderSpinnerPanel(final EventService es, final BDVHandlePanel<I, T, L> bdvHandlePanel,
			final Map<String, MetaSourceProperties<T>> sourceLookup) {
		this.es = es;
		this.labelingMinMax = new HashMap<>();
		es.subscribe(this);
		setupPanel();

		setupAssignments = bdvHandlePanel.getBdvHandle().getSetupAssignments();
		visibilityAndGrouping = bdvHandlePanel.getBdvHandle().getViewerPanel().getVisibilityAndGrouping();

		currentMinSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1.0, 1.0));
		setupMinSpinner();

		currentMaxSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 1.0, 1.0));
		setupMaxSpinner();

		rs = new RangeSlider(0, RS_UPPER_BOUND);
		setupRangeSlider();

		final JButton shrinkRange = new JButton("><");
		setupShrinkRangeButton(shrinkRange);

		this.add(currentMinSpinner);
		this.add(rs, "growx");
		this.add(currentMaxSpinner);
		this.add(shrinkRange);
	}

	private void setupPanel() {
		this.setLayout(new MigLayout("fillx, hidemode 3", "[][grow][][]", ""));
		this.setBorder(new TitledBorder(new LineBorder(Color.lightGray), "Display Range"));
		this.setBackground(Color.WHITE);
	}

	private void setupShrinkRangeButton(final JButton shrinkRange) {
		shrinkRange.setBackground(Color.white);
		shrinkRange.setForeground(Color.darkGray);
		shrinkRange.setBorder(null);
		shrinkRange.setMargin(new Insets(0, 2, 0, 2));
		shrinkRange.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == shrinkRange) {
					upperBound = posToUpperValue(rs.getUpperValue());
					lowerBound = posToLowerValue(rs.getValue());
					upperValue = upperBound;
					lowerValue = lowerBound;
					upperBoundLookup.put(currentSourceIdx, upperValue);
					lowerBoundLookup.put(currentSourceIdx, lowerValue);
					((SpinnerNumberModel) currentMaxSpinner.getModel()).setMaximum(upperBound);
					((SpinnerNumberModel) currentMinSpinner.getModel()).setMinimum(lowerBound);
					rs.setValue(0);
					rs.setUpperValue(RS_UPPER_BOUND);
					setDisplayRange(lowerBound, upperBound);
				}
			}
		});
	}

	private void setupRangeSlider() {
		rs.setBackground(Color.WHITE);
		rs.setPreferredSize(new Dimension(50, rs.getPreferredSize().height));
		rs.setValue(0);
		rs.setUpperValue(RS_UPPER_BOUND);
		rs.setMinorTickSpacing(1);
		rs.setSnapToTicks(true);
		rs.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if (!block) {
					upperValue = posToUpperValue(rs.getUpperValue());
					currentMaxSpinner.setValue(upperValue);
					lowerValue = posToLowerValue(rs.getValue());
					currentMinSpinner.setValue(lowerValue);
				}
			}
		});
	}

	private void setupMaxSpinner() {
		currentMaxSpinner.setPreferredSize(new Dimension(65, currentMaxSpinner.getPreferredSize().height));
		currentMaxSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if (e.getSource() == currentMaxSpinner && !block) {
					block = true;
					upperValue = (double) ((SpinnerNumberModel) currentMaxSpinner.getModel()).getValue();
					setDisplayRange(lowerValue, upperValue);
					setRangeSlider();
					block = false;
				}
			}
		});
		currentMaxSpinner.setEditor(new UpperBoundNumberEditor(currentMaxSpinner));
	}

	private void setupMinSpinner() {
		currentMinSpinner.setPreferredSize(new Dimension(65, currentMinSpinner.getPreferredSize().height));
		currentMinSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if (e.getSource() == currentMinSpinner && !block) {
					block = true;
					lowerValue = (double) ((SpinnerNumberModel) currentMinSpinner.getModel()).getValue();
					setDisplayRange(lowerValue, upperValue);
					setRangeSlider();
					block = false;
				}
			}
		});
		currentMinSpinner.setEditor(new LowerBoundNumberEditor(currentMinSpinner));
	}

	class UpperBoundNumberEditor extends JSpinner.NumberEditor implements KeyListener {

		private static final long serialVersionUID = 1L;
		private JFormattedTextField textField;

		public UpperBoundNumberEditor(JSpinner spinner) {
			super(spinner);
			textField = getTextField();
			textField.addKeyListener(this);
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}

		@Override
		public void keyPressed(KeyEvent e) {
			final String text = textField.getText();
			if (!text.isEmpty()) {
				try {
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						double tmp = NumberFormat.getNumberInstance().parse(text).doubleValue();
						if (isLabeling) {
							tmp = Math.min(tmp, 255);
						}
						if (tmp > upperBound) {
							upperBound = tmp;
							upperBoundLookup.put(currentSourceIdx, upperBound);
							((SpinnerNumberModel) currentMaxSpinner.getModel()).setMaximum(upperBound);
							((SpinnerNumberModel) currentMinSpinner.getModel()).setMaximum(upperBound);
							setDisplayRange(lowerBound, upperBound);
							upperValue = upperBound;
							setRangeSlider();
						}
					}
				} catch (ParseException e1) {
					textField.setText(Double.toString(upperBound));
				}
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}
	}

	class LowerBoundNumberEditor extends JSpinner.NumberEditor implements KeyListener {

		private static final long serialVersionUID = 1L;
		private JFormattedTextField textField;

		public LowerBoundNumberEditor(JSpinner spinner) {
			super(spinner);
			textField = getTextField();
			textField.addKeyListener(this);
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}

		@Override
		public void keyPressed(KeyEvent e) {
			final String text = textField.getText();
			if (!text.isEmpty()) {
				try {
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						double tmp = NumberFormat.getNumberInstance().parse(text).doubleValue();
						if (isLabeling) {
							tmp = Math.max(tmp, 0);
						}
						if (tmp < lowerBound) {
							lowerBound = tmp;
							lowerBoundLookup.put(currentSourceIdx, lowerBound);
							((SpinnerNumberModel) currentMinSpinner.getModel()).setMinimum(lowerBound);
							((SpinnerNumberModel) currentMaxSpinner.getModel()).setMinimum(lowerBound);
							setDisplayRange(lowerBound, upperBound);
							lowerValue = lowerBound;
							setRangeSlider();
						}
					}
				} catch (ParseException e1) {
					textField.setText(Double.toString(lowerBound));
				}
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}
	}

	/**
	 * Set display range in setup-assignments.
	 * 
	 * @param min
	 * @param max
	 */
	private void setDisplayRange(double min, double max) {
		final int i = visibilityAndGrouping.getCurrentSource();
		if (isLabeling) {
			es.publish(new DisplayRangeChangedEvent(i, Math.max(0, min), Math.min(255, max)));
			labelingMinMax.put(i, new ValuePair<>(min, max));
		} else {
			setupAssignments.getConverterSetups().get(i).setDisplayRange(min, max);
		}
	}

	/**
	 * Convert range-slider position to upper-value.
	 * 
	 * @param pos
	 *            of range-slider
	 * @return value
	 */
	private double posToUpperValue(final int pos) {
		double frac = pos / 1000d;
		double val = Math.abs(upperBound - lowerBound) * frac + lowerBound;
		setDisplayRange(lowerValue, val);
		return val;
	}

	/**
	 * Convert range-slider position to lower-value.
	 * 
	 * @param pos
	 *            of range-slider
	 * @return value
	 */
	private double posToLowerValue(final int pos) {
		double frac = pos / 1000d;
		double val = Math.abs(upperBound - lowerBound) * frac + lowerBound;
		setDisplayRange(val, upperValue);
		return val;
	}

	@EventHandler
	public void selectionChanged(SourceSelectionChangeEvent<T> e) {
		if (e.getSource() != null) {
			getCurrentSourceIndex(e.getSource().getSourceName());
			isLabeling = e.getSource().isLabeling();
			if (lowerBoundLookup.containsKey(currentSourceIdx)) {
				block = true;
				lowerBound = lowerBoundLookup.get(currentSourceIdx);
				upperBound = upperBoundLookup.get(currentSourceIdx);
				if (isLabeling) {
					final Pair<Double, Double> p = labelingMinMax.get(currentSourceIdx);
					lowerValue = p.getA();
					upperValue = p.getB();
				} else {
					lowerValue = setupAssignments.getConverterSetups().get(currentSourceIdx).getDisplayRangeMin();
					upperValue = setupAssignments.getConverterSetups().get(currentSourceIdx).getDisplayRangeMax();
				}

				setRangeSlider();

				((SpinnerNumberModel) currentMinSpinner.getModel()).setMinimum(lowerBound);
				((SpinnerNumberModel) currentMaxSpinner.getModel()).setMinimum(lowerBound);
				((SpinnerNumberModel) currentMinSpinner.getModel()).setMaximum(upperBound);
				((SpinnerNumberModel) currentMaxSpinner.getModel()).setMaximum(upperBound);

				currentMinSpinner.setValue(lowerValue);
				currentMinSpinner.revalidate();
				currentMaxSpinner.setValue(upperValue);
				currentMaxSpinner.revalidate();

				rs.revalidate();
				rs.repaint();
				block = false;
			}
		}
	}

	/**
	 * Compute index of the currently selected source.
	 * 
	 * @param e
	 */
	private void getCurrentSourceIndex(final String name) {
		List<String> sourceNames = new ArrayList<>();
		visibilityAndGrouping.getSources().forEach(s -> sourceNames.add(s.getSpimSource().getName()));
		currentSourceIdx = sourceNames.indexOf(name);
	}

	@EventHandler
	public void addSource(final AddSourceEvent<T> e) {
		getCurrentSourceIndex(e.getSourceName());
		isLabeling = e.isLabeling();

		lowerBound = setupAssignments.getConverterSetups().get(currentSourceIdx).getDisplayRangeMin();
		lowerValue = lowerBound;
		upperBound = setupAssignments.getConverterSetups().get(currentSourceIdx).getDisplayRangeMax();
		upperValue = upperBound;

		block = true;
		currentMinSpinner.setValue(lowerValue);
		((SpinnerNumberModel) currentMinSpinner.getModel()).setMinimum(lowerBound);
		((SpinnerNumberModel) currentMaxSpinner.getModel()).setMinimum(lowerBound);
		lowerBoundLookup.put(currentSourceIdx, lowerBound);

		currentMaxSpinner.setValue(upperValue);
		((SpinnerNumberModel) currentMinSpinner.getModel()).setMaximum(upperBound);
		((SpinnerNumberModel) currentMaxSpinner.getModel()).setMaximum(upperBound);
		upperBoundLookup.put(currentSourceIdx, upperBound);

		if (isLabeling) {
			upperValue = 170.0;
			currentMaxSpinner.setValue(upperValue);
			labelingMinMax.put(currentSourceIdx, new ValuePair<>(lowerValue, upperValue));
		}

		setRangeSlider();
		block = false;
	}

	@EventHandler
	public void removeSource(final RemoveSourceEvent event) {
		final int sourceID = event.getSourceIndex();
		setupAssignments.removeSetup(setupAssignments.getConverterSetups().get(sourceID));
		lowerBoundLookup.remove(sourceID);
		upperBoundLookup.remove(sourceID);
		if (labelingMinMax.containsKey(sourceID)) {
			labelingMinMax.remove(sourceID);
		}
		HashMap<Integer, Pair<Double, Double>> tmp = new HashMap<>();
		Iterator<Integer> it = labelingMinMax.keySet().iterator();
		while (it.hasNext()) {
			final int idx = it.next();
			Pair<Double, Double> pair = labelingMinMax.get(idx);
			if (idx > sourceID) {
				tmp.put(idx - 1, pair);
				// System.out.println("sliderRemove" + (idx - 1));
			} else {
				tmp.put(idx, pair);
			}
		}
		labelingMinMax = tmp;

	}

	/**
	 * Set the knobs of the range-slider.
	 */
	private void setRangeSlider() {
		double range = upperBound - lowerBound;
		rs.setUpperValue((int) (((upperValue - lowerBound) / range) * numberOfSteps));
		rs.setValue((int) (((lowerValue - lowerBound) / range) * numberOfSteps));
	}
}
