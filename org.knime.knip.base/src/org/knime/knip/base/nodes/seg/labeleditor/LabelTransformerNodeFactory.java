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
package org.knime.knip.base.nodes.seg.labeleditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.view.Views;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.util.StringTransformer;

/**
 * Label Transformer Node Model
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 *
 * @param <L>
 *
 */
public class LabelTransformerNodeFactory<L extends Comparable<L>> extends ValueToCellNodeFactory<LabelingValue<L>> {

    private static final String LABEL_VAR = "current_label";

    private static final String ROW_VAR = "rowstring";

    private static SettingsModelString createExpressionModel() {
        return new SettingsModelString("expression", "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<LabelingValue<L>> createNodeDialog() {
        return new ValueToCellNodeDialog<LabelingValue<L>>() {

            @Override
            public void addDialogComponents() {
                DialogComponent dc =
                        new DialogComponentStringTransformer(createExpressionModel(), true, 0, null, new String[]{
                                LABEL_VAR, ROW_VAR});
                addDialogComponent("Options", "", dc);

            }

        };
    }

    @Override
    public ValueToCellNodeModel<LabelingValue<L>, LabelingCell<String>> createNodeModel() {
        return new ValueToCellNodeModel<LabelingValue<L>, LabelingCell<String>>() {

            private String[] m_columnNames;

            private final SettingsModelString m_expressionModel = createExpressionModel();

            private LabelingCellFactory m_labCellFactory;

            private final HashMap<String, Object> m_objects = new HashMap<String, Object>();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_expressionModel);
            }

            @Override
            protected LabelingCell<String> compute(final LabelingValue<L> cellLabelingVal) throws Exception {

                final StringTransformer transformer = new StringTransformer(m_expressionModel.getStringValue(), "$");

                // The input labeling
                final RandomAccessibleInterval<LabelingType<L>> labeling = cellLabelingVal.getLabeling();
                final RandomAccessibleInterval<LabelingType<String>> res =
                        KNIPGateway.ops().create().imgLabeling(labeling);

                final Cursor<LabelingType<L>> inCursor = Views.iterable(labeling).cursor();
                final Cursor<LabelingType<String>> outCursor = Views.iterable(res).cursor();

                while (inCursor.hasNext()) {
                    inCursor.fwd();
                    outCursor.fwd();

                    final Set<L> inLabeling = inCursor.get();

                    if (inLabeling.isEmpty()) {
                        continue;
                    }

                    final ArrayList<String> labelList = new ArrayList<String>(inLabeling.size());

                    for (final L label : inLabeling) {
                        m_objects.put(LABEL_VAR, label);
                        labelList.add(transformer.transform(m_objects));
                    }

                    outCursor.get().clear();
                    outCursor.get().addAll(labelList);
                }

                final LabelingCell<String> lab =
                        m_labCellFactory.createCell(res, cellLabelingVal.getLabelingMetadata());
                return lab;

            }

            @Override
            protected void computeDataRow(final DataRow row) {
                m_objects.clear();
                final Iterator<DataCell> rowIt = row.iterator();

                int i = 0;
                while (rowIt.hasNext()) {
                    m_objects.put(m_columnNames[i++], rowIt.next().toString());
                }

                m_objects.put(ROW_VAR, row.getKey().toString());
            }

            @Override
            protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
                m_columnNames = ((DataTableSpec)inSpecs[0]).getColumnNames();
                return super.configure(inSpecs);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_labCellFactory = new LabelingCellFactory(exec);
            }

        };

    }
}
