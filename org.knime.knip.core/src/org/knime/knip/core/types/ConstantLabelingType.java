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
package org.knime.knip.core.types;

import java.util.List;

import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.LabelingType;
import net.imglib2.type.numeric.IntegerType;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ConstantLabelingType<T extends Comparable<T>> extends LabelingType<T> {

    public ConstantLabelingType(final List<T> labeling) {
        super(labeling);
    }

    /**
     * Constructor for mirroring state with another labeling
     * 
     * @param type Wrapped type
     * @param mapping Mapping from wrapped type to LabelingList
     * @param generation Generation of the type
     */
    protected ConstantLabelingType(final IntegerType<?> type, final LabelingMapping<T> mapping, final long[] generation) {
        super(type, mapping, generation);
    }

    // this is the constructor if you want it to read from an array
    public ConstantLabelingType(final IntegerType<?> type, final LabelingMapping<T> mapping) {
        super(type, mapping);
    }

    public ConstantLabelingType(final T value) {
        super(value);
    }

    // this is the constructor if you want it to be a variable
    public ConstantLabelingType() {
        super();
    }

    /**
     * ConstantLabelingType. Nothing happens here
     * 
     * @param labeling
     */
    @Override
    public void setLabeling(final List<T> labeling) {
        // do nothing
    }

    /**
     * ConstantLabelingType. Nothing happens here
     * 
     * @param labeling
     */
    @Override
    public void setLabeling(final T[] labeling) {
        // do nothing
    }

    /**
     * ConstantLabelingType. Nothing happens here
     * 
     * @param m_labeling
     */
    @Override
    public void setLabel(final T label) {
        // do nothing
    }

    @Override
    public LabelingType<T> copy() {
        return new ConstantLabelingType<T>(getLabeling());
    }

}
