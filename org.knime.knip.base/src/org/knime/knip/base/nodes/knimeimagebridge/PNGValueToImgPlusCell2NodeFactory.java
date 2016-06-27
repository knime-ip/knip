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
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel Einsdorf</a>
 */
@SuppressWarnings("deprecation")
public class PNGValueToImgPlusCell2NodeFactory extends ValueToCellNodeFactory<PNGImageValue> {

    private static final String SETTINGS_TAB = "Settings";

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
                addDialogComponent(SETTINGS_TAB, "Factory Selection",
                                   new DialogComponentStringSelection(createFactoryModel(), "Factory Type",
                                           EnumUtils.getStringListFromName(ImgFactoryTypes.ARRAY_IMG_FACTORY,
                                                                           ImgFactoryTypes.PLANAR_IMG_FACTORY,
                                                                           ImgFactoryTypes.CELL_IMG_FACTORY)));

                SettingsModelBoolean replace = createReplaceAlphaValueWithConstantModel();
                SettingsModelIntegerBounded alphval = createConstantAlphaValueModel();

                addDialogComponent(SETTINGS_TAB, "Color Handling", new DialogComponentBoolean(replace,
                        "Replace Transparent Alpha Values With Constant?"));
                addDialogComponent(SETTINGS_TAB, "Color Handling",
                                   new DialogComponentNumber(alphval, "Constant Alpha Replacement Value", 1));

                replace.addChangeListener(e -> alphval.setEnabled(replace.getBooleanValue()));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected String getDefaultSuffixForAppend() {
                return "_PNGtoIP";
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

                final ImgFactory imgFactory = ImgFactoryTypes.getImgFactory(m_factory.getStringValue(), null);

                // always create rgb image with alpha channel, slice the alpha channel if nothing was written to it
                final Img<UnsignedByteType> img =
                        imgFactory.create(new long[]{image.getWidth(), image.getHeight(), 4}, new UnsignedByteType());

                final LocalizingIntervalIterator iter =
                        new LocalizingIntervalIterator(new long[]{image.getWidth(), image.getHeight()});
                final RandomAccess<UnsignedByteType> access = img.randomAccess();

                // if we replace with a transparent
                boolean settingTransparentAlphaValue = m_replaceTransparentAlphaWithConstant.getBooleanValue()
                        && m_alphaReplacementValue.getIntValue() < 255;

                boolean hasTransparentInputAlphaValues = false;
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

                    if (m_replaceTransparentAlphaWithConstant.getBooleanValue()) {
                        access.get().set(m_alphaReplacementValue.getIntValue());
                    } else {
                        access.get().set(alpha);
                    }

                    // detect if we are setting are any transparent pixels
                    if (alpha < 255) {
                        hasTransparentInputAlphaValues = true;
                    }
                }

                final ImgPlus<UnsignedByteType> imgPlus;

                // if the image has alpha values, just create the imgplus from the img
                // NB: the second condition ensures that we only add an alpha channel if we are not setting the alpha channel to 255 everywhere.
                if (settingTransparentAlphaValue
                        || hasTransparentInputAlphaValues && !m_replaceTransparentAlphaWithConstant.getBooleanValue()) {
                    imgPlus = new ImgPlus<>(img);
                }
                // if the image has NO alpha values, we can remove the alpha channel
                else {
                    imgPlus =
                            new ImgPlus<>(new ImgView<UnsignedByteType>(
                                    Views.interval(img, new long[]{0, 0, 0},
                                                   new long[]{image.getWidth() - 1, image.getHeight() - 1, 2}),
                                    imgFactory));
                }

                imgPlus.setAxis(new DefaultLinearAxis(Axes.get("X")), 0);
                imgPlus.setAxis(new DefaultLinearAxis(Axes.get("Y")), 1);
                imgPlus.setAxis(new DefaultLinearAxis(Axes.get("Channel")), 2);
                imgPlus.setName("Untitled");

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
}
