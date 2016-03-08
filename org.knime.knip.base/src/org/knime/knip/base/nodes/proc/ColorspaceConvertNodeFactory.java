/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2015
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
 * ---------------------------------------------------------------------
 *
 * Created on Oct 22, 2015 by hornm
 */
package org.knime.knip.base.nodes.proc;

import java.awt.Color;
import java.util.List;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.exceptions.KNIPRuntimeException;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.Descriptions;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.core.util.MinimaUtils;
import org.knime.node.v210.KnimeNodeDocument.KnimeNode;

import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * Node that converts between different color spaces (e.g. rgb to hsb)
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 *
 */
public class ColorspaceConvertNodeFactory<T extends RealType<T>> extends ValueToCellNodeFactory<ImgPlusValue<T>> {

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dim_selection", "Channel");
    }

    private static enum ColorspaceConverter {
        RGBtoHSB, HSBtoRGB, RGBtoLAB, LABtoRGB;
    }

    private static SettingsModelString createColorspaceConverterModel() {
        return new SettingsModelString("colorspace_converter", ColorspaceConverter.RGBtoHSB.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue<T>>() {

            @Override
            public void addDialogComponents() {
                addDialogComponent(new DialogComponentDimSelection(createDimSelectionModel(), "Dimension selection", 1,
                        1));
                addDialogComponent(new DialogComponentStringSelection(createColorspaceConverterModel(),
                        "Colorspace Converter", EnumUtils.getStringListFromToString(ColorspaceConverter.values())));

            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addNodeDescriptionContent(final KnimeNode node) {
        Descriptions
                .createNodeDescriptionDimSelection(node.getFullDescription().getTabList().get(0).addNewOption());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<FloatType>> createNodeModel() {
        return new ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<FloatType>>() {

            private final SettingsModelDimSelection m_dimSel = createDimSelectionModel();

            private final SettingsModelString m_colorspaceConverter = createColorspaceConverterModel();

            private ImgPlusCellFactory m_imgCellFac;

            private ColorspaceConverterI<T> m_converter;

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_dimSel);
                settingsModels.add(m_colorspaceConverter);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_imgCellFac = new ImgPlusCellFactory(exec);
                ColorspaceConverter conv =
                        EnumUtils.valueForName(m_colorspaceConverter.getStringValue(), ColorspaceConverter.values());
                switch (conv) {
                    case RGBtoHSB:
                        m_converter = new RGBtoHSB<T>();
                        break;
                    case HSBtoRGB:
                        m_converter = new HSBtoRGB<T>();
                        break;
                    case RGBtoLAB:
                        m_converter = new RGBtoLAB();
                        break;
                    case LABtoRGB:
                        m_converter = new LABtoRGB();
                }
            }

            @Override
            protected ImgPlusCell<FloatType> compute(final ImgPlusValue<T> cellValue) throws Exception {

                final ImgPlus<T> fromCell = cellValue.getImgPlus();
                final ImgPlus<T> zeroMinFromCell = MinimaUtils.getZeroMinImgPlus(fromCell);

                //create float image for convenience
                Img<FloatType> res = KNIPGateway.ops().create().img(zeroMinFromCell, new FloatType());
                int[] selDims = m_dimSel.getSelectedDimIndices(zeroMinFromCell);
                if (selDims.length == 0) {
                    throw new KNIPRuntimeException("Selected dimension not present in image.");
                }
                if (zeroMinFromCell.dimension(selDims[0]) != 3) {
                    throw new KNIPRuntimeException("Image must have exactly be of size 3 in the selected dimension!");
                }

                IterableInterval[] in = new IterableInterval[3];
                IterableInterval[] out = new IterableInterval[3];
                for (int i = 0; i < in.length; i++) {
                    in[i] = Views.flatIterable(Views.hyperSlice(zeroMinFromCell, selDims[0], i));
                    out[i] = Views.flatIterable(Views.hyperSlice(res, selDims[0], i));
                }
                m_converter.convert(in, out);
                return m_imgCellFac.createCell(MinimaUtils
                        .getTranslatedImgPlus(fromCell, new ImgPlus<FloatType>(res, zeroMinFromCell)));
            }
        };
    }

    private interface ColorspaceConverterI<T> {
        void convert(IterableInterval[] in, IterableInterval[] out);
    }

    private class RGBtoHSB<T extends RealType<T>> implements ColorspaceConverterI<T> {

        ImageJColorSpaceConverter m_conv = new ImageJColorSpaceConverter();

        /**
         * {@inheritDoc}
         */
        @Override
        public void convert(final IterableInterval[] in, final IterableInterval[] out) {
            Cursor[] inC = new Cursor[3];
            Cursor[] outC = new Cursor[3];
            for (int i = 0; i < inC.length; i++) {
                inC[i] = in[i].cursor();
                outC[i] = out[i].cursor();
            }
            while (inC[0].hasNext()) {
                for (int i = 0; i < outC.length; i++) {
                    inC[i].fwd();
                    outC[i].fwd();
                }
                int r = (int)((T)inC[0].get()).getRealDouble();
                int g = (int)((T)inC[1].get()).getRealDouble();
                int b = (int)((T)inC[2].get()).getRealDouble();
                double[] res = m_conv.RGBtoHSB(new int[]{r, g, b});
                for (int i = 0; i < res.length; i++) {
                    ((FloatType)outC[i].get()).setReal(res[i]);
                }
            }
        }

    }

    private class HSBtoRGB<T extends RealType<T>> implements ColorspaceConverterI<T> {

        ImageJColorSpaceConverter m_conv = new ImageJColorSpaceConverter();

        /**
         * {@inheritDoc}
         */
        @Override
        public void convert(final IterableInterval[] in, final IterableInterval[] out) {
            Cursor[] inC = new Cursor[3];
            Cursor[] outC = new Cursor[3];
            for (int i = 0; i < inC.length; i++) {
                inC[i] = in[i].cursor();
                outC[i] = out[i].cursor();
            }
            while (inC[0].hasNext()) {
                for (int i = 0; i < outC.length; i++) {
                    inC[i].fwd();
                    outC[i].fwd();
                }
                double h = ((T)inC[0].get()).getRealDouble();
                double s = ((T)inC[1].get()).getRealDouble();
                double b = ((T)inC[2].get()).getRealDouble();
                int[] res = m_conv.HSBtoRGB(h, s, b);
                for (int i = 0; i < res.length; i++) {
                    ((FloatType)outC[i].get()).setReal(res[i]);
                }
            }
        }

    }

    private class RGBtoLAB<T extends RealType<T>> implements ColorspaceConverterI<T> {

        ImageJColorSpaceConverter m_conv = new ImageJColorSpaceConverter();

        /**
         * {@inheritDoc}
         */
        @Override
        public void convert(final IterableInterval[] in, final IterableInterval[] out) {
            Cursor[] inC = new Cursor[3];
            Cursor[] outC = new Cursor[3];
            for (int i = 0; i < inC.length; i++) {
                inC[i] = in[i].cursor();
                outC[i] = out[i].cursor();
            }
            while (inC[0].hasNext()) {
                for (int i = 0; i < outC.length; i++) {
                    inC[i].fwd();
                    outC[i].fwd();
                }
                int r = (int)((T)inC[0].get()).getRealDouble();
                int g = (int)((T)inC[1].get()).getRealDouble();
                int b = (int)((T)inC[2].get()).getRealDouble();
                double[] res = m_conv.XYZtoLAB(m_conv.RGBtoXYZ(r, g, b));
                for (int i = 0; i < res.length; i++) {
                    ((FloatType)outC[i].get()).setReal(res[i]);
                }
            }
        }

    }

    private class LABtoRGB<T extends RealType<T>> implements ColorspaceConverterI<T> {

        ImageJColorSpaceConverter m_conv = new ImageJColorSpaceConverter();

        /**
         * {@inheritDoc}
         */
        @Override
        public void convert(final IterableInterval[] in, final IterableInterval[] out) {
            Cursor[] inC = new Cursor[3];
            Cursor[] outC = new Cursor[3];
            for (int i = 0; i < inC.length; i++) {
                inC[i] = in[i].cursor();
                outC[i] = out[i].cursor();
            }
            while (inC[0].hasNext()) {
                for (int i = 0; i < outC.length; i++) {
                    inC[i].fwd();
                    outC[i].fwd();
                }
                double L = ((T)inC[0].get()).getRealDouble();
                double a = ((T)inC[1].get()).getRealDouble();
                double b = ((T)inC[2].get()).getRealDouble();
                int[] res = m_conv.LABtoRGB(L, a, b);
                for (int i = 0; i < res.length; i++) {
                    ((FloatType)outC[i].get()).setReal(res[i]);
                }
            }
        }

    }

    /**
     * COPIED FROM IMAGEJ!!!
     *
     * ColorSpaceConverter
     *
     * @author dvs, hlp Created Jan 15, 2004 Version 3 posted on ImageJ Mar 12, 2006 by Duane Schwartzwald
     *         vonschwartzwalder at mac.com Version 4 created Feb. 27, 2007 by Harry Parker, harrylparker at yahoo dot
     *         com, corrects RGB to XYZ (and LAB) conversion.
     */
    private class ImageJColorSpaceConverter {

        /**
         * reference white in XYZ coordinates
         */
        public double[] D50 = {96.4212, 100.0, 82.5188};

        public double[] D55 = {95.6797, 100.0, 92.1481};

        public double[] D65 = {95.0429, 100.0, 108.8900};

        public double[] D75 = {94.9722, 100.0, 122.6394};

        public double[] whitePoint = D65;

        /**
         * reference white in xyY coordinates
         */
        public double[] chromaD50 = {0.3457, 0.3585, 100.0};

        public double[] chromaD55 = {0.3324, 0.3474, 100.0};

        public double[] chromaD65 = {0.3127, 0.3290, 100.0};

        public double[] chromaD75 = {0.2990, 0.3149, 100.0};

        public double[] chromaWhitePoint = chromaD65;

        /**
         * sRGB to XYZ conversion matrix
         */
        public double[][] M = {{0.4124, 0.3576, 0.1805}, {0.2126, 0.7152, 0.0722}, {0.0193, 0.1192, 0.9505}};

        /**
         * XYZ to sRGB conversion matrix
         */
        public double[][] Mi = {{3.2406, -1.5372, -0.4986}, {-0.9689, 1.8758, 0.0415}, {0.0557, -0.2040, 1.0570}};

        /**
         * Default constructor; uses D65 for the white point
         */
        public ImageJColorSpaceConverter() {
            whitePoint = D65;
            chromaWhitePoint = chromaD65;
        }

        /**
         * Constructor for setting a non-default white point
         *
         * @param white "d50", "d55", "d65" or "d75"
         */
        public ImageJColorSpaceConverter(final String white) {
            whitePoint = D65;
            chromaWhitePoint = chromaD65;
            if (white.equalsIgnoreCase("d50")) {
                whitePoint = D50;
                chromaWhitePoint = chromaD50;
            } else if (white.equalsIgnoreCase("d55")) {
                whitePoint = D55;
                chromaWhitePoint = chromaD55;
            } else if (white.equalsIgnoreCase("d65")) {
                whitePoint = D65;
                chromaWhitePoint = chromaD65;
            } else if (white.equalsIgnoreCase("d75")) {
                whitePoint = D75;
                chromaWhitePoint = chromaD75;
            } else {
                throw new IllegalArgumentException("Invalid white point");
            }
        }

        /**
         * @param H Hue angle/360 (0..1)
         * @param S Saturation (0..1)
         * @param B Value (0..1)
         * @return RGB values
         */
        public int[] HSBtoRGB(final double H, final double S, final double B) {
            int[] result = new int[3];
            int rgb = Color.HSBtoRGB((float)H, (float)S, (float)B);
            result[0] = (rgb >> 16) & 0xff;
            result[1] = (rgb >> 8) & 0xff;
            result[2] = (rgb >> 0) & 0xff;
            return result;
        }

        public int[] HSBtoRGB(final double[] HSB) {
            return HSBtoRGB(HSB[0], HSB[1], HSB[2]);
        }

        /**
         * Convert LAB to RGB.
         *
         * @param L
         * @param a
         * @param b
         * @return RGB values
         */
        public int[] LABtoRGB(final double L, final double a, final double b) {
            return XYZtoRGB(LABtoXYZ(L, a, b));
        }

        /**
         * @param Lab
         * @return RGB values
         */
        public int[] LABtoRGB(final double[] Lab) {
            return XYZtoRGB(LABtoXYZ(Lab));
        }

        /**
         * Convert LAB to XYZ.
         *
         * @param L
         * @param a
         * @param b
         * @return XYZ values
         */
        public double[] LABtoXYZ(final double L, final double a, final double b) {
            double[] result = new double[3];

            double y = (L + 16.0) / 116.0;
            double y3 = Math.pow(y, 3.0);
            double x = (a / 500.0) + y;
            double x3 = Math.pow(x, 3.0);
            double z = y - (b / 200.0);
            double z3 = Math.pow(z, 3.0);

            if (y3 > 0.008856) {
                y = y3;
            } else {
                y = (y - (16.0 / 116.0)) / 7.787;
            }
            if (x3 > 0.008856) {
                x = x3;
            } else {
                x = (x - (16.0 / 116.0)) / 7.787;
            }
            if (z3 > 0.008856) {
                z = z3;
            } else {
                z = (z - (16.0 / 116.0)) / 7.787;
            }

            result[0] = x * whitePoint[0];
            result[1] = y * whitePoint[1];
            result[2] = z * whitePoint[2];

            return result;
        }

        /**
         * Convert LAB to XYZ.
         *
         * @param Lab
         * @return XYZ values
         */
        public double[] LABtoXYZ(final double[] Lab) {
            return LABtoXYZ(Lab[0], Lab[1], Lab[2]);
        }

        /**
         * @param R Red in range 0..255
         * @param G Green in range 0..255
         * @param B Blue in range 0..255
         * @return HSB values: H is 0..360 degrees / 360 (0..1), S is 0..1, B is 0..1
         */
        public double[] RGBtoHSB(final int R, final int G, final int B) {
            double[] result = new double[3];
            float[] hsb = new float[3];
            Color.RGBtoHSB(R, G, B, hsb);
            result[0] = hsb[0];
            result[1] = hsb[1];
            result[2] = hsb[2];
            return result;
        }

        public double[] RGBtoHSB(final int[] RGB) {
            return RGBtoHSB(RGB[0], RGB[1], RGB[2]);
        }

        /**
         * @param rgb RGB value
         * @return Lab values
         */
        public double[] RGBtoLAB(final int rgb) {
            int r = (rgb & 0xff0000) >> 16;
            int g = (rgb & 0xff00) >> 8;
            int b = rgb & 0xff;
            return XYZtoLAB(RGBtoXYZ(r, g, b));
        }

        /**
         * @param RGB
         * @return Lab values
         */
        public double[] RGBtoLAB(final int[] RGB) {
            return XYZtoLAB(RGBtoXYZ(RGB));
        }

        /**
         * Convert RGB to XYZ
         *
         * @param R
         * @param G
         * @param B
         * @return XYZ in double array.
         */
        public double[] RGBtoXYZ(final int R, final int G, final int B) {
            double[] result = new double[3];

            // convert 0..255 into 0..1
            double r = R / 255.0;
            double g = G / 255.0;
            double b = B / 255.0;

            // assume sRGB
            if (r <= 0.04045) {
                r = r / 12.92;
            } else {
                r = Math.pow(((r + 0.055) / 1.055), 2.4);
            }
            if (g <= 0.04045) {
                g = g / 12.92;
            } else {
                g = Math.pow(((g + 0.055) / 1.055), 2.4);
            }
            if (b <= 0.04045) {
                b = b / 12.92;
            } else {
                b = Math.pow(((b + 0.055) / 1.055), 2.4);
            }

            r *= 100.0;
            g *= 100.0;
            b *= 100.0;

            // [X Y Z] = [r g b][M]
            result[0] = (r * M[0][0]) + (g * M[0][1]) + (b * M[0][2]);
            result[1] = (r * M[1][0]) + (g * M[1][1]) + (b * M[1][2]);
            result[2] = (r * M[2][0]) + (g * M[2][1]) + (b * M[2][2]);

            return result;
        }

        /**
         * Convert RGB to XYZ
         *
         * @param RGB
         * @return XYZ in double array.
         */
        public double[] RGBtoXYZ(final int[] RGB) {
            return RGBtoXYZ(RGB[0], RGB[1], RGB[2]);
        }

        /**
         * @param x
         * @param y
         * @param Y
         * @return XYZ values
         */
        public double[] xyYtoXYZ(final double x, final double y, final double Y) {
            double[] result = new double[3];
            if (y == 0) {
                result[0] = 0;
                result[1] = 0;
                result[2] = 0;
            } else {
                result[0] = (x * Y) / y;
                result[1] = Y;
                result[2] = ((1 - x - y) * Y) / y;
            }
            return result;
        }

        /**
         * @param xyY
         * @return XYZ values
         */
        public double[] xyYtoXYZ(final double[] xyY) {
            return xyYtoXYZ(xyY[0], xyY[1], xyY[2]);
        }

        /**
         * Convert XYZ to LAB.
         *
         * @param X
         * @param Y
         * @param Z
         * @return Lab values
         */
        public double[] XYZtoLAB(final double X, final double Y, final double Z) {

            double x = X / whitePoint[0];
            double y = Y / whitePoint[1];
            double z = Z / whitePoint[2];

            if (x > 0.008856) {
                x = Math.pow(x, 1.0 / 3.0);
            } else {
                x = (7.787 * x) + (16.0 / 116.0);
            }
            if (y > 0.008856) {
                y = Math.pow(y, 1.0 / 3.0);
            } else {
                y = (7.787 * y) + (16.0 / 116.0);
            }
            if (z > 0.008856) {
                z = Math.pow(z, 1.0 / 3.0);
            } else {
                z = (7.787 * z) + (16.0 / 116.0);
            }

            double[] result = new double[3];

            result[0] = (116.0 * y) - 16.0;
            result[1] = 500.0 * (x - y);
            result[2] = 200.0 * (y - z);

            return result;
        }

        /**
         * Convert XYZ to LAB.
         *
         * @param XYZ
         * @return Lab values
         */
        public double[] XYZtoLAB(final double[] XYZ) {
            return XYZtoLAB(XYZ[0], XYZ[1], XYZ[2]);
        }

        /**
         * Convert XYZ to RGB.
         *
         * @param X
         * @param Y
         * @param Z
         * @return RGB in int array.
         */
        public int[] XYZtoRGB(final double X, final double Y, final double Z) {
            int[] result = new int[3];

            double x = X / 100.0;
            double y = Y / 100.0;
            double z = Z / 100.0;

            // [r g b] = [X Y Z][Mi]
            double r = (x * Mi[0][0]) + (y * Mi[0][1]) + (z * Mi[0][2]);
            double g = (x * Mi[1][0]) + (y * Mi[1][1]) + (z * Mi[1][2]);
            double b = (x * Mi[2][0]) + (y * Mi[2][1]) + (z * Mi[2][2]);

            // assume sRGB
            if (r > 0.0031308) {
                r = ((1.055 * Math.pow(r, 1.0 / 2.4)) - 0.055);
            } else {
                r = (r * 12.92);
            }
            if (g > 0.0031308) {
                g = ((1.055 * Math.pow(g, 1.0 / 2.4)) - 0.055);
            } else {
                g = (g * 12.92);
            }
            if (b > 0.0031308) {
                b = ((1.055 * Math.pow(b, 1.0 / 2.4)) - 0.055);
            } else {
                b = (b * 12.92);
            }

            r = (r < 0) ? 0 : r;
            g = (g < 0) ? 0 : g;
            b = (b < 0) ? 0 : b;

            // convert 0..1 into 0..255
            result[0] = (int)Math.round(r * 255);
            result[1] = (int)Math.round(g * 255);
            result[2] = (int)Math.round(b * 255);

            return result;
        }

        /**
         * Convert XYZ to RGB
         *
         * @param XYZ in a double array.
         * @return RGB in int array.
         */
        public int[] XYZtoRGB(final double[] XYZ) {
            return XYZtoRGB(XYZ[0], XYZ[1], XYZ[2]);
        }

        /**
         * @param X
         * @param Y
         * @param Z
         * @return xyY values
         */
        public double[] XYZtoxyY(final double X, final double Y, final double Z) {
            double[] result = new double[3];
            if ((X + Y + Z) == 0) {
                result[0] = chromaWhitePoint[0];
                result[1] = chromaWhitePoint[1];
                result[2] = chromaWhitePoint[2];
            } else {
                result[0] = X / (X + Y + Z);
                result[1] = Y / (X + Y + Z);
                result[2] = Y;
            }
            return result;
        }

        /**
         * @param XYZ
         * @return xyY values
         */
        public double[] XYZtoxyY(final double[] XYZ) {
            return XYZtoxyY(XYZ[0], XYZ[1], XYZ[2]);
        }

    }

}
