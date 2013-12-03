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
package org.knime.knip.base.nodes.proc;

import java.util.List;

import net.imglib2.IterableInterval;
import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.img.UnaryOperationBasedConverter;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.real.unary.RealUnaryOperation;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.IterableIntervalsNodeDialog;
import org.knime.knip.base.node.IterableIntervalsNodeFactory;
import org.knime.knip.base.node.IterableIntervalsNodeModel;
import org.knime.node2012.FullDescriptionDocument.FullDescription;
import org.knime.node2012.KnimeNodeDocument;
import org.knime.node2012.KnimeNodeDocument.KnimeNode;

/**
 * Factory class to produce an image inverter node.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @param <T>
 * @param <L>
 */
public class InverterNodeFactory2<T extends RealType<T>, L extends Comparable<L>> extends
        IterableIntervalsNodeFactory<T, T, L> {

    public InverterNodeFactory2() {
        super();
        m_hasDimensionSelection = false;
    }

    private class SignedRealInvert<I extends RealType<I>, O extends RealType<O>> implements RealUnaryOperation<I, O> {

        @Override
        public O compute(final I x, final O output) {
            final double value = x.getRealDouble() * -1.0 - 1;
            output.setReal(value);
            return output;
        }

        @Override
        public SignedRealInvert<I, O> copy() {
            return new SignedRealInvert<I, O>();
        }

    }

    private class UnsignedRealInvert<I extends RealType<I>, O extends RealType<O>> implements RealUnaryOperation<I, O> {
        private final double m_specifiedMax;

        /**
         * Constructor.
         *
         * @param specifiedMax - maximum value of the range to invert about
         */
        public UnsignedRealInvert(final double specifiedMax) {
            this.m_specifiedMax = specifiedMax;
        }

        @Override
        public O compute(final I x, final O output) {
            final double value = m_specifiedMax - x.getRealDouble();
            output.setReal(value);
            return output;
        }

        @Override
        public UnsignedRealInvert<I, O> copy() {
            return new UnsignedRealInvert<I, O>(m_specifiedMax);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createNodeDescription(final KnimeNodeDocument doc) {
        final KnimeNode node = doc.addNewKnimeNode();
        node.setIcon("icons/inverter.png");
        node.setType(KnimeNode.Type.MANIPULATOR);
        node.setName("Inverter");
        node.setShortDescription("Inverts Images");
        final FullDescription desc = node.addNewFullDescription();
        desc.addNewIntro().addNewP().newCursor().setTextValue("Inverts Images");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IterableIntervalsNodeModel<T, T, L> createNodeModel() {
        return new IterableIntervalsNodeModel<T, T, L>(false) {

            private UnaryOperation<T, T> m_preparedOp;

            @Override
            protected T getOutType(final T input) {
                return input.createVariable();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected ImgPlusCell<T> compute(final ImgPlusValue<T> cellValue) throws Exception {
                if (isLabelingPresent()) {
                    // here we really can't optimize using converters
                    return super.compute(cellValue);
                } else {
                    // this can be done faster
                    ImgPlus<T> in = cellValue.getImgPlus();
                    return m_cellFactory.createCell(new ImgPlus<T>(new ImgView<T>(
                                                            new ConvertedRandomAccessibleInterval<T, T>(in,
                                                                    new UnaryOperationBasedConverter<T, T>(createOp(in
                                                                            .firstElement().createVariable())),
                                                                    getOutType(in.firstElement())), in.factory()), in),
                                                    cellValue.getMinimum());
                }
            }

            @Override
            public void prepareOperation(final T input) {
                m_preparedOp = createOp(input);
            }

            @Override
            public UnaryOperation<IterableInterval<T>, IterableInterval<T>> operation() {
                return Operations.map(m_preparedOp);
            }

            private UnaryOperation<T, T> createOp(final T input) {
                UnaryOperation<T, T> invert;
                if (input.getMinValue() < 0) {
                    invert = new SignedRealInvert<T, T>();
                } else {
                    invert = new UnsignedRealInvert<T, T>(input.getMaxValue());
                }
                return invert;
            }

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                //
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IterableIntervalsNodeDialog<T> createNodeDialog() {
        return new IterableIntervalsNodeDialog<T>(false) {

            /**
             * {@inheritDoc}
             */
            @Override
            protected String getDefaultSuffixForAppend() {
                return "_inverted";
            }

            @Override
            public void addDialogComponents() {
                //
            }
        };
    }
}
