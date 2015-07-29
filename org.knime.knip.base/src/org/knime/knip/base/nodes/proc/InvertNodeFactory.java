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

import net.imagej.ImgPlus;
import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.ops.img.UnaryOperationBasedConverter;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.real.unary.RealUnaryOperation;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.node.v210.FullDescriptionDocument.FullDescription;
import org.knime.node.v210.KnimeNodeDocument;
import org.knime.node.v210.KnimeNodeDocument.KnimeNode;

/**
 * Factory class to produce an image inverter node.
 *
 * Use InverterNodeFactory
 *
 * @param <T>
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
@Deprecated
public class InvertNodeFactory<T extends RealType<T>> extends ValueToCellNodeFactory<ImgPlusValue<T>> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createNodeDescription(final KnimeNodeDocument doc) {
        final KnimeNode node = doc.addNewKnimeNode();
        node.setIcon("icons/inverter.png");
        node.setType(KnimeNode.Type.MANIPULATOR);
        node.setName("Inverter (Deprecated)");
        node.setShortDescription("Inverts Images");
        final FullDescription desc = node.addNewFullDescription();
        desc.addNewIntro().addNewP().newCursor().setTextValue("Inverts Images");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<T>> createNodeModel() {
        return new ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<T>>() {
            private ImgPlusCellFactory m_imgCellFactory;

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {

            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_imgCellFactory = new ImgPlusCellFactory(exec);
            }

            @SuppressWarnings({"unchecked", "rawtypes"})
            @Override
            protected ImgPlusCell<T> compute(final ImgPlusValue<T> cellValue) throws Exception {
                final Img<T> img = cellValue.getImgPlus();
                UnaryOperation<T, T> invert;
                T type = img.firstElement().createVariable();
                if (type instanceof IntegerType) {
                    if (img.firstElement().getMinValue() < 0) {
                        invert = new SignedIntegerInvert();
                    } else {
                        invert = new UnsignedIntegerInvert((long)type.getMaxValue());
                    }
                } else {
                    invert = new RealInvert<T, T>();
                }

                return m_imgCellFactory.createCell(new ImgPlus<>(new ImgView<T>(Views
                        .translate(new ConvertedRandomAccessibleInterval<T, T>(img,
                                           new UnaryOperationBasedConverter<T, T>(invert), img.firstElement()
                                                   .createVariable()),
                                   ((ImgPlusValue)cellValue).getMinimum()), img.factory()), cellValue.getMetadata()));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue<T>>() {

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

    private class SignedIntegerInvert<I extends IntegerType<I>, O extends IntegerType<O>> implements
            RealUnaryOperation<I, O> {

        @Override
        public O compute(final I x, final O output) {
            final long value = x.getIntegerLong() * -1L - 1L;
            output.setInteger(value);
            return output;
        }

        @Override
        public SignedIntegerInvert<I, O> copy() {
            return new SignedIntegerInvert<I, O>();
        }

    }

    private class UnsignedIntegerInvert<I extends IntegerType<I>, O extends IntegerType<O>> implements
            RealUnaryOperation<I, O> {
        private final long m_specifiedMax;

        /**
         * Constructor.
         *
         * @param specifiedMax - maximum value of the range to invert about
         */
        public UnsignedIntegerInvert(final long specifiedMax) {
            this.m_specifiedMax = specifiedMax;
        }

        @Override
        public O compute(final I x, final O output) {
            final long value = m_specifiedMax - x.getIntegerLong();
            output.setInteger(value);
            return output;
        }

        @Override
        public UnsignedIntegerInvert<I, O> copy() {
            return new UnsignedIntegerInvert<I, O>(m_specifiedMax);
        }

    }

    private class RealInvert<I extends RealType<I>, O extends RealType<O>> implements RealUnaryOperation<I, O> {

        @Override
        public O compute(final I x, final O output) {
            final double value = x.getRealDouble() * -1;
            output.setReal(value);
            return output;
        }

        @Override
        public RealInvert<I, O> copy() {
            return new RealInvert<I, O>();
        }

    }
}
