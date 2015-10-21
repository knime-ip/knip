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
package org.knime.knip.features.sets.optimizedfeatures;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import net.imagej.ops.AbstractFunctionOp;
import net.imagej.ops.FunctionOp;
import net.imagej.ops.Op;
import net.imagej.ops.Ops.Stats.Min;
import net.imagej.ops.Ops.Stats.MinMax;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;

/**
 * {@link Op} to calculate the {@code stats.min}.
 * 
 * @author Daniel Seebacher, University of Konstanz.
 * @param <IN>
 *            input type
 * @param <OUT>
 *            output type
 */
@Plugin(type = Min.class, label = "Statistics: Min", priority = Priority.FIRST_PRIORITY - 1)
public class OptimizedMin<IN extends RealType<IN>, OUT extends RealType<OUT>>
		extends AbstractFunctionOp<Iterable<IN>, OUT> implements Min {

	@SuppressWarnings("rawtypes")
	private FunctionOp<Iterable<IN>, Pair> function;

	@Override
	public void initialize() {
		super.initialize();
		function = ops().function(MinMax.class, Pair.class, in());
	}

	@SuppressWarnings("unchecked")
	@Override
	public OUT compute(Iterable<IN> input) {
		return (OUT) function.compute(input).getA();
	}

}