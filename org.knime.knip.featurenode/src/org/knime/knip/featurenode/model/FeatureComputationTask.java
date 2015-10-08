package org.knime.knip.featurenode.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.knime.knip.featurenode.OpsGateway;
import org.scijava.command.CommandInfo;
import org.scijava.module.Module;

import net.imagej.ops.featuresets.FeatureSet;
import net.imagej.ops.featuresets.NamedFeature;
import net.imagej.ops.slicewise.Hyperslice;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.Type;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

/**
 * This class compiles all given input {@link FeatureSet}s in the constructor
 * and computes the results of the {@link FeatureSet}s for the given
 * {@link FeatureTaskInput} in the {@link FeatureComputationTask#call()} method.
 *
 * @author Daniel Seebacher, University of Konstanz.
 *
 */
@SuppressWarnings({"unchecked"})
public class FeatureComputationTask<T extends Type<T>, L extends Comparable<L>>
		implements
			Callable<List<FeatureTaskOutput<T, L>>> {

	private final List<FeatureSet<IterableInterval<T>, T>> compiledFeatureSets;
	private final FeatureTaskInput<T, L> featureTaskInput;

	/**
	 * Default constructor.
	 *
	 * @param inputFeatureSets
	 *            A list of {@link FeatureSetInfo}s.
	 * @param featureTaskInput
	 *            A {@link FeatureTaskInput}
	 */
	public FeatureComputationTask(final List<FeatureSetInfo> inputFeatureSets,
			final FeatureTaskInput<T, L> featureTaskInput) {

		this.compiledFeatureSets = new ArrayList<FeatureSet<IterableInterval<T>, T>>();
		this.featureTaskInput = featureTaskInput;

		// for each feature set info
		for (final FeatureSetInfo fsi : inputFeatureSets) {

			// create a module of the feature set

			// TODO: hier
			
			Module module = OpsGateway.getModuleService().createModule(new CommandInfo(fsi.getFeatureSetClass()));


//			Module module = OpsGateway.getCommandService().getModuleService()
//					.createModule(new CommandInfo(fsi.getFeatureSetClass()));

			
			
			final Map<String, Object> fieldNameAndValues = fsi
					.getFieldNameAndValues();

			// set the parameters

			module.setInputs(fieldNameAndValues);

			// get the feature set
			FeatureSet<IterableInterval<T>, T> fs = (FeatureSet<IterableInterval<T>, T>) module
					.getDelegateObject();

			// if the feature set is an AbstractAutoResolvingFeatureSet check
			// which features need to be computed
			// if
			// (AbstractAutoResolvingFeatureSet.class.isAssignableFrom(fs.getClass()))
			// {
			// final AbstractAutoResolvingFeatureSet<IterableInterval<T>,
			// DoubleType> arfs =
			// (AbstractAutoResolvingFeatureSet<IterableInterval<T>,
			// DoubleType>) fs;
			//
			// // get all selected features
			// final Set<OpRef<?>> ops = new HashSet<OpRef<?>>();
			// for (final Entry<Class<?>, Boolean> entry :
			// fsi.getSelectedFeatures().entrySet()) {
			// if (!entry.getValue()) {
			// continue;
			// }
			//
			// ops.add(new OpRef(entry.getKey()));
			// }
			//
			// fs = new RestrictedFeatureSet<IterableInterval<T>,
			// T>(arfs.getClass(), ops, arfs.getHiddenOps());
			// }

			// initialize
			OpsGateway.getContext().inject(fs);
			fs.initialize();
			this.compiledFeatureSets.add(fs);
		}
	}

	@Override
	public List<FeatureTaskOutput<T, L>> call() throws Exception {

		final List<FeatureTaskOutput<T, L>> results = new ArrayList<FeatureTaskOutput<T, L>>();
		int i = 0;

		if (this.featureTaskInput.hasLabeling()) {

			// get input
			final RandomAccessibleInterval<LabelingType<L>> labeling = this.featureTaskInput
					.getLabelRegions();
			final Img<T> img = (this.featureTaskInput.hasImg())
					? this.featureTaskInput.getImage()
					: null;

			final Hyperslice hyperslice = new Hyperslice(
					OpsGateway.getOpService(), labeling,
					this.featureTaskInput.getSelectedDimensions(), false);
			final Cursor<RandomAccessibleInterval<?>> cursor = hyperslice
					.cursor();
			while (cursor.hasNext()) {
				final RandomAccessibleInterval<LabelingType<L>> asd = (RandomAccessibleInterval<LabelingType<L>>) cursor
						.next();

				labelLoop : for (final LabelRegion<L> label : new LabelRegions<L>(
						asd)) {

					// check if label is valid
					if (!this.featureTaskInput.getLabelSettings()
							.getRuleBasedLabelFilter()
							.isValid(label.getLabel())) {
						continue labelLoop;
					}

					// check if label is broken
					for (int k = 0; k < label.numDimensions(); k++) {
						if (label.max(k) < label.min(k)) {
							continue labelLoop;
						}
					}

					final List<Pair<String, T>> featureSetResults = new ArrayList<Pair<String, T>>();
					// get the results from each feature set
					for (final FeatureSet<IterableInterval<T>, T> fs : this.compiledFeatureSets) {

						// if we have a RestrictedFeatureSet
						// if (fs instanceof RestrictedFeatureSet) {
						// final RestrictedFeatureSet<IterableInterval<T>, T>
						// rfs = (RestrictedFeatureSet<IterableInterval<T>, T>)
						// fs;
						//
						// // if we have a geometric
						//// if
						// (Geometric2DFeatureSet.class.isAssignableFrom(rfs.getFeatureSetClass())
						//// ||
						// Geometric3DFeatureSet.class.isAssignableFrom(rfs.getFeatureSetClass()))
						// {
						//// featureSetResults.addAll(getGeometricFeatureSetResult(rfs,
						// label));
						//// } else {
						//// featureSetResults.addAll(getFeatureSetResults(fs,
						// img, label));
						//// }
						// }
						// // a normal FeatureSet
						// else {
						featureSetResults
								.addAll(getFeatureSetResults(fs, img, label));
						// }
					}

					results.add(
							new FeatureTaskOutput<T, L>(this.featureTaskInput,
									featureSetResults, img, label, i++));

				}
			}
		} else {

			// get input
			final Img<T> img = this.featureTaskInput.getImage();

			final Hyperslice hyperslice = new Hyperslice(
					OpsGateway.getOpService(), img,
					this.featureTaskInput.getSelectedDimensions());
			final Cursor<RandomAccessibleInterval<?>> cursor = hyperslice
					.cursor();
			while (cursor.hasNext()) {
				final RandomAccessibleInterval<T> asd = (RandomAccessibleInterval<T>) cursor
						.next();

				final List<Pair<String, T>> featureSetResults = new ArrayList<Pair<String, T>>();
				for (final FeatureSet<IterableInterval<T>, T> fs : this.compiledFeatureSets) {

					for (final Entry<NamedFeature, T> pair : fs
							.compute((IterableInterval<T>) asd).entrySet()) {
						featureSetResults.add(new ValuePair<String, T>(
								pair.getKey().getName(),
								pair.getValue().copy()));
					}
				}

				results.add(new FeatureTaskOutput<T, L>(this.featureTaskInput,
						featureSetResults, img, null, i++));

			}
		}

		return results;
	}

	// private List<Pair<String, T>> getGeometricFeatureSetResult(final
	// FeatureSet<IterableInterval<T>, T> rfs,
	// final LabelRegion<L> region) {
	// // build geometric feature set
	// final RestrictedFeatureSet<RandomAccessibleInterval<BoolType>, T> gfs =
	// new RestrictedFeatureSet<RandomAccessibleInterval<BoolType>, T>(
	// rfs.getFeatureSetClass(), rfs.getOutputOps(), rfs.getHiddenOps());
	// OpsGateway.getContext().inject(gfs);
	//
	// final List<Pair<String, T>> results = new ArrayList<Pair<String, T>>();
	// for (final Pair<String, T> pair :
	// gfs.getFeatureList(Regions.iterable(region))) {
	// results.add(new ValuePair<String, T>(pair.getA(), pair.getB().copy()));
	// }
	//
	// return results;
	// }

	private List<Pair<String, T>> getFeatureSetResults(
			final FeatureSet<IterableInterval<T>, T> fs, final Img<T> img,
			final LabelRegion<L> region) {

		final List<Pair<String, T>> results = new ArrayList<Pair<String, T>>();

		if (img != null) {
			HashMap<NamedFeature, T> r = (HashMap<NamedFeature, T>) fs
					.compute(Regions.sample(region, img));
			for (final Entry<NamedFeature, T> pair : r.entrySet()) {
				results.add(new ValuePair<String, T>(pair.getKey().getName(),
						pair.getValue().copy()));
			}
		} else {
			// HashMap<NamedFeature, T> r = (HashMap<NamedFeature, T>)
			// fs.compute(Regions.iterable(region));
			// for (final Entry<NamedFeature, T> pair : r.entrySet()) {
			// results.add(new ValuePair<String, T>(pair.getKey().getName(),
			// pair.getValue().copy()));
			// }
		}

		return results;
	}

}
