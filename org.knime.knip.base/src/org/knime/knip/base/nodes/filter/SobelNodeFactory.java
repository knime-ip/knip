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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.img.BinaryOperationAssignment;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.BinaryOperation;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.exceptions.KNIPRuntimeException;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeDialog;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeFactory;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeModel;
import org.knime.knip.base.node.dialog.DescriptionHelper;
import org.knime.knip.base.node.dialog.DialogComponentOutOfBoundsSelection;
import org.knime.knip.core.algorithm.convolvers.Convolver;
import org.knime.knip.core.algorithm.convolvers.DirectConvolver;
import org.knime.knip.core.algorithm.convolvers.ImgLib2FourierConvolver;
import org.knime.knip.core.algorithm.convolvers.filter.linear.ConstantFilter;
import org.knime.knip.core.types.OutOfBoundsStrategyEnum;
import org.knime.knip.core.types.OutOfBoundsStrategyFactory;
import org.knime.knip.core.util.ImgUtils;
import org.knime.node2012.KnimeNodeDocument.KnimeNode;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class SobelNodeFactory<T extends RealType<T> & NativeType<T>> extends ImgPlusToImgPlusNodeFactory<T, T> {

    protected static SettingsModelString createOutOfBoundsModel() {
        return new SettingsModelString("outofboundsstrategy", OutOfBoundsStrategyEnum.BORDER.toString());
    }

    protected static SettingsModelString createConvolverSelectionModel() {
        return new SettingsModelString("convolver", getConvolverNames().iterator().next());
    }

    public static Set<String> getConvolverNames() {

        Set<String> result = new HashSet<String>();
        result.add("Direct Convolver");
        result.add("ImgLib2 Fourier");

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImgPlusToImgPlusNodeDialog<T> createNodeDialog() {
        return new ImgPlusToImgPlusNodeDialog<T>(2, 2, "X", "Y") {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "Out of Bounds Strategy", new DialogComponentOutOfBoundsSelection(
                        createOutOfBoundsModel()));

                addDialogComponent("Options", "Convolution Selection", new DialogComponentStringSelection(
                        createConvolverSelectionModel(), "Convolution Method: ", getConvolverNames()));

            }

        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImgPlusToImgPlusNodeModel<T, T> createNodeModel() {
        return new ImgPlusToImgPlusNodeModel<T, T>("X", "Y") {

            private final SettingsModelString m_smOutOfBoundsStrategy = createOutOfBoundsModel();

            private final SettingsModelString m_smConvolverSelection = createConvolverSelectionModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_smOutOfBoundsStrategy);
                settingsModels.add(m_smConvolverSelection);
            }

            @Override
            protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<T>> op(final ImgPlus<T> imgPlus) {

                if (m_dimSelection.getNumSelectedDimLabels(imgPlus) != 2) {
                    throw new KNIPRuntimeException("image " + imgPlus.getName()
                            + " does not contain both of the two selected dimensions.");
                }

                return new SobelOp<T>(OutOfBoundsStrategyFactory.getStrategy(m_smOutOfBoundsStrategy.getStringValue(),
                                                                             imgPlus.firstElement()),
                        m_smConvolverSelection.getStringValue());
            }

            @Override
            protected int getMinDimensions() {
                return 2;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addNodeDescriptionContent(final KnimeNode node) {
        super.addNodeDescriptionContent(node);

        int index = DescriptionHelper.findTabIndex("Options", node.getFullDescription().getTabList());
        DialogComponentOutOfBoundsSelection.createNodeDescription(node.getFullDescription().getTabArray(index)
                .addNewOption());
    }

}

class SobelOp<T extends RealType<T> & NativeType<T>> implements UnaryOutputOperation<ImgPlus<T>, ImgPlus<T>> {

    private static final Img<DoubleType> X = ConstantFilter.Sobel.createImage(0);

    private static final Img<DoubleType> Y = ConstantFilter.Sobel.createImage(1);

    private OutOfBoundsFactory<T, RandomAccessibleInterval<T>> m_fac;

    private String m_selectedConvolver;

    public SobelOp(final OutOfBoundsFactory<T, RandomAccessibleInterval<T>> outOfBoundsFactory,
                   final String convolverName) {
        m_fac = outOfBoundsFactory;
        m_selectedConvolver = convolverName;
    }

    public Convolver<T, DoubleType, T> getConvolverByName(final String name) {

        if (name.equals("Direct Convolver")) {
            return new DirectConvolver<T, DoubleType, T>();
        }

        if (name.equals("ImgLib2 Fourier")) {
            return new ImgLib2FourierConvolver<T, DoubleType, T>();
        }

        return null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImgPlus<T> compute(final ImgPlus<T> img, final ImgPlus<T> resx) {

        final Convolver<T, DoubleType, T> cX = getConvolverByName(m_selectedConvolver);

        final Convolver<T, DoubleType, T> cY = getConvolverByName(m_selectedConvolver);

        cX.compute(Views.interval(Views.extend(img, m_fac), img), X, resx);

        final Img<T> resy = ImgUtils.createEmptyImg(img);

        cY.compute(Views.interval(Views.extend(img, m_fac), img), Y, resy);

        new BinaryOperationAssignment<T, T, T>(new BinaryOperation<T, T, T>() {

            @Override
            public T compute(final T input1, final T input2, final T output) {
                final double gradient =
                        Math.sqrt(Math.pow(input2.getRealDouble(), 2) + Math.pow(input1.getRealDouble(), 2));
                output.setReal(Math.min(Math.max(input1.getMinValue(), gradient), input1.getMaxValue()));

                return output;
            }

            @Override
            public BinaryOperation<T, T, T> copy() {
                return null;
            }

        }).compute(resx, resy, resx);

        return resx;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryObjectFactory<ImgPlus<T>, ImgPlus<T>> bufferFactory() {
        return new UnaryObjectFactory<ImgPlus<T>, ImgPlus<T>>() {

            @Override
            public ImgPlus<T> instantiate(final ImgPlus<T> a) {
                return new ImgPlus<T>(ImgUtils.createEmptyCopy(a), a);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryOutputOperation<ImgPlus<T>, ImgPlus<T>> copy() {
        return new SobelOp<T>(m_fac, m_selectedConvolver);
    }

}
