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

import java.io.IOException;
import java.util.List;

import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.img.unary.ImgConvert;
import net.imglib2.ops.operation.img.unary.ImgConvert.ImgConversionTypes;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.Unsigned12BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.core.types.ImgFactoryTypes;
import org.knime.knip.core.types.NativeTypes;
import org.knime.knip.core.util.EnumListProvider;
import org.knime.knip.core.util.ImgUtils;

/**
 * Factory class to produce the Histogram Operations Node.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ConvertImgNodeFactory<T extends RealType<T> & NativeType<T>> extends
        ValueToCellNodeFactory<ImgPlusValue<T>> {

    private static SettingsModelString createConversionTypeModel() {
        return new SettingsModelString("scaling", ImgConversionTypes.DIRECT.getLabel());
    }

    private static SettingsModelString createFactorySelectionModel() {
        return new SettingsModelString("factoryselection", ImgFactoryTypes.SOURCE_FACTORY.toString());
    }

    private static SettingsModelString createTargetTypeModel() {
        return new SettingsModelString("targetformat", NativeTypes.BYTETYPE.toString());
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
            public void addDialogComponents() {
                addDialogComponent("Options", "Target Type", new DialogComponentStringSelection(
                        createTargetTypeModel(), "Target type", EnumListProvider.getStringList(NativeTypes.values())));
                addDialogComponent("Options", "Target Type", new DialogComponentStringSelection(
                        createConversionTypeModel(), "Conversion method", ImgConversionTypes.labelsAsStringArray()));
                addDialogComponent("Options", "Factory Selection",
                                   new DialogComponentStringSelection(createFactorySelectionModel(), "Factory Type",
                                           EnumListProvider.getStringList(ImgFactoryTypes.values())));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<T>> createNodeModel() {
        return new ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<T>>() {

            private final SettingsModelString m_conversionType = createConversionTypeModel();

            private final SettingsModelString m_factorySelection = createFactorySelectionModel();

            private ImgPlusCellFactory m_imgCellFactory;

            private final SettingsModelString m_targetType = createTargetTypeModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_factorySelection);
                settingsModels.add(m_conversionType);
                settingsModels.add(m_targetType);

            }

            /**
             * {@inheritDoc}
             * 
             * @throws IllegalArgumentException
             */
            @SuppressWarnings({"unchecked", "rawtypes"})
            @Override
            protected ImgPlusCell compute(final ImgPlusValue<T> cellValue) throws IOException {
                final ImgPlus<T> img = cellValue.getImgPlus();
                Img<? extends RealType> res = null;

                final ImgConversionTypes mode = ImgConversionTypes.getByLabel(m_conversionType.getStringValue());

                final ImgFactoryTypes facType = ImgFactoryTypes.valueOf(m_factorySelection.getStringValue());

                switch (NativeTypes.valueOf(m_targetType.getStringValue().toUpperCase())) {
                    case BITTYPE:
                        res = convert(img, new BitType(), facType, mode);
                        break;
                    case BYTETYPE:
                        res = convert(img, new ByteType(), facType, mode);
                        break;
                    case DOUBLETYPE:
                        res = convert(img, new DoubleType(), facType, mode);
                        break;
                    case FLOATTYPE:
                        res = convert(img, new FloatType(), facType, mode);
                        break;
                    case INTTYPE:
                        res = convert(img, new IntType(), facType, mode);
                        break;
                    case LONGTYPE:
                        res = convert(img, new LongType(), facType, mode);

                        break;
                    case SHORTTYPE:
                        res = convert(img, new ShortType(), facType, mode);

                        break;
                    case UNSIGNED12BITTYPE:
                        res = convert(img, new Unsigned12BitType(), facType, mode);
                        break;
                    case UNSIGNEDBYTETYPE:
                        res = convert(img, new UnsignedByteType(), facType, mode);
                        break;
                    case UNSIGNEDINTTYPE:
                        res = convert(img, new UnsignedIntType(), facType, mode);
                        break;
                    case UNSIGNEDSHORTTYPE:
                        res = convert(img, new UnsignedShortType(), facType, mode);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported conversion.");
                }

                return m_imgCellFactory.createCell(res, img);
            }

            public synchronized <O extends RealType<O> & NativeType<O>> Img<O> convert(final Img<T> img,
                                                                                       final O outType,
                                                                                       final ImgFactoryTypes facType,
                                                                                       final ImgConversionTypes mode) {

                final ImgConvert<T, O> imgConvert =
                        new ImgConvert<T, O>(img.firstElement().createVariable(), outType, mode);

                @SuppressWarnings("unchecked")
                final Img<O> res =
                        ImgUtils.<T, O> createEmptyCopy(img, ImgFactoryTypes.<T> getImgFactory(facType, img), outType);

                return imgConvert.compute(img, res);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_imgCellFactory = new ImgPlusCellFactory(exec);
            }

        };
    }

}
