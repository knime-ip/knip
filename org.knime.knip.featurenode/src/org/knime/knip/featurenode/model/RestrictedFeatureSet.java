package org.knime.knip.featurenode.model;

import java.util.Set;

import net.imagej.ops.OpRef;
import net.imagej.ops.features.AbstractAutoResolvingFeatureSet;
import net.imagej.ops.features.LabeledFeatures;

public class RestrictedFeatureSet<I, O> extends
		AbstractAutoResolvingFeatureSet<I, O> implements
		LabeledFeatures<I, O> {

	private final Class<?> featureSetClass;
	private final Set<OpRef<?>> outputOps;
	private final Set<OpRef<?>> hiddenOps;

	public RestrictedFeatureSet(Class<?> featureSetClass,
			Set<OpRef<?>> outputOps, Set<OpRef<?>> hiddenOps) {
		this.featureSetClass = featureSetClass;
		this.outputOps = outputOps;
		this.hiddenOps = hiddenOps;
	}

	public Class<?> getFeatureSetClass() {
		return featureSetClass;
	}

	@Override
	public Set<OpRef<?>> getOutputOps() {
		return outputOps;
	}

	@Override
	public Set<OpRef<?>> getHiddenOps() {
		return hiddenOps;
	}

}
