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
package org.knime.knip.base.nodes.filter;

import org.knime.node.v210.FullDescriptionDocument.FullDescription;
import org.knime.node.v210.KnimeNodeDocument;
import org.knime.node.v210.KnimeNodeDocument.KnimeNode;

import net.imagej.ImgPlus;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.ops.operation.iterable.unary.Variance;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * {@link VarianceFilterNodeFactory} as {@link SlidingWindowOperationNodeFactory}
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 *
 * @param <T>
 */
public class VarianceFilterNodeFactory<T extends RealType<T> & NativeType<T>>
        extends SlidingWindowOperationNodeFactory<T, T> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createNodeDescription(final KnimeNodeDocument doc) {
        final KnimeNode node = doc.addNewKnimeNode();
        node.setIcon("icons/imgfilter.png");
        node.setType(KnimeNode.Type.MANIPULATOR);
        node.setName("Variance Filter");
        node.setShortDescription("Applys Variance filtering to images in n-dimensions");
        final FullDescription desc = node.addNewFullDescription();
        desc.addNewIntro().addNewP().newCursor().setTextValue("Applys Variance filtering to images in n-dimensions");
    }

    @Override
    protected SlidingWindowOperationNodeDialog<T> createNodeDialog() {
        return new SlidingWindowOperationNodeDialog<T>(){
            /**
             * {@inheritDoc}
             */
            @Override
            protected String getDefaultSuffixForAppend() {
                return "_vf";
            }
        };
    }

    @Override
    public AbstractSlidingWindowOperationNodeModel<T, T> createNodeModel() {
        return new AbstractSlidingWindowOperationNodeModel<T, T>() {

            @Override
            protected T getOutType(final T inType) {
                return inType.createVariable();
            }

            @Override
            protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<T>>
                      getSlidingOperation(final ImgPlus<T> img, final T type, final Shape neighborhood,
                                          final OutOfBoundsFactory<T, ImgPlus<T>> outStrat) {

                return defaultUnary(new Variance<T, T>(), type, neighborhood, outStrat);
            }

            @Override
            protected int getMinDimensions() {
                return 1;
            }
        };
    }

    @Override
    protected String getSpeedUpOptionText() {
        return super.NO_SPEED_UP_TEXT;
    }

}
