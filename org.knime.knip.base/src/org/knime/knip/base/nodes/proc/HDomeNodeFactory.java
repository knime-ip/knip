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
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgView;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.HDomeTransformation;
import net.imglib2.ops.types.ConnectedType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeDialog;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeFactory;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeModel;
import org.knime.knip.core.util.ImgPlusFactory;

/**
 * HDomeNodeFactory
 *
 * @param <T>
 * @param <TMP>
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author muethingc, Univeristy of Konstanz
 *
 */
public class HDomeNodeFactory<T extends RealType<T>, TMP extends IterableInterval<T> & RandomAccessibleInterval<T>>
        extends ImgPlusToImgPlusNodeFactory<T, T> {

    private static SettingsModelString createConnectionTypeModel() {
        return new SettingsModelString("connection_type", ConnectedType.FOUR_CONNECTED.toString());
    }

    private static SettingsModelDouble createHeightModel() {
        return new SettingsModelDoubleBounded("height_dome", 1, 0, Double.MAX_VALUE);
    }

    private static SettingsModelDouble createSubtractModel() {
        return new SettingsModelDoubleBounded("height_subtraction", 0, 0, Double.MAX_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImgPlusToImgPlusNodeDialog<T> createNodeDialog() {
        return new ImgPlusToImgPlusNodeDialog<T>(2, Integer.MAX_VALUE, "X", "Y") {

            @Override
            public void addDialogComponents() {

                addDialogComponent("Options", "Regional Maxima Options", new DialogComponentStringSelection(
                        createConnectionTypeModel(), "Connection Type", ConnectedType.NAMES));

                addDialogComponent("Options", "Regional Maxima Options", new DialogComponentNumber(
                        createSubtractModel(), "Subtract Domes of Height Before Extraction", 1));

                addDialogComponent("Options", "Regional Maxima Options", new DialogComponentNumber(createHeightModel(),
                        "Height of Domes", 1));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImgPlusToImgPlusNodeModel<T, T> createNodeModel() {
        return new ImgPlusToImgPlusNodeModel<T, T>("X", "Y") {

            final class HDomOp implements UnaryOutputOperation<ImgPlus<T>, ImgPlus<T>> {

                private final T m_type;

                public HDomOp(final T type) {
                    m_type = type;
                }

                @Override
                public UnaryObjectFactory<ImgPlus<T>, ImgPlus<T>> bufferFactory() {
                    return ImgPlusFactory.<T, T> get(m_type);
                }

                @Override
                public ImgPlus<T> compute(final ImgPlus<T> input, final ImgPlus<T> output) {
                    HDomeTransformation<T> hDomeTransformation =
                            new HDomeTransformation<T>(ConnectedType.value(m_connection.getStringValue()),
                                    m_height.getDoubleValue(), m_subtract.getDoubleValue(),
                                    wrappedFactory(input.factory()));

                    // we really want to have the same buffer, that's why we
                    hDomeTransformation.compute(new ImgView<T>(input, wrappedFactory(input.factory())), new ImgView<T>(
                            output, wrappedFactory(output.factory())));

                    return output;
                }

                /**
                 * @param factory
                 * @return
                 */
                private ImgFactory<T> wrappedFactory(final ImgFactory<T> factory) {
                    return new ImgFactory<T>() {

                        @Override
                        public Img<T> create(final long[] dim, final T type) {
                            return new ImgView<T>(factory.create(dim, type), this);
                        }

                        @Override
                        public <S> ImgFactory<S> imgFactory(final S type) throws IncompatibleTypeException {
                            throw new UnsupportedOperationException("not supported by wrapped factory");
                        }
                    };
                }

                @Override
                public UnaryOutputOperation<ImgPlus<T>, ImgPlus<T>> copy() {
                    return new HDomOp(m_type);
                }
            }

            private final SettingsModelString m_connection = createConnectionTypeModel();

            private final SettingsModelDouble m_height = createHeightModel();

            private final SettingsModelDouble m_subtract = createSubtractModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_height);
                settingsModels.add(m_connection);
                settingsModels.add(m_subtract);
            }

            @Override
            protected UnaryOutputOperation<ImgPlus<T>, ImgPlus<T>> op(final ImgPlus<T> imgPlus) {
                return new HDomOp(imgPlus.firstElement().createVariable());
            }

            @Override
            protected int getMinDimensions() {
                return 2;
            }

        };

    }
}
