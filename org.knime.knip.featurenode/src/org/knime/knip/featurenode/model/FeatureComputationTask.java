package org.knime.knip.featurenode.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import net.imagej.ops.OpRef;
import net.imagej.ops.features.AbstractAutoResolvingFeatureSet;
import net.imagej.ops.features.FeatureSet;
import net.imagej.ops.features.LabeledFeatures;
import net.imglib2.IterableInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

import org.knime.knip.featurenode.OpsGateway;
import org.scijava.command.CommandInfo;
import org.scijava.module.Module;

/**
 * This class compiles all given input {@link FeatureSet}s in the constructor
 * and computes the results of the {@link FeatureSet}s for the given
 * {@link FeatureTaskInput} in the {@link FeatureComputationTask#call()} method.
 * 
 * @author Daniel Seebacher, University of Konstanz.
 *
 * @param <T extends RealType<T> & NativeType<T>>
 * @param <L extends Comparable<L>>
 */
public class FeatureComputationTask<T extends RealType<T> & NativeType<T>, L extends Comparable<L>>
		implements Callable<List<FeatureTaskOutput<T, L>>> {

	private final List<LabeledFeatures<IterableInterval<T>, T>> compiledFeatureSets;
	private FeatureTaskInput<T, L> featureTaskInput;

	/**
	 * Default constructor.
	 * 
	 * @param inputFeatureSets
	 *            A list of {@link FeatureSetInfo}s.
	 * @param featureTaskInput
	 *            A {@link FeatureTaskInput}
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public FeatureComputationTask(final List<FeatureSetInfo> inputFeatureSets,
			final FeatureTaskInput<T, L> featureTaskInput) {

		this.compiledFeatureSets = new ArrayList<LabeledFeatures<IterableInterval<T>, T>>();
		this.featureTaskInput = featureTaskInput;

		// for each feature set info
		for (final FeatureSetInfo fsi : inputFeatureSets) {

			// create a module of the feature set
			final Module module = OpsGateway.getCommandService()
					.getModuleService()
					.createModule(new CommandInfo(fsi.getFeatureSetClass()));

			// set the parameters
			module.setInputs(fsi.getFieldNameAndValues());

			// get the feature set
			LabeledFeatures<IterableInterval<T>, T> fs = (LabeledFeatures<IterableInterval<T>, T>) module
					.getDelegateObject();

			// if the feature set is an AbstractAutoResolvingFeatureSet check
			// which features need to be computed
			if (AbstractAutoResolvingFeatureSet.class.isAssignableFrom(fs
					.getClass())) {
				final AbstractAutoResolvingFeatureSet<IterableInterval<T>, DoubleType> arfs = (AbstractAutoResolvingFeatureSet<IterableInterval<T>, DoubleType>) fs;

				// get all selected features
				final Set<OpRef<?>> ops = new HashSet<OpRef<?>>();
				for (final Entry<Class<?>, Boolean> entry : fsi
						.getSelectedFeatures().entrySet()) {
					if (!entry.getValue()) {
						continue;
					}

					ops.add(new OpRef(entry.getKey()));
				}

				// create a new AbstractAutoResolvingFeatureSet with only the
				// selected features
				fs = new AbstractAutoResolvingFeatureSet<IterableInterval<T>, T>() {

					@Override
					public Set<OpRef<?>> getOutputOps() {
						return ops;
					}

					@Override
					public Set<OpRef<?>> getHiddenOps() {
						return arfs.getHiddenOps();
					}

				};
			}

			// initialize
			OpsGateway.getContext().inject(fs);
			this.compiledFeatureSets.add(fs);
		}
	}

	@Override
	public List<FeatureTaskOutput<T, L>> call() throws Exception {
		final List<FeatureTaskOutput<T, L>> results = new ArrayList<FeatureTaskOutput<T, L>>();

		// for each iterable interval in the input
		for (final Pair<IterableInterval<T>, L> input : this.featureTaskInput
				.getIterableIntervals()) {
			IterableInterval<T> ii = input.getA();
			L label = input.getB();

			List<Pair<String, T>> featureSetResults = new ArrayList<Pair<String, T>>();

			// get the results from each feature set
			for (LabeledFeatures<IterableInterval<T>, T> featureSet : compiledFeatureSets) {
				featureSetResults.addAll(featureSet.getFeatureList(ii));
			}

			results.add(new FeatureTaskOutput<T, L>(featureTaskInput,
					new ValuePair<List<Pair<String, T>>, L>(featureSetResults,
							label)));
		}

		return results;
	}
}
