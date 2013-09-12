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
package org.knime.knip.base.nodes.seg.labeltopng;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import net.imglib2.Interval;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingType;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.image.png.PNGImageCell;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColor;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.exceptions.KNIPException;
import org.knime.knip.base.node.TwoValuesToCellNodeModel;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.awt.AWTImageTools;
import org.knime.knip.core.awt.BoundingBoxLabelRenderer;
import org.knime.knip.core.awt.BoundingBoxRandomColorLabelRenderer;
import org.knime.knip.core.awt.ColorLabelingRenderer;
import org.knime.knip.core.awt.ImageRenderer;
import org.knime.knip.core.awt.Real2GreyRenderer;
import org.knime.knip.core.awt.Transparency;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTable;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableRenderer;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTableUtils;
import org.knime.knip.core.awt.labelingcolortable.RandomMissingColorHandler;
import org.knime.knip.core.awt.parametersupport.RendererWithLabels;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class LabelingToPNGValueNodeModel<T extends RealType<T>, L extends Comparable<L> & Type<L>> extends
        TwoValuesToCellNodeModel<ImgPlusValue<T>, LabelingValue<L>, ListCell> {

    //TODO provide static name access for the renderers
    @SuppressWarnings("rawtypes")
    final static String[] RENDERER_NAMES = new String[]{new ColorLabelingRenderer().toString(),
            new BoundingBoxLabelRenderer().toString(), new BoundingBoxRandomColorLabelRenderer().toString()};

    NodeLogger LOGGER = NodeLogger.getLogger(LabelingToPNGValueNodeFactory.class);

    static SettingsModelStringArray createRendererSM() {
        return new SettingsModelStringArray("rendererSettingsModel", new String[]{RENDERER_NAMES[0]});
    }

    static SettingsModelDimSelection createDimSelectionModelPlane() {
        return new SettingsModelDimSelection("dimensions_plane", "X", "Y");
    }

    static SettingsModelIntegerBounded createTransparencySM() {
        return new SettingsModelIntegerBounded("transparency", 128, 0, 255);
    }

    static SettingsModelColor createColorSM() {
        return new SettingsModelColor("boundingBoxSM", LabelingColorTableUtils.getBoundingBoxColor());
    }

    static SettingsModelBoolean createShowBoundingBoxNameSM() {
        return new SettingsModelBoolean("showBoundingBoxLabelsSM", true);
    }

    //create settings models

    private SettingsModelIntegerBounded m_transparency = createTransparencySM();

    private SettingsModelDimSelection m_dimensionsPlane = createDimSelectionModelPlane();

    private SettingsModelStringArray m_renderer = createRendererSM();

    private SettingsModelColor m_boundingBoxColor = createColorSM();

    private SettingsModelBoolean m_showBoundingBoxNames = createShowBoundingBoxNameSM();

    @Override
    protected void addSettingsModels(final List<SettingsModel> settingsModels) {
        settingsModels.add(m_transparency);
        settingsModels.add(m_dimensionsPlane);
        settingsModels.add(m_renderer);
        settingsModels.add(m_boundingBoxColor);
        settingsModels.add(m_showBoundingBoxNames);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws KNIPException
     * 
     * @throws IllegalArgumentException
     */
    @Override
    protected ListCell compute(final ImgPlusValue<T> imgValue, final LabelingValue<L> labelingValue)
            throws IOException, KNIPException {
        final Labeling<L> lab = labelingValue.getLabeling();
        final ImgPlus<T> imgPlus = imgValue.getImgPlus();

        //render with image first check dimensionality
        if (imgPlus.numDimensions() == lab.numDimensions()) {
            for (int i = 0; i < imgPlus.numDimensions(); i++) {
                if (imgPlus.dimension(i) != lab.dimension(i)) {
                    throw new KNIPException(
                            "Incompatible dimension sizes: label dimension size != image dimension size for image axis "
                                    + imgPlus.axis(i).type().getLabel());
                }
            }
        } else {
            throw new KNIPException("Labeling and image are incompatible, different dimension count!");
        }

        if (imgPlus.firstElement().getClass() == DoubleType.class) {
            throw new KNIPException("double type is currently not supported please convert the images first");
        }

        // Order of dimensions always X before Y
        final Interval[] intervals = m_dimensionsPlane.getIntervals(imgPlus, imgPlus);
        final List<DataCell> cells = new ArrayList<DataCell>(0);

        int X = m_dimensionsPlane.getSelectedDimIndices(imgPlus)[0];
        int Y = m_dimensionsPlane.getSelectedDimIndices(imgPlus)[1];

        final long[] min = new long[imgPlus.numDimensions()];
        for (final Interval interval : intervals) {
            interval.min(min);

            //create partial images
            BufferedImage label =
                    createLabelImage(lab, labelingValue.getLabelingMetadata().getLabelingColorTable(), min, X, Y);
            BufferedImage grey = createGreyImage(imgPlus, min, X, Y);
            BufferedImage result = renderTogether(grey, label);

            //create final png output
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(result, "PNG", out);
            cells.add(new PNGImageContent(out.toByteArray()).toImageCell());
        }

        return CollectionCellFactory.createListCell(cells);
    }

    //
    //
    //helper methods for image rendering (see also the AWTImageProvider subclasses)
    //
    //

    private BufferedImage createGreyImage(final ImgPlus<T> img, final long[] min, final int X, final int Y) {
        Real2GreyRenderer<T> greyRenderer = new Real2GreyRenderer<T>();
        BufferedImage res = AWTImageTools.makeBuffered(greyRenderer.render(img, X, Y, min).image());
        return res;
    }

    private BufferedImage createLabelImage(final Labeling<L> lab, final LabelingColorTable table, final long[] min,
                                           final int X, final int Y) {
        ImageRenderer<LabelingType<L>> labRenderer;
        LabelingColorTable extendedTable =
                LabelingColorTableUtils.extendLabelingColorTable(table, new RandomMissingColorHandler());

        if (m_renderer.getStringArrayValue()[0].equals(RENDERER_NAMES[0])) {
            //RandomColorLabelingRenderer
            labRenderer = new ColorLabelingRenderer<L>();
            ((ColorLabelingRenderer)labRenderer).setLabelingColorTable(extendedTable);
        } else {
            if (m_renderer.getStringArrayValue()[0].equals(RENDERER_NAMES[1])) {
                //BoundingBoxLabelRenderer
                labRenderer = new BoundingBoxLabelRenderer<L>();

            } else {
                //BoundingBoxRandomColorLabelRenderer
                labRenderer = new BoundingBoxRandomColorLabelRenderer<L>();
                ((LabelingColorTableRenderer)labRenderer).setLabelingColorTable(extendedTable);
            }
            ((BoundingBoxLabelRenderer<L>)labRenderer).setBoxColor(m_boundingBoxColor.getColorValue());
            ((BoundingBoxLabelRenderer<L>)labRenderer).setRenderingWithLabelStrings(m_showBoundingBoxNames
                    .getBooleanValue());
        }

        ((RendererWithLabels<L>)labRenderer).setLabelMapping(lab.firstElement().getMapping());

        BufferedImage label = AWTImageTools.makeBuffered(labRenderer.render(lab, X, Y, min).image());
        return label;
    }

    private BufferedImage renderTogether(final BufferedImage img, final BufferedImage labeling) {
        GraphicsConfiguration config =
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        BufferedImage result =
                config.createCompatibleImage(img.getWidth(), img.getHeight(), java.awt.Transparency.TRANSLUCENT);
        Graphics g = result.getGraphics();

        g.drawImage(img, 0, 0, null);
        g.drawImage(Transparency.makeColorTransparent(labeling, Color.WHITE, m_transparency.getIntValue()), 0, 0, null);

        return result;
    }

    @Override
    protected DataType getOutDataCellListCellType() {
        return DataType.getType(PNGImageCell.class);
    }
}
