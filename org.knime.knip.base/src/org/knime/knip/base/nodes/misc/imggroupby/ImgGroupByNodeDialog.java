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

import java.awt.FlowLayout;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.base.node.preproc.groupby.GroupByNodeDialog;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnFilter;
import org.knime.knip.base.data.IntervalValue;
import org.knime.knip.core.types.ImgFactoryTypes;
import org.knime.knip.core.types.NativeTypes;
import org.knime.knip.core.util.EnumListProvider;

/**
 * The node dialog of the image group by node
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
@Deprecated
public class ImgGroupByNodeDialog extends GroupByNodeDialog {

    private class DialogComponentUpdateableColumnNameSelection extends DialogComponentColumnNameSelection {

        /**
         * @param model
         * @param label
         * @param specIndex
         * @param isRequired
         * @param addNoneCol
         * @param columnFilter
         */
        public DialogComponentUpdateableColumnNameSelection(final SettingsModelString model, final String label,
                                                            final int specIndex, final boolean isRequired,
                                                            final boolean addNoneCol, final ColumnFilter columnFilter) {
            super(model, label, specIndex, isRequired, addNoneCol, columnFilter);
        }

        @Override
        public void updateComponent() {
            super.updateComponent();
        }

    }

    private class GroupByIncludeColumnsColumnFilter implements ColumnFilter {

        private List<String> m_columns;

        /**
         * {@inheritDoc}
         */
        @Override
        public String allFilteredMsg() {
            return "No column available for the interval selection.";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean includeColumn(final DataColumnSpec colSpec) {
            return m_columns.contains(colSpec.getName()) && colSpec.getType().isCompatible(IntervalValue.class);
        }

        public void setIncludeColumns(final List<String> columns) {
            m_columns = columns;

        }

    }

    private final GroupByIncludeColumnsColumnFilter m_columnFilter = new GroupByIncludeColumnsColumnFilter();

    private final DialogComponentStringSelection m_imgTypeComponent;

    private final DialogComponentUpdateableColumnNameSelection m_intervalColImgComponent;

    private final DialogComponentUpdateableColumnNameSelection m_intervalColLabComponent;

    private final DialogComponentStringSelection m_labelingFactory;

    private final DialogComponentStringSelection m_labelingType;

    private final DialogComponentBoolean m_useImgNameAsLabel;

    /** Constructor for class Pivot2NodeDialog. */
    public ImgGroupByNodeDialog() {
        super();
        /* components for the labeling compose operator */
        m_intervalColLabComponent =
                new DialogComponentUpdateableColumnNameSelection(ImgGroupByNodeModel.createIntervalColumnLabModel(),
                        "Interval (the size of the result labeling)", 0, false, true, m_columnFilter);

        m_useImgNameAsLabel =
                new DialogComponentBoolean(ImgGroupByNodeModel.createUseImgNameAsLabelModel(),
                        "Use bitmask name and source as label");

        m_labelingType =
                new DialogComponentStringSelection(ImgGroupByNodeModel.createLabelingTypeModel(),
                        "Result labeling type", EnumListProvider.getStringList(NativeTypes.SHORTTYPE,
                                                                               NativeTypes.BITTYPE,
                                                                               NativeTypes.BYTETYPE,
                                                                               NativeTypes.INTTYPE,
                                                                               NativeTypes.UNSIGNEDSHORTTYPE,
                                                                               NativeTypes.UNSIGNEDINTTYPE,
                                                                               NativeTypes.UNSIGNEDBYTETYPE));

        m_labelingFactory =
                new DialogComponentStringSelection(ImgGroupByNodeModel.createLabelingFactoryModel(),
                        "Result labeling factory", EnumListProvider.getStringList(ImgFactoryTypes.ARRAY_IMG_FACTORY,
                                                                                  ImgFactoryTypes.CELL_IMG_FACTORY,
                                                                                  ImgFactoryTypes.NTREE_IMG_FACTORY,
                                                                                  ImgFactoryTypes.PLANAR_IMG_FACTORY));

        // build the labeling option panel
        final JPanel labelingComposeOptionPanel = new JPanel();
        labelingComposeOptionPanel.setLayout(new BoxLayout(labelingComposeOptionPanel, BoxLayout.Y_AXIS));
        labelingComposeOptionPanel.add(m_intervalColLabComponent.getComponentPanel());
        labelingComposeOptionPanel.add(m_useImgNameAsLabel.getComponentPanel());
        labelingComposeOptionPanel.add(m_labelingType.getComponentPanel());
        labelingComposeOptionPanel.add(m_labelingFactory.getComponentPanel());

        addPanel(labelingComposeOptionPanel, "Labeling Compose Options");

        /* components for the image compose operator */
        m_intervalColImgComponent =
                new DialogComponentUpdateableColumnNameSelection(ImgGroupByNodeModel.createIntervalColumnImgModel(),
                        "Interval (the size of the result image)", 0, false, true, m_columnFilter);
        m_imgTypeComponent =
                new DialogComponentStringSelection(ImgGroupByNodeModel.createImgTypeModel(), "Result image type",
                        EnumListProvider.getStringList(NativeTypes.values()));

        final JPanel imgComposeOptionPanel = new JPanel(new FlowLayout());
        imgComposeOptionPanel.add(m_intervalColImgComponent.getComponentPanel());
        imgComposeOptionPanel.add(m_imgTypeComponent.getComponentPanel());
        addPanel(imgComposeOptionPanel, "Image Compose Options");
    }

    /** {@inheritDoc} */
    @Override
    protected void excludeColumns(final List<String> columns) {
        // exclude these column from the list of available column
        m_columnFilter.setIncludeColumns(columns);
        m_intervalColLabComponent.updateComponent();
        m_intervalColImgComponent.updateComponent();
        super.excludeColumns(columns);

    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_intervalColLabComponent.loadSettingsFrom(settings, specs);
        m_useImgNameAsLabel.loadSettingsFrom(settings, specs);
        m_intervalColImgComponent.loadSettingsFrom(settings, specs);
        m_imgTypeComponent.loadSettingsFrom(settings, specs);
        m_labelingType.loadSettingsFrom(settings, specs);
        m_labelingFactory.loadSettingsFrom(settings, specs);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveSettingsTo(settings);
        m_intervalColLabComponent.saveSettingsTo(settings);
        m_useImgNameAsLabel.saveSettingsTo(settings);
        m_intervalColImgComponent.saveSettingsTo(settings);
        m_imgTypeComponent.saveSettingsTo(settings);
        m_labelingFactory.saveSettingsTo(settings);
        m_labelingType.saveSettingsTo(settings);
    }

}
