package org.knime.knip.featurenode.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import net.imagej.ops.OpRef;
import net.imagej.ops.features.AbstractAutoResolvingFeatureSet;
import net.imagej.ops.features.LabeledFeatures;
import net.imglib2.IterableInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;

import org.knime.knip.featurenode.OpsGateway;
import org.scijava.command.CommandInfo;
import org.scijava.module.Module;

public class FeatureTask<T extends RealType<T> & NativeType<T>, L extends Comparable<L>>
		implements Callable<List<FeatureRowResult<T, L>>> {

	private final List<LabeledFeatures<IterableInterval<?>, T>> compiledFeatureSets;
	private FeatureRowInput<T, L> fri;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public FeatureTask(final List<FeatureSetInfo> list,
			final FeatureRowInput<T, L> fri) {

		this.compiledFeatureSets = new ArrayList<LabeledFeatures<IterableInterval<?>, T>>();
		this.fri = fri;

		for (final FeatureSetInfo fsi : list) {

			// immer input: IterableInterval
			final Module module = OpsGateway.getCommandService()
					.getModuleService()
					.createModule(new CommandInfo(fsi.getFeatureSetClass()));

			module.setInputs(fsi.getFieldNameAndValues());

			LabeledFeatures<IterableInterval<?>, T> fs = (LabeledFeatures<IterableInterval<?>, T>) module
					.getDelegateObject();

			if (AbstractAutoResolvingFeatureSet.class.isAssignableFrom(fs
					.getClass())) {
				final AbstractAutoResolvingFeatureSet<IterableInterval<?>, DoubleType> arfs = (AbstractAutoResolvingFeatureSet<IterableInterval<?>, DoubleType>) fs;
				final Set<OpRef<?>> ops = new HashSet<OpRef<?>>();
				for (final Entry<Class<?>, Boolean> entry : fsi
						.getSelectedFeatures().entrySet()) {
					if (!entry.getValue()) {
						continue;
					}

					ops.add(new OpRef(entry.getKey()));
				}

				fs = new AbstractAutoResolvingFeatureSet<IterableInterval<?>, T>() {

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

			OpsGateway.getContext().inject(fs);

			this.compiledFeatureSets.add(fs);
		}
	}

	@Override
	public List<FeatureRowResult<T, L>> call() throws Exception {
		final List<FeatureRowResult<T, L>> results = new ArrayList<FeatureRowResult<T, L>>();

		for (final Pair<IterableInterval<T>, L> ii : this.fri
				.getIterableIntervals()) {

			final List<Pair<String, T>> allResults = new ArrayList<Pair<String, T>>();

			for (final LabeledFeatures<IterableInterval<?>, T> labeledFeatures : this.compiledFeatureSets) {
				allResults.addAll(labeledFeatures.getFeatureList(ii.getA()));
			}

			results.add(new FeatureRowResult<T, L>(this.fri.getImgValue(),
					this.fri.getLabelingValue(), ii.getB(), allResults,
					this.fri.getOtherCells()));
		}

		return results;
	}
}
