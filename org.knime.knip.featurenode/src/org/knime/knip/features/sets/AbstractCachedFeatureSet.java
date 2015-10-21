/*
 * ------------------------------------------------------------------------
 *
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
  ---------------------------------------------------------------------
 *
 */
package org.knime.knip.features.sets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.scijava.Priority;
import org.scijava.cache.CacheService;
import org.scijava.command.CommandInfo;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginService;

import net.imagej.ops.AbstractFunctionOp;
import net.imagej.ops.AbstractOp;
import net.imagej.ops.CustomOpEnvironment;
import net.imagej.ops.FunctionOp;
import net.imagej.ops.HybridOp;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;
import net.imagej.ops.cached.CachedOpEnvironment;
import net.imglib2.type.numeric.RealType;

/**
 * In an {@link AbstractCachedFeatureSet} intermediate results are cached during
 * computation, avoiding redundant computations of the same feature @see
 * {@link CachedOpEnvironment}.
 * 
 * @author Christian Dietz, University of Konstanz.
 * @param <I>
 *            type of the input
 * @param <O>
 *            type of the output
 */
public abstract class AbstractCachedFeatureSet<I, O extends RealType<O>>
		extends AbstractFunctionOp<I, Map<NamedFeature, O>> implements FeatureSet<I, O> {

	@Parameter
	private PluginService ps;

	@Parameter
	protected Class<? extends Op>[] prioritizedOps;

	@Override
	public void initialize() {
		final List<CommandInfo> infos = new ArrayList<CommandInfo>();
		if (prioritizedOps != null) {
			for (final Class<? extends Op> prio : prioritizedOps) {
				final CommandInfo info = new CommandInfo(prio);
				info.setPriority(Priority.FIRST_PRIORITY);
				infos.add(info);
			}
		}
		// FIXME: use CachedOpEnvironment if Converter bug is fixed
		setEnvironment(new KNIPCachedOpEnvironment(ops(), infos));
	}

	protected static class KNIPCachedOpEnvironment extends CustomOpEnvironment {

		@Parameter
		private CacheService cs;

		public KNIPCachedOpEnvironment(final OpEnvironment parent) {
			this(parent, null);
		}

		public KNIPCachedOpEnvironment(final OpEnvironment parent,
				final Collection<? extends CommandInfo> prioritizedInfos) {
			super(parent, prioritizedInfos);
			for (final CommandInfo info : prioritizedInfos) {
				info.setPriority(Priority.FIRST_PRIORITY);
			}
		}

		@Override
		public <I, O, OP extends Op> FunctionOp<I, O> function(final Class<OP> opType, final Class<O> outType,
				final Class<I> inType, Object... otherArgs) {
			final CachedFunctionOp<I, O> cached = new CachedFunctionOp<I, O>(
					super.function(opType, outType, inType, otherArgs), otherArgs);
			getContext().inject(cached);
			return cached;
		}

		@Override
		public <I, O, OP extends Op> FunctionOp<I, O> function(final Class<OP> opType, final Class<O> outType, I in,
				Object... otherArgs) {
			final CachedFunctionOp<I, O> cached = new CachedFunctionOp<I, O>(
					super.function(opType, outType, in, otherArgs), otherArgs);
			getContext().inject(cached);
			return cached;
		}

		@Override
		public <I, O, OP extends Op> HybridOp<I, O> hybrid(Class<OP> opType, Class<O> outType, Class<I> inType,
				Object... otherArgs) {
			final CachedHybridOp<I, O> cached = new CachedHybridOp<I, O>(
					super.hybrid(opType, outType, inType, otherArgs), otherArgs);
			getContext().inject(cached);
			return cached;
		}

		@Override
		public <I, O, OP extends Op> HybridOp<I, O> hybrid(Class<OP> opType, Class<O> outType, I in,
				Object... otherArgs) {
			final CachedHybridOp<I, O> cached = new CachedHybridOp<I, O>(super.hybrid(opType, outType, in, otherArgs),
					otherArgs);
			getContext().inject(cached);
			return cached;
		}

		/**
		 * Wraps a {@link FunctionOp} and caches the results. New inputs will
		 * result in re-computation of the result.
		 * 
		 * @author Christian Dietz, University of Konstanz
		 * @param <I>
		 * @param <O>
		 */
		protected class CachedFunctionOp<I, O> extends AbstractOp implements FunctionOp<I, O> {

			@Parameter
			private CacheService cache;

			private final FunctionOp<I, O> delegate;

			private final Object[] args;

			public CachedFunctionOp(final FunctionOp<I, O> delegate, final Object[] args) {
				this.delegate = delegate;
				this.args = args;
			}

			@SuppressWarnings("unchecked")
			public Class<FunctionOp<I, O>> getDelegateType() {
				return (Class<FunctionOp<I, O>>) this.delegate.getClass();
			}

			@Override
			public O compute(final I input) {

				final Hash hash = new Hash(input, delegate, args);

				@SuppressWarnings("unchecked")
				O output = (O) cache.get(hash);
				
				if (output == null) {
					output = delegate.compute(input);
					cache.put(hash, output);
				}
				return output;
			}

			@Override
			public void run() {
				delegate.run();
			}

			@Override
			public I in() {
				return delegate.in();
			}

			@Override
			public void setInput(I input) {
				delegate.setInput(input);
			}

			@Override
			public O out() {
				return delegate.out();
			}

			@Override
			public void initialize() {
				delegate.initialize();
			}

			@Override
			public CachedFunctionOp<I, O> getIndependentInstance() {
				return this;
			}

		}

		/**
		 * Wraps a {@link HybridOp} and caches the results. New inputs will
		 * result in re-computation if {@link HybridOp} is used as
		 * {@link FunctionOp}.
		 * 
		 * @author Christian Dietz, University of Konstanz
		 * @param <I>
		 * @param <O>
		 */
		protected class CachedHybridOp<I, O> extends CachedFunctionOp<I, O> implements HybridOp<I, O> {

			@Parameter
			private CacheService cache;

			private final HybridOp<I, O> delegate;

			private final Object[] args;

			public CachedHybridOp(final HybridOp<I, O> delegate, final Object[] args) {
				super(delegate, args);
				this.delegate = delegate;
				this.args = args;
			}

			@Override
			public O compute(final I input) {

				final Hash hash = new Hash(input, delegate, args);

				@SuppressWarnings("unchecked")
				O output = (O) cache.get(hash);

				if (output == null) {
					output = createOutput(input);
					compute(input, output);
					cache.put(hash, output);
				}
				return output;
			}

			@Override
			public O createOutput(I input) {
				return delegate.createOutput(input);
			}

			@Override
			public void compute(final I input, final O output) {
				delegate.compute(input, output);
			}

			@Override
			public CachedHybridOp<I, O> getIndependentInstance() {
				return this;
			}
		}

		/**
		 * Simple utility class to wrap two objects and an array of objects in a
		 * single object which combines their hashes.
		 */
		public static class Hash {

			private final int hash;

			public Hash(final Object o1, final Object o2, final Object[] args) {
				long hash = o1.hashCode() ^ o2.getClass().getSimpleName().hashCode();

				for (final Object o : args) {
					hash ^= o.hashCode();
				}

				this.hash = (int) hash;
			}

			@Override
			public int hashCode() {
				return hash;
			}

			@Override
			public boolean equals(final Object obj) {
				if (obj == this)
					return true;
				if (obj instanceof Hash)
					return hash == ((Hash) obj).hash;
				return false;
			}
		}
	}
}
