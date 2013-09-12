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
package org.knime.knip.base.nodes.misc.imggroupby;

import java.util.List;

import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.node.preproc.groupby.GroupByNodeModel;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.knip.base.data.aggregation.ImgComposeOperatorDeprecated;
import org.knime.knip.base.data.aggregation.LabelingComposeOperatorDeprecated;
import org.knime.knip.core.types.ImgFactoryTypes;
import org.knime.knip.core.types.NativeTypes;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
@Deprecated
public class ImgGroupByNodeModel extends GroupByNodeModel implements BufferedDataTableHolder {

    static SettingsModelString createImgTypeModel() {
        return new SettingsModelString("image_type", NativeTypes.FLOATTYPE.toString());
    }

    /* settings for the image compose operator */
    static SettingsModelString createIntervalColumnImgModel() {
        return new SettingsModelString("inveral_column_for_image_compostion", "");
    }

    /* settings for the labeling compose operator */
    static SettingsModelString createIntervalColumnLabModel() {
        return new SettingsModelString("interval_column_for_labeling_composition", "");
    }

    static SettingsModelString createLabelingFactoryModel() {
        return new SettingsModelString("labeling_factory", ImgFactoryTypes.ARRAY_IMG_FACTORY.toString());
    }

    static SettingsModelString createLabelingTypeModel() {
        return new SettingsModelString("labeling_type", NativeTypes.SHORTTYPE.toString());
    }

    static SettingsModelBoolean createUseImgNameAsLabelModel() {
        return new SettingsModelBoolean("use_img_name_as_label", false);
    }

    private final NodeLogger LOGGER = NodeLogger.getLogger(ImgGroupByNodeModel.class);

    /* data for the table cell viewer */
    private BufferedDataTable m_data;

    private final SettingsModelString m_imgType = createImgTypeModel();

    /* settings for the image compose operator */
    private final SettingsModelString m_intervalColImg = createIntervalColumnImgModel();

    /* settings for the labeling compose operator */
    private final SettingsModelString m_intervalColLab = createIntervalColumnLabModel();

    private final SettingsModelString m_labFactory = createLabelingFactoryModel();

    private final SettingsModelString m_labType = createLabelingTypeModel();

    private final SettingsModelBoolean m_useImgNameAslLabel = createUseImgNameAsLabelModel();

    /**
     * {@inheritDoc}
     */
    @Override
    protected GlobalSettings createGlobalSettings(final ExecutionContext exec, final BufferedDataTable table,
                                                  final List<String> groupByCols, final int maxUniqueVals) {
        final GlobalSettings globalSettings = super.createGlobalSettings(exec, table, groupByCols, maxUniqueVals);
        if (table.getDataTableSpec().findColumnIndex(m_intervalColLab.getStringValue()) != -1) {
            globalSettings.addValue(LabelingComposeOperatorDeprecated.GLOBAL_SETTINGS_KEY_INTERVAL_COLUMN,
                                    m_intervalColLab.getStringValue());
        }
        // setWarningMessage("Default values are possibly used for the labeling aggregation method.");

        globalSettings.addValue(LabelingComposeOperatorDeprecated.GLOBAL_SETTINGS_KEY_USE_IMG_NAME_AS_LABEL,
                                m_useImgNameAslLabel.getBooleanValue());

        if (table.getDataTableSpec().findColumnIndex(m_intervalColImg.getStringValue()) != -1) {
            globalSettings.addValue(ImgComposeOperatorDeprecated.GLOBAL_SETTINGS_KEY_INTERVAL_COLUMN,
                                    m_intervalColImg.getStringValue());
        }
        // setWarningMessage("Default values are possibly used for the image aggregation method.");

        globalSettings.addValue(ImgComposeOperatorDeprecated.GLOBAL_SETTINGS_KEY_OUTPUT_TYPE,
                                m_imgType.getStringValue());

        globalSettings.addValue(LabelingComposeOperatorDeprecated.GLOBAL_SETTINGS_KEY_RES_TYPE,
                                m_labType.getStringValue());

        globalSettings.addValue(LabelingComposeOperatorDeprecated.GLOBAL_SETTINGS_KEY_RES_FACTORY,
                                m_labFactory.getStringValue());

        return globalSettings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final PortObject[] result = super.execute(inData, exec);
        m_data = (BufferedDataTable)result[0];
        return result;
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
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_intervalColLab.loadSettingsFrom(settings);
        m_useImgNameAslLabel.loadSettingsFrom(settings);
        m_intervalColImg.loadSettingsFrom(settings);
        m_imgType.loadSettingsFrom(settings);
        try {
            m_labType.loadSettingsFrom(settings);
        } catch (final Exception e) {
            LOGGER.info("Problems occurred validating the settings " + m_labType.toString() + ": "
                    + e.getLocalizedMessage());
            // setWarningMessage("Problems occurred while validating settings.");
        }

        try {
            m_labFactory.loadSettingsFrom(settings);
        } catch (final Exception e) {
            LOGGER.info("Problems occurred validating the settings " + m_labFactory.toString() + ": "
                    + e.getLocalizedMessage());
            // setWarningMessage("Problems occurred while validating settings.");
        }

        super.loadValidatedSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_intervalColLab.saveSettingsTo(settings);
        m_useImgNameAslLabel.saveSettingsTo(settings);
        m_intervalColImg.saveSettingsTo(settings);
        m_imgType.saveSettingsTo(settings);
        m_labType.saveSettingsTo(settings);
        m_labFactory.saveSettingsTo(settings);
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_data = tables[0];

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

        m_intervalColLab.validateSettings(settings);
        m_useImgNameAslLabel.validateSettings(settings);
        m_intervalColImg.validateSettings(settings);
        m_imgType.validateSettings(settings);

        try {
            m_labType.validateSettings(settings);
        } catch (final InvalidSettingsException e) {

            LOGGER.info("Problems occurred validating the settings " + m_labType.toString() + ": "
                    + e.getLocalizedMessage());
            // setWarningMessage("Problems occurred while validating settings.");
        }

        try {
            m_labFactory.validateSettings(settings);
        } catch (final InvalidSettingsException e) {

            LOGGER.info("Problems occurred validating the settings " + m_labFactory.toString() + ": "
                    + e.getLocalizedMessage());
            // setWarningMessage("Problems occurred while validating settings.");
        }

        super.validateSettings(settings);
    }

}
