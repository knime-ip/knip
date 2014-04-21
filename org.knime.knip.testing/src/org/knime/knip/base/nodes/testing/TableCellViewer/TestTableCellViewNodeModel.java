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
package org.knime.knip.base.nodes.testing.TableCellViewer;

/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003, 2010
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   29 Jan 2010 (hornm): created
 */
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.iterator.LocalizingIntervalIterator;
import net.imglib2.meta.Axes;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.axis.DefaultLinearAxis;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.nodes.view.TableCellViewNodeModel;
import org.knime.knip.core.types.ImgFactoryTypes;

/**
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Andreas Burger, University of Konstanz
 */
public class TestTableCellViewNodeModel extends NodeModel implements BufferedDataTableHolder {

    private ImgPlusCellFactory m_imgCellFactory;

    /**
     * A TableContentModel with custom chunk and cache sizes.
     *
     * @author hornm, University of Konstanz
     */
    class CustomTableContentModel extends TableContentModel {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public CustomTableContentModel(final int cacheSize, final int chunkSize) {
            super();
            setCacheSize(cacheSize);
            setChunkSize(chunkSize);
        }
    }

//    public static final String CFG_THUMBNAIL_SIZE = "thumbnail-size";

    private BufferedDataTable m_data;

    /*
     * object to save and restore the configuration of the
     * TableCellViewPanes
     */
    // private ModelContent m_config;

    private final SettingsModelInteger m_thumbnailSize = new SettingsModelInteger(
            TableCellViewNodeModel.CFG_THUMBNAIL_SIZE, 150);

    /**
     * @param nrInDataPorts
     * @param nrOutDataPorts
     */
    protected TestTableCellViewNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        assert (inData != null);
        assert (inData.length == 1);
        final DataTable in = inData[0];
        assert (in != null);
        // HiLiteHandler inProp = getInHiLiteHandler(INPORT);
        m_data = inData[0];
        // m_contModel.setHiLiteHandler(inProp);
        // assert (m_tableModel.hasData());

        m_imgCellFactory = new ImgPlusCellFactory(exec);

        // Open view and prepare it.
        @SuppressWarnings({"rawtypes", "unchecked"})
        TestTableCellViewNodeView view = new TestTableCellViewNodeView(this, 0, true);
        view.onOpen(); // Manual preparation

        // After the call of onOpen(), every test-view has run on every image.
        // Fetch the logger.
        List<HiddenImageLogger> imageFetchingComponents = view.getImageLogger();

        // Prepare output schema (One column, images only)
        final BufferedDataContainer con =
                exec.createDataContainer(new DataTableSpec(DataTableSpec
                        .createColumnSpecs(new String[]{"Images"}, new DataType[]{ImgPlusCell.TYPE})));
        int i = 0;

        // Convert every BufferedImage to png and then to ImgPlus and add it to the output container.
        for (HiddenImageLogger logger : imageFetchingComponents) {
            for (BufferedImage img : logger.getImages()) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(img, "png", os);
                os.flush();
                byte[] imageInByte = os.toByteArray();
                os.close();

                PNGImageContent pngCell = new PNGImageContent(imageInByte);
                ImgPlusCell<UnsignedByteType> imgPlusCell = compute(pngCell);
                con.addRowToTable(new DefaultRow(new RowKey("" + (i++)), imgPlusCell));

            }
        }

        con.close();

        BufferedDataTable[] res = new BufferedDataTable[]{con.getTable()};

        return res;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{m_data};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // try {
        // File f = new File(nodeInternDir, "internals");
        // m_config = (ModelContent) ModelContent
        // .loadFromXML(new FileInputStream(f));
        // return;
        // } catch (FileNotFoundException e) {
        // // Empty exception
        // }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
//        m_thumbnailSize.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_data = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // File f = new File(nodeInternDir, "internals");
        // m_config.saveToXML(new FileOutputStream(f));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
//        m_thumbnailSize.saveSettingsTo(settings);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        if (tables.length != 1) {
            throw new IllegalArgumentException();
        }
        m_data = tables[0];

        // HiLiteHandler inProp = getInHiLiteHandler(INPORT);
        // m_contModel.setHiLiteHandler(inProp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
//        m_thumbnailSize.validateSettings(settings);
    }

    /**
     * Converts png images to ImgPlusCells.
     */
    private ImgPlusCell<UnsignedByteType> compute(final PNGImageContent imageCell) throws IOException {

        final BufferedImage cellBufferedImage = (BufferedImage)imageCell.getImage();

        @SuppressWarnings("unchecked")
        final ImgFactory<UnsignedByteType> imgFactory =
                ImgFactoryTypes.getImgFactory(ImgFactoryTypes.ARRAY_IMG_FACTORY, null);

        final Img<UnsignedByteType> img =
                imgFactory.create(new long[]{cellBufferedImage.getWidth(null), cellBufferedImage.getHeight(null), 3},
                                  new UnsignedByteType());

        final LocalizingIntervalIterator iter =
                new LocalizingIntervalIterator(new long[]{cellBufferedImage.getWidth(), cellBufferedImage.getHeight()});

        final RandomAccess<UnsignedByteType> access = img.randomAccess();

        final int[] pos = new int[2];
        while (iter.hasNext()) {
            iter.fwd();
            iter.localize(pos);

            // Set position
            access.setPosition(pos[0], 0);
            access.setPosition(pos[1], 1);

            // read rgb
            final int pixelCol = cellBufferedImage.getRGB(pos[0], pos[1]);

            for (int c = 0; c < img.dimension(2); c++) {
                access.setPosition(c, 2);
                access.get().set(((pixelCol >>> ((img.dimension(2) - 1 - c) * 8)) & 0xff));
            }
        }

        final ImgPlus<UnsignedByteType> imgPlus = new ImgPlus<UnsignedByteType>(img);

        imgPlus.setAxis(new DefaultLinearAxis(Axes.get("X")), 0);
        imgPlus.setAxis(new DefaultLinearAxis(Axes.get("Y")), 1);
        imgPlus.setAxis(new DefaultLinearAxis(Axes.get("Channel")), 2);

        return m_imgCellFactory.createCell(imgPlus);
    }

}
