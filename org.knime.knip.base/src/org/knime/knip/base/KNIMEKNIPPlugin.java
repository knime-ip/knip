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
package org.knime.knip.base;

import java.awt.image.BufferedImage;

import net.imglib2.meta.Axes;
import net.imglib2.meta.DefaultTypedAxis;
import net.imglib2.meta.TypedAxis;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.knip.base.prefs.PreferenceConstants;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class KNIMEKNIPPlugin extends AbstractUIPlugin {

    // The shared instance.
    private static KNIMEKNIPPlugin plugin;

    /** The plugin ID. */
    public static final String PLUGIN_ID = "org.knime.ip";

    /**
     * @return true, if the files should be compressed
     */
    public static final boolean compressFiles() {
        return KNIMEKNIPPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.P_COMPRESS_FILES);
    }

    /**
     * @return the maximum number of {@link BufferedImage} in the cache (for the data table and the viewers)
     */
    public static final int getCacheSizeForBufferedImages() {
        return KNIMEKNIPPlugin.getDefault().getPreferenceStore()
                .getInt(PreferenceConstants.P_BUFFEREDIMAGES_CACHE_SIZE);
    }

    /**
     * Returns the shared instance.
     * 
     * @return Singleton instance of the Plugin
     */
    public static KNIMEKNIPPlugin getDefault() {
        return plugin;
    }

    /**
     * @return the maximum size of the files where the images are stored
     */
    public static final long getMaxFileSizeInByte() {
        return KNIMEKNIPPlugin.getDefault().getPreferenceStore().getInt(PreferenceConstants.P_MAX_FILE_SIZE) * 1024l * 1024l;
    }

    /**
     * @return the set maximum height for image data cells
     */
    public static final int getMaximumImageCellHeight() {
        return KNIMEKNIPPlugin.getDefault().getPreferenceStore().getInt(PreferenceConstants.P_IMAGE_CELL_HEIGHT);
    }

    /**
     * A user-defined ratio determining when to create a thumbnail for the rendering in the KNIME image table. If ratio
     * of the number of pixels of the thumbnail to be generated to the number of pixels of the actual image object is
     * below the specified value, the thumbnail will be generated and stored with the image data.
     * 
     * @return the ratio
     */
    public static final double getThumbnailImageRatio() {
        return KNIMEKNIPPlugin.getDefault().getPreferenceStore().getDouble(PreferenceConstants.P_THUMBNAIL_IMAGE_RATIO);
    }

    /**
     * @return parses and returns the dimension labels
     */
    public static final String[] parseDimensionLabels() {
        final IPreferenceStore store = KNIMEKNIPPlugin.getDefault().getPreferenceStore();
        return store.getString(PreferenceConstants.P_DIM_LABELS).split(",");
    }

    /**
     * Parses and returns the dimension labels
     * 
     * @return the labels as {@link CustomAxis}
     */
    public static final TypedAxis[] parseDimensionLabelsAsAxis() {
        final String[] labels = parseDimensionLabels();

        final TypedAxis[] axes = new TypedAxis[labels.length];
        for (int a = 0; a < axes.length; a++) {
            axes[a] = new DefaultTypedAxis(Axes.get(labels[a]));
        }

        return axes;
    }

    /**
     * The constructor.
     */
    public KNIMEKNIPPlugin() {
        plugin = this;
    }

}
