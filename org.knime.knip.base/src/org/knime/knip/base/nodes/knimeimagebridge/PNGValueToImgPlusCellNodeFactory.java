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
package org.knime.knip.base.nodes.knimeimagebridge;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.image.png.PNGImageValue;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.core.types.ImgFactoryTypes;
import org.knime.knip.core.util.EnumUtils;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgView;
import net.imglib2.iterator.LocalizingIntervalIterator;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

/**
 * Converts PNGImageValues to ImgPlusCells<UnsignedByteType>
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author <a href="mailto:danielseebacher@t-online.de">Daniel Seebacher</a>
 */
@SuppressWarnings("deprecation")
public class PNGValueToImgPlusCellNodeFactory extends ValueToCellNodeFactory<PNGImageValue> {

    private static SettingsModelString createFactoryModel() {
        return new SettingsModelString("factoryselection", ImgFactoryTypes.ARRAY_IMG_FACTORY.toString());
    }

    private static SettingsModelBoolean createReplaceAlphaValueWithConstantModel() {
        return new SettingsModelBoolean("replacealphavaluewithconstant", true);
    }

    private static SettingsModelIntegerBounded createConstantAlphaValueModel() {
        return new SettingsModelIntegerBounded("constantalphavalue", 255, 0, 255);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<PNGImageValue> createNodeDialog() {
        return new ValueToCellNodeDialog<PNGImageValue>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Settings", "Factory Selection",
                                   new DialogComponentStringSelection(createFactoryModel(), "Factory Type",
                                           EnumUtils.getStringListFromName(ImgFactoryTypes.ARRAY_IMG_FACTORY,
                                                                           ImgFactoryTypes.PLANAR_IMG_FACTORY,
                                                                           ImgFactoryTypes.CELL_IMG_FACTORY)));

                SettingsModelBoolean replace = createReplaceAlphaValueWithConstantModel();
                SettingsModelIntegerBounded alphval = createConstantAlphaValueModel();

                addDialogComponent("Settings", "Color Handling", new DialogComponentBoolean(replace,
                        "Replace Transparent Alpha Values With Constant?"));
                addDialogComponent("Settings", "Color Handling",
                                   new DialogComponentNumber(alphval, "Constant Alpha Replacement Value", 1));

                replace.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(final ChangeEvent e) {
                        alphval.setEnabled(replace.getBooleanValue());
                    }
                });
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<PNGImageValue, ImgPlusCell<UnsignedByteType>> createNodeModel() {
        return new ValueToCellNodeModel<PNGImageValue, ImgPlusCell<UnsignedByteType>>() {

            private final SettingsModelString m_factory = createFactoryModel();

            private final SettingsModelBoolean m_replaceTransparentAlphaWithConstant =
                    createReplaceAlphaValueWithConstantModel();

            private final SettingsModelIntegerBounded m_alphaReplacementValue = createConstantAlphaValueModel();

            private ImgPlusCellFactory m_imgCellFactory;

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_factory);
                settingsModels.add(m_replaceTransparentAlphaWithConstant);
                settingsModels.add(m_alphaReplacementValue);
            }

            /**
             * {@inheritDoc}
             *
             * @throws IllegalArgumentException
             */
            @SuppressWarnings({"rawtypes", "unchecked"})
            @Override
            protected ImgPlusCell compute(final PNGImageValue cellValue) throws IOException {

                final BufferedImage image = (BufferedImage)cellValue.getImageContent().getImage();

                // check if the png image has an alpha channel
                //                boolean hasAlphaChannel = m_hasAlphaChannel.getBooleanValue();
                final ImgFactory imgFactory = ImgFactoryTypes.getImgFactory(m_factory.getStringValue(), null);

                // always create rgb image with alpha channel, slice the alpha channel if nothing was written to it
                final Img<UnsignedByteType> img =
                        imgFactory.create(new long[]{image.getWidth(), image.getHeight(), 4}, new UnsignedByteType());

                final LocalizingIntervalIterator iter =
                        new LocalizingIntervalIterator(new long[]{image.getWidth(), image.getHeight()});
                final RandomAccess<UnsignedByteType> access = img.randomAccess();

                boolean hasAlphaValues = false;
                while (iter.hasNext()) {
                    // Set position
                    iter.fwd();
                    access.setPosition(new long[]{iter.getIntPosition(0), iter.getIntPosition(1), 0});

                    // get color rgb
                    final Color pixelCol =
                            new Color(image.getRGB(iter.getIntPosition(0), iter.getIntPosition(1)), true);

                    // set rgb
                    access.get().set(pixelCol.getRed());
                    access.move(1, 2);
                    access.get().set(pixelCol.getGreen());
                    access.move(1, 2);
                    access.get().set(pixelCol.getBlue());

                    // get alpha value
                    int alpha = pixelCol.getAlpha();

                    // set alpha value
                    access.move(1, 2);
                    if (255 == alpha && m_replaceTransparentAlphaWithConstant.getBooleanValue()) {
                        access.get().set(m_alphaReplacementValue.getIntValue());
                    } else {
                        // important, check if there is any value that isn't 0.
                        hasAlphaValues = true;
                        access.get().set(alpha);
                    }
                }

                final ImgPlus<UnsignedByteType> imgPlus;

                // if the image has alpha values, just create the imgplus from the img
                if (hasAlphaValues) {
                    imgPlus = new ImgPlus<UnsignedByteType>(img);
                }
                // if the image has NO alpha values, we can remove the alpha channel
                else {
                    imgPlus =
                            new ImgPlus<UnsignedByteType>(new ImgView<UnsignedByteType>(
                                    Views.interval(img, new long[]{0, 0, 0},
                                                   new long[]{image.getWidth() - 1, image.getHeight() - 1, 2}),
                                    imgFactory));
                }

                imgPlus.setAxis(new DefaultLinearAxis(Axes.get("X")), 0);
                imgPlus.setAxis(new DefaultLinearAxis(Axes.get("Y")), 1);
                imgPlus.setAxis(new DefaultLinearAxis(Axes.get("Channel")), 2);

                return m_imgCellFactory.createCell(imgPlus);
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

    //    /**
    //     * Dialog for the {@link PNGValueToImgPlusCellNodeFactory}.
    //     *
    //     * @author <a href="mailto:danielseebacher@t-online.de">Daniel Seebacher</a>
    //     */
    //    private class PNGValueToImgPlusCellNodeDialog extends ValueToCellNodeDialog<PNGImageValue> {
    //
    //        private SettingsModelString m_factoryType;
    //
    //        private SettingsModelBoolean m_replaceAlphaValueWithConstant;
    //
    //        private SettingsModelNumber m_createConstantAlphaValueModel;
    //
    //        /**
    //         * @param factoryType the factory type model
    //         * @param replaceAlphaValueWithConstant the alpha replacement model
    //         * @param createConstantAlphaValueModel the alpha replacement value model
    //         *
    //         */
    //        public PNGValueToImgPlusCellNodeDialog(final SettingsModelString factoryType,
    //                                               final SettingsModelBoolean replaceAlphaValueWithConstant,
    //                                               final SettingsModelNumber createConstantAlphaValueModel) {
    //            super(true);
    //            this.m_factoryType = factoryType;
    //            this.m_replaceAlphaValueWithConstant = replaceAlphaValueWithConstant;
    //            this.m_createConstantAlphaValueModel = createConstantAlphaValueModel;
    //
    //            replaceAlphaValueWithConstant.addChangeListener(new ChangeListener() {
    //                @Override
    //                public void stateChanged(final ChangeEvent e) {
    //                    createConstantAlphaValueModel.setEnabled(replaceAlphaValueWithConstant.getBooleanValue());
    //                }
    //            });
    //
    //            addDialogComponents();
    //            buildDialog();
    //        }
    //
    //        /**
    //         * {@inheritDoc}
    //         */
    //        @Override
    //        public void addDialogComponents() {
    //            addDialogComponent("Settings", "Factory Selection",
    //                               new DialogComponentStringSelection(m_factoryType, "Factory Type",
    //                                       EnumUtils.getStringListFromName(ImgFactoryTypes.ARRAY_IMG_FACTORY,
    //                                                                       ImgFactoryTypes.PLANAR_IMG_FACTORY,
    //                                                                       ImgFactoryTypes.CELL_IMG_FACTORY)));
    //
    //            addDialogComponent("Settings", "Color Handling", new DialogComponentBoolean(m_replaceAlphaValueWithConstant,
    //                    "Replace Transparent Alpha Values With Constant?"));
    //            addDialogComponent("Settings", "Color Handling", new DialogComponentNumber(m_createConstantAlphaValueModel,
    //                    "Constant Alpha Replacement Value", 1));
    //
    //            replaceAlphaValueWithConstant.addChangeListener(new ChangeListener() {
    //                @Override
    //                public void stateChanged(final ChangeEvent e) {
    //                    createConstantAlphaValueModel.setEnabled(replaceAlphaValueWithConstant.getBooleanValue());
    //                }
    //            });
    //        }
    //
    //    }
}
