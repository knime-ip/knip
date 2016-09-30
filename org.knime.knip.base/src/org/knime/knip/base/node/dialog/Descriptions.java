/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2016
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
 * Created on Sep 30, 2016 by dietzc
 */
package org.knime.knip.base.node.dialog;

import org.knime.node.v210.ADocument.A;
import org.knime.node.v210.OptionDocument.Option;
import org.knime.node.v210.PDocument.P;

/**
 * Class maintaining reusable descriptions of dialog components.
 *
 * @author Christian Dietz, UniKN
 */
public class Descriptions {
    /**
     * @param opt Option pane to add dimension selection description.
     *
     */
    public static void createNodeDescriptionDimSelection(final Option opt) {
        opt.setName("Dimension Selection");
        opt.addNewP().newCursor().setTextValue("This component allows the selection of dimensions of interest."
                + System.getProperty("line.separator")
                + "If an algorithm cannot, as it only supports fewer dimensions than the number of dimensions of the image, or shouldnot, as one wants to run the algorithm only on subsets of the image, be applied on the complete image, the dimension selection can be used to define the plane/cube/hypercube on which the algorithm is applied."
                + System.getProperty("line.separator")
                + "Example 1 with three dimensional Image (X,Y,Time): An algorithm cannot be applied to the complete image, as it only supports two dimensional images. If you select e.g. X,Y then the algorithm will be applied to all X,Y planes individually."
                + System.getProperty("line.separator")
                + "Example 2 with three dimensional Image (X,Y,Time): The algorithm can be applied in two, three or even more dimensions. Select the dimensions to define your plane/cube/hypercube on which the algorithm will be applied.");

    }

    /**
     * @param opt Option pane to add dimension selection description.
     * @deprecated Consider using {@link org.knime.node.v210.OptionDocument.Option}
     */
    @Deprecated
    public static void createNodeDescriptionDimSelection(final org.knime.node2012.OptionDocument.Option opt) {
        opt.setName("Dimension Selection");
        opt.addNewP().newCursor().setTextValue("This component allows the selection of dimensions of interest."
                + System.getProperty("line.separator")
                + "If an algorithm cannot, as it only supports fewer dimensions than the number of dimensions of the image, or shouldnot, as one wants to run the algorithm only on subsets of the image, be applied on the complete image, the dimension selection can be used to define the plane/cube/hypercube on which the algorithm is applied."
                + System.getProperty("line.separator")
                + "Example 1 with three dimensional Image (X,Y,Time): An algorithm cannot be applied to the complete image, as it only supports two dimensional images. If you select e.g. X,Y then the algorithm will be applied to all X,Y planes individually."
                + System.getProperty("line.separator")
                + "Example 2 with three dimensional Image (X,Y,Time): The algorithm can be applied in two, three or even more dimensions. Select the dimensions to define your plane/cube/hypercube on which the algorithm will be applied.");

    }

    @SuppressWarnings("javadoc")
    public static void createNodeDescriptionColumnCreation(final Option opt) {
        opt.setName("Column Creation Mode");
        opt.addNewP().newCursor()
                .setTextValue("Mode how to handle the selected column. The processed column can be added to a new table, appended to the end of the table, or the old column can be replaced by the new result");

    }

    @SuppressWarnings("javadoc")
    public static void createNodeDescriptionColumnSuffix(final Option opt) {
        opt.setName("Column suffix");
        opt.newCursor()
                .setTextValue("A suffix appended to the column name. If \"Append\" is not selected, it can be left empty.");
    }

    @SuppressWarnings("javadoc")
    public static void createNodeDescriptionFactorySelection(final Option opt) {
        opt.setName("Factory Selection");
        P para = opt.addNewP();

        //add node description
        para.newCursor()
                .setTextValue("The Factory is used to create images and determines how the data of an image is stored."
                        + System.getProperty("line.separator")
                        + "An 'ArrayImgFactory' for example stores the data in a single java array which results in optimal performance, but limits the amount of possible pixels to 2^31-1 (~2 billion pixels)."
                        + System.getProperty("line.separator")
                        + "For bigger images a 'CellImgFactory' can be used. The type of the selected Factory may have an impact on the runtime of algorithms.");
    }

    @SuppressWarnings("javadoc")
    public static void createNodeDescriptionOutOfBounds(final Option opt) {

        opt.setName("Out of Bounds Selection");
        P para = opt.addNewP();

        //add node description
        para.newCursor()
                .setTextValue("The 'OutOfBounds Strategy' is used when an algorithm needs access to pixels which lie outside of an image (for example convolutions)."
                        + System.getProperty("line.separator")
                        + "The strategy determines how an image is extended, for examples see ");

        // add new <a> element
        A a = para.addNewA();
        // add attribute href
        a.addNewHref().setStringValue("http://fiji.sc/ImgLib2_Examples#Example_5_-_Out_of_bounds");
        // set text
        a.newCursor().setTextValue("here");
    }

    /**
     * @param opt
     */
    public static void createNodeDescriptionPixelType(final Option opt) {
        opt.setName("PixelType Selection");
        P para = opt.addNewP();

        //add node description
        para.newCursor()
                .setTextValue("The 'PixelType' determines how much information can be stored in a single pixel. "
                        + "For monochromatic images the BitType suffices (0 or 1 for black and white), while for a greyscale image at least the UnsignedByteType must be used. "
                        + "The selected PixelType affects the memory consumption.");
    }

    @SuppressWarnings("javadoc")
    public static void createNodeDescriptionRadiusSelection(final Option opt) {
        opt.setName("Radius");
        P para = opt.addNewP();

        //add node description
        para.newCursor().setTextValue("The size of the radius which will be used by the algorithm.");
    }

    @SuppressWarnings("javadoc")
    public static void createNodeDescriptionSpan(final Option opt) {
        opt.setName("Window Span");
        P para = opt.addNewP();

        //add node description
        para.newCursor().setTextValue("The size of the span which will be used by the algorithm.");
    }
}
