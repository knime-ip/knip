/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.core.ops.labeling;

import java.util.Random;

import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.view.Views;

import org.knime.knip.core.data.LabelGenerator;

/**
 * 
 * 
 * random seed with a certain average distance
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class RandomSeeds<L extends Comparable<L>> implements UnaryOperation<Interval, Labeling<L>> {

    private final int m_avgDistance;

    private final LabelGenerator<L> m_seedGen;

    public RandomSeeds(final LabelGenerator<L> seedGen, final int avgDistance) {
        m_seedGen = seedGen;
        m_avgDistance = avgDistance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Labeling<L> compute(final Interval input, final Labeling<L> output) {
        m_seedGen.reset();
        final Random rand = new Random();
        final long[] currentGridPos = new long[output.numDimensions()];
        final RandomAccess<LabelingType<L>> out =
                Views.extendValue(output, output.firstElement().createVariable()).randomAccess();

        while (currentGridPos[currentGridPos.length - 1] < input.dimension(currentGridPos.length - 1)) {

            /* regular grid */

            currentGridPos[0] += m_avgDistance;

            // introduce randomness into the
            // grid position
            for (int i = 0; i < currentGridPos.length; i++) {
                out.setPosition(currentGridPos[i] + rand.nextInt(m_avgDistance), i);

            }
            out.get().setLabel(m_seedGen.nextLabel());

            // next position in the higher
            // dimensions than 0
            for (int i = 0; i < (output.numDimensions() - 1); i++) {
                if (currentGridPos[i] > input.dimension(i)) {
                    currentGridPos[i] = 0;
                    currentGridPos[i + 1] += m_avgDistance;
                }
            }

        }
        return output;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryOperation<Interval, Labeling<L>> copy() {
        return new RandomSeeds<L>(m_seedGen, m_avgDistance);
    }

}
