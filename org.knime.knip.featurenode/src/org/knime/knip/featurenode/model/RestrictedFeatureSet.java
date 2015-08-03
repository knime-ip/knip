package org.knime.knip.featurenode.model;

import java.util.Set;

import net.imagej.ops.OpRef;
import net.imagej.ops.features.AbstractAutoResolvingFeatureSet;
import net.imagej.ops.features.FeatureSet;

/**
 * A FeatureSet which has restricted output ops set.
 *
 * @author Daniel Seebacher, University of Konstanz.
 *
 * @param <I>
 * @param <O>
 */
public class RestrictedFeatureSet<I, O> extends AbstractAutoResolvingFeatureSet<I, O> {

	private final Class<?> featureSetClass;
	private final Set<OpRef<?>> outputOps;
	private final Set<OpRef<?>> hiddenOps;

	public RestrictedFeatureSet(final Class<?> featureSetClass, final Set<OpRef<?>> outputOps,
			final Set<OpRef<?>> hiddenOps) {
		this.featureSetClass = featureSetClass;
		this.outputOps = outputOps;
		this.hiddenOps = hiddenOps;
	}

	/**
	 * @return the {@link Class}ss of the {@link FeatureSet}
	 */
	public Class<?> getFeatureSetClass() {
		return this.featureSetClass;
	}

	@Override
	public Set<OpRef<?>> getOutputOps() {
		return this.outputOps;
	}

	@Override
	public Set<OpRef<?>> getHiddenOps() {
		return this.hiddenOps;
	}

}
