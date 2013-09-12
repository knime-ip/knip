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
package org.knime.knip.base.nodes.filter.convolver;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.BinaryObjectFactory;
import net.imglib2.ops.operation.BinaryOutputOperation;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.knip.core.algorithm.convolvers.DirectIterativeConvolver;
import org.knime.knip.core.types.OutOfBoundsStrategyEnum;
import org.knime.knip.core.types.OutOfBoundsStrategyFactory;
import org.knime.knip.core.util.ImgUtils;
import org.knime.node2012.OptionDocument.Option;
import org.knime.node2012.TabDocument.Tab;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class DirectIterativeConvolverExt<T extends RealType<T>, O extends RealType<O> & NativeType<O>, K extends RealType<K> & NativeType<K>>
        implements MultiKernelImageConvolverExt<T, K, O> {

    private OutOfBoundsStrategyEnum m_outOfBounds;

    private O m_resType;

    public DirectIterativeConvolverExt() {
        // Empty constructor must be available
    }

    @Override
    public void addNodeDescription(final Tab tab) {
        final Option opt = tab.addNewOption();
        opt.setName("Direct Iterative Convolution");
        opt.addNewP()
                .newCursor()
                .setTextValue("Direct convolution of the image. Convolution takes place in image space.  Each convolution is applied on the result of the last one");
    }

    @Override
    public BinaryObjectFactory<ImgPlus<T>, Img<K>[], ImgPlus<O>> bufferFactory() {
        return new BinaryObjectFactory<ImgPlus<T>, Img<K>[], ImgPlus<O>>() {

            @Override
            public ImgPlus<O> instantiate(final ImgPlus<T> inputA, final Img<K>[] inputB) {
                return new ImgPlus<O>(ImgUtils.createEmptyCopy(inputA, m_resType), inputA);
            }
        };
    }

    @Override
    public void close() {
        // Nothing to cleanup
    }

    @Override
    public ImgPlus<O> compute(final ImgPlus<T> input, final Img<K>[] kernels, final ImgPlus<O> output) {
        final OutOfBoundsFactory<T, RandomAccessibleInterval<T>> a =
                OutOfBoundsStrategyFactory.getStrategy(m_outOfBounds, input.firstElement(), input.firstElement());

        final OutOfBoundsFactory<O, RandomAccessibleInterval<O>> b =
                OutOfBoundsStrategyFactory.getStrategy(m_outOfBounds, output.firstElement(), input.firstElement());

        return (ImgPlus<O>)new DirectIterativeConvolver<T, K, O>(output.factory(), a, b)
                .compute(input, kernels, output);
    }

    @Override
    public BinaryOutputOperation<ImgPlus<T>, Img<K>[], ImgPlus<O>> copy() {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public List<SettingsModel> getAdditionalSettingsModels() {
        return new ArrayList<SettingsModel>();
    }

    @Override
    public DialogComponent getDialogComponent() {
        return null;
    }

    @Override
    public String getName() {
        return "Direct Convolver (Iterative)";
    }

    @Override
    public void load() {
        // Nothing to do here
    }

    @Override
    public void setOutOfBounds(final OutOfBoundsStrategyEnum outOfBounds) {
        m_outOfBounds = outOfBounds;
    }

    @Override
    public void setResultType(final O resType) {
        m_resType = resType;
    }
}
