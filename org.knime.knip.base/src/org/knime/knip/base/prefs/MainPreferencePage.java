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
package org.knime.knip.base.prefs;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.knip.base.KNIMEKNIPPlugin;

/**
 * This class represents a preference page that is contributed to the Preferences dialog. By subclassing
 * <samp>FieldEditorPreferencePage</samp>, we can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the preference store that belongs to the main
 * plug-in class. That way, preferences can be accessed directly via the preference store.
 */

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class MainPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private class DoubleFieldEditor extends StringFieldEditor {

        public DoubleFieldEditor(final String name, final String labelText, final Composite parent) {
            init(name, labelText);
            setTextLimit(10);
            setEmptyStringAllowed(false);
            setErrorMessage("Value between 0 and 1 required.");//$NON-NLS-1$
            createControl(parent);
        }

        @Override
        protected boolean checkState() {

            final Text text = getTextControl();

            if (text == null) {
                return false;
            }

            final String numberString = text.getText();
            try {
                final double number = Double.valueOf(numberString).doubleValue();
                if ((number >= 0) && (number <= 1)) {
                    clearErrorMessage();
                    return true;
                }

                showErrorMessage();
                return false;

            } catch (final NumberFormatException e1) {
                showErrorMessage();
            }

            return false;
        }

    }

    public MainPreferencePage() {
        super(GRID);
        setPreferenceStore(KNIMEKNIPPlugin.getDefault().getPreferenceStore());
        setDescription("Preferences for the KNIME Image Processing Plugin.");
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common GUI blocks needed to manipulate various
     * types of preferences. Each field editor knows how to save and restore itself.
     */
    @Override
    public void createFieldEditors() {
        final Composite parent = getFieldEditorParent();

        addField(new StringFieldEditor(PreferenceConstants.P_DIM_LABELS,
                "The available dimension labels (separated by ','):", getFieldEditorParent()));

        addField(new HorizontalLineField(parent));
        addField(new LabelField(parent, "The maximum image cell height \n"
                + "(0, if it should be determined by the first image in a column)."));
        addField(new IntegerFieldEditor(PreferenceConstants.P_IMAGE_CELL_HEIGHT, "Image cell height",
                getFieldEditorParent()));

        addField(new HorizontalLineField(parent));
        addField(new LabelField(parent, "The maximal number of rendered images\n" + "to be cached (requires restart)."));
        addField(new IntegerFieldEditor(PreferenceConstants.P_BUFFEREDIMAGES_CACHE_SIZE,
                "Max. number of cached images", getFieldEditorParent()));

        addField(new HorizontalLineField(parent));
        addField(new LabelField(parent, "The maximum file sizes in which the images are stored in MB.\n"
                + "If 0, each image is stored in a single file (which might result\n"
                + "in a higher overhead storing the files). Please note that if one\n"
                + "single image exceeds the file size, then the resulting file will be\n" + "larger than specified.\n"
                + "NOTE: It's highly recommended to set the file size to 0 otherwise,\n"
                + "for instance, sorting or writing of tables might not work so far."));
        addField(new StringFieldEditor(PreferenceConstants.P_MAX_FILE_SIZE, "The maximum file size in MB",
                getFieldEditorParent()));

        // addField(new LabelField(parent,
        // "If true, the files will be compressed (experimental!)"));
        // addField(new BooleanFieldEditor(
        // PreferenceConstants.P_COMPRESS_FILES,
        // "Compress files (zip)", getFieldEditorParent()));

        addField(new HorizontalLineField(parent));
        addField(new LabelField(parent, "A user-defined ratio determining when to create a thumbnail"
                + "\nfor the rendering in the KNIME image table. If the ratio of"
                + "\nthe number of pixels of the thumbnail to be generated to the"
                + "\nnumber of pixels of the actual image object is below the specified value,"
                + "\nthe thumbnail will be generated and stored with the image data."
                + "\nIf 0, the thumbnail will never be stored, if 1 everytime."));
        addField(new DoubleFieldEditor(PreferenceConstants.P_THUMBNAIL_IMAGE_RATIO, "The Thumbnail-Image Ratio",
                getFieldEditorParent()));

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench
     * )
     */
    @Override
    public void init(final IWorkbench workbench) {
        //
    }

}
