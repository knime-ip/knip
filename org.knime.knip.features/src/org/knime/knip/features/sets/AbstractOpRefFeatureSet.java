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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.imagej.ops.OpRef;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.type.numeric.RealType;

import org.scijava.command.CommandService;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.Parameter;

/**
 * {@link OpRef} based {@link AbstractCachedFeatureSet}.
 * 
 * @author Christian Dietz, University of Konstanz.
 * @param <I>
 *            type of the input
 * @param <O>
 *            type of the output
 */
public abstract class AbstractOpRefFeatureSet<I, O extends RealType<O>> extends AbstractCachedFeatureSet<I, O> {

	protected final static String ATTR_FEATURE = "feature";

	protected final static String ATTR_TYPE = "feature_type";

	protected final static String ATTR_PARAMS = "feature_params";

	@Parameter
	private CommandService cs;

	// all features
	private Map<NamedFeature, UnaryFunctionOp<Object, ? extends O>> namedFeatureMap;

	@Override
	public List<NamedFeature> getFeatures() {

		final List<NamedFeature> features = new ArrayList<NamedFeature>();
		if (namedFeatureMap == null) {
			final Module self = cs.getCommand(this.getClass()).createModule(this);

			for (final ModuleItem<?> item : self.getInfo().inputs()) {
				// we found a feature. lets create a named feature!
				if (item.get(ATTR_FEATURE) != null && ((Boolean) item.getValue(self))) {
					features.add(new NamedFeature(item.getLabel()));
				}
			}
		} else {
			features.addAll(namedFeatureMap.keySet());
		}

		return features;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize() {
		super.initialize();

		namedFeatureMap = new LinkedHashMap<NamedFeature, UnaryFunctionOp<Object, ? extends O>>();

		final Module self = cs.getCommand(this.getClass()).createModule(this);
		try {
			for (final ModuleItem<?> item : self.getInfo().inputs()) {
				// we found a feature. lets create a named feature!
				if (item.get(ATTR_FEATURE) != null && ((Boolean) item.getValue(self))) {

					final String[] params;

					final String paramString = item.get(ATTR_PARAMS);
					if (paramString != null) {
						params = paramString.split(",");
					} else {
						params = new String[0];
					}

					final Object[] args = new Object[params.length];

					int i = 0;
					for (final String param : params) {
						args[i++] = self.getInput(param);
					}

					@SuppressWarnings("rawtypes")
					final OpRef ref = new OpRef(null, Class.forName((String) item.get(ATTR_TYPE)), null, null, args);

					namedFeatureMap.put(new NamedFeature(ref, item.getLabel()),
							(UnaryFunctionOp<Object, ? extends O>) Functions.unary(ops(), ref.getType(), RealType.class,
									in(), ref.getArgs()));
				}
			}
		} catch (final ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<NamedFeature, O> compute1(final I input) {
		final Map<NamedFeature, O> res = new HashMap<NamedFeature, O>();

		for (final Entry<NamedFeature, UnaryFunctionOp<Object, ? extends O>> entry : namedFeatureMap.entrySet()) {
			res.put(entry.getKey(), evalFunction(entry.getValue(), input));
		}

		return res;
	}

	/**
	 * Can be overriden by implementors to provide specialized implementations
	 * for certain functions.
	 * 
	 * @param func
	 *            function used to compute output. Will be any function added as
	 *            OpRef.
	 * @param input
	 *            input object
	 * @return
	 */
	protected O evalFunction(final UnaryFunctionOp<Object, ? extends O> func, final I input) {
		return func.compute1(input);
	}
}
