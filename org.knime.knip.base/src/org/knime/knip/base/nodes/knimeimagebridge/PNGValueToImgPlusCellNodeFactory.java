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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.iterator.LocalizingIntervalIterator;
import net.imglib2.meta.Axes;
import net.imglib2.meta.DefaultCalibratedAxis;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.knime.core.data.image.png.PNGImageValue;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.core.types.ImgFactoryTypes;
import org.knime.knip.core.util.EnumListProvider;

/**
 * Converts PNGImageValues to ImgPlusCells<UnsignedByteType>
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class PNGValueToImgPlusCellNodeFactory extends ValueToCellNodeFactory<PNGImageValue> {

    private static SettingsModelString createFactoryModel() {
        return new SettingsModelString("factoryselection", ImgFactoryTypes.ARRAY_IMG_FACTORY.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<PNGImageValue> createNodeDialog() {
        return new ValueToCellNodeDialog<PNGImageValue>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void addDialogComponents() {

                addDialogComponent("Settings",
                                   "Factory Selection",
                                   new DialogComponentStringSelection(createFactoryModel(), "Factory Type",
                                           EnumListProvider.getStringList(ImgFactoryTypes.ARRAY_IMG_FACTORY,
                                                                          ImgFactoryTypes.PLANAR_IMG_FACTORY,
                                                                          ImgFactoryTypes.CELL_IMG_FACTORY)));

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

            private ImgPlusCellFactory m_imgCellFactory;

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_factory);
            }

            /**
             * {@inheritDoc}
             * 
             * @throws IllegalArgumentException
             */
            @SuppressWarnings({"rawtypes"})
            @Override
            protected ImgPlusCell compute(final PNGImageValue cellValue) throws IOException {

                final BufferedImage image = (BufferedImage)cellValue.getImageContent().getImage();

                final ImgFactory imgFactory = ImgFactoryTypes.getImgFactory(m_factory.getStringValue(), null);

                @SuppressWarnings("unchecked")
                final Img<UnsignedByteType> img =
                        imgFactory.create(new long[]{image.getWidth(null), image.getHeight(null), 3},
                                          new UnsignedByteType());

                final LocalizingIntervalIterator iter =
                        new LocalizingIntervalIterator(new long[]{image.getWidth(), image.getHeight()});

                final RandomAccess<UnsignedByteType> access = img.randomAccess();

                final int[] pos = new int[2];
                while (iter.hasNext()) {
                    iter.fwd();
                    iter.localize(pos);

                    // Set position
                    access.setPosition(pos[0], 0);
                    access.setPosition(pos[1], 1);

                    // read rgb
                    final int pixelCol = image.getRGB(pos[0], pos[1]);

                    for (int c = 0; c < img.dimension(2); c++) {
                        access.setPosition(c, 2);
                        access.get().set(((pixelCol >>> ((img.dimension(2) - 1 - c) * 8)) & 0xff));
                    }
                }

                final ImgPlus<UnsignedByteType> imgPlus = new ImgPlus<UnsignedByteType>(img);

                imgPlus.setAxis(new DefaultCalibratedAxis(Axes.get("X")), 0);
                imgPlus.setAxis(new DefaultCalibratedAxis(Axes.get("Y")), 1);
                imgPlus.setAxis(new DefaultCalibratedAxis(Axes.get("Channel")), 2);

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
