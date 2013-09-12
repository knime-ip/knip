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
package org.knime.knip.base.nodes.proc.binary;

import java.util.List;

import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.ops.operation.randomaccessible.unary.FillHoles;
import net.imglib2.ops.types.ConnectedType;
import net.imglib2.type.logic.BitType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.exceptions.ImageTypeNotCompatibleException;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeDialog;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeFactory;
import org.knime.knip.base.node.ImgPlusToImgPlusNodeModel;
import org.knime.knip.core.util.EnumListProvider;
import org.knime.knip.core.util.ImgPlusFactory;

/**
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public final class FillHolesNodeFactory extends ImgPlusToImgPlusNodeFactory<BitType, BitType> {

    private static SettingsModelString createTypeModel() {
        return new SettingsModelString("connection_type", ConnectedType.values()[0].toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImgPlusToImgPlusNodeDialog<BitType> createNodeDialog() {
        return new ImgPlusToImgPlusNodeDialog(2, Integer.MAX_VALUE, "X", "Y") {

            @Override
            public void addDialogComponents() {
                addDialogComponent("Options", "Settings", new DialogComponentStringSelection(createTypeModel(),
                        "Connection Type", EnumListProvider.getStringList(ConnectedType.values())));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImgPlusToImgPlusNodeModel<BitType, BitType> createNodeModel() {
        return new ImgPlusToImgPlusNodeModel<BitType, BitType>("X", "Y") {

            private final SettingsModelString m_conType = createTypeModel();

            private UnaryOperation<ImgPlus<BitType>, ImgPlus<BitType>> m_op;

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_conType);
            }

            @Override
            protected UnaryOutputOperation<ImgPlus<BitType>, ImgPlus<BitType>> op(final ImgPlus<BitType> imgPlus) {

                if (!(imgPlus.firstElement() instanceof BitType)) {
                    throw new ImageTypeNotCompatibleException("fill holes", imgPlus.firstElement(), BitType.class);
                }

                return Operations.wrap(m_op, new ImgPlusFactory<BitType, BitType>(new BitType()));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                if (m_conType.getStringValue().equals(ConnectedType.EIGHT_CONNECTED.name())) {
                    m_op = new FillHoles<ImgPlus<BitType>>(ConnectedType.EIGHT_CONNECTED);
                } else {
                    m_op = new FillHoles<ImgPlus<BitType>>(ConnectedType.FOUR_CONNECTED);
                }

                super.prepareExecute(exec);
            }

            @Override
            protected int getMinDimensions() {
                return 2;
            }
        };
    }

}
