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
package org.knime.knip.io.nodes.imgimporter;

import java.util.List;

import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentSubsetSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection;
import org.knime.knip.core.types.ImgFactoryTypes;
import org.knime.knip.core.util.EnumListProvider;
import org.knime.knip.io.ImgRefValue;
import org.knime.knip.io.ImgSource;
import org.knime.knip.io.ImgSourcePool;

/**
 * The Factory class for the Image Reader.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class ImgImporterNodeFactory<T extends RealType<T> & NativeType<T>>
		extends ValueToCellNodeFactory<ImgRefValue> {

	private static SettingsModelString createFactorySelectionModel() {
		return new SettingsModelString("factory_selection",
				ImgFactoryTypes.ARRAY_IMG_FACTORY.name());
	}

	private static SettingsModelSubsetSelection createSubsetSelectionModel() {
		return new SettingsModelSubsetSelection("subsetselection");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ValueToCellNodeDialog<ImgRefValue> createNodeDialog() {
		return new ValueToCellNodeDialog<ImgRefValue>() {

			@Override
			public void addDialogComponents() {

				addDialogComponent("Options", "Options",
						new DialogComponentSubsetSelection(
								createSubsetSelectionModel(), true, true));

				addDialogComponent(
						"Options",
						"Factory selection",
						new DialogComponentStringSelection(
								createFactorySelectionModel(), "",
								EnumListProvider.getStringList(ImgFactoryTypes
										.values())));

			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ValueToCellNodeModel<ImgRefValue, ImgPlusCell<T>> createNodeModel() {
		return new ValueToCellNodeModel<ImgRefValue, ImgPlusCell<T>>() {

			private final SettingsModelString m_factorySelect = createFactorySelectionModel();

			private ImgPlusCellFactory m_imgCellFactory;

			private final SettingsModelSubsetSelection m_subsetSelect = createSubsetSelectionModel();

			@Override
			protected void addSettingsModels(
					final List<SettingsModel> settingsModels) {
				settingsModels.add(m_subsetSelect);
				settingsModels.add(m_factorySelect);
			}

			@SuppressWarnings("unchecked")
			@Override
			protected ImgPlusCell<T> compute(final ImgRefValue cellValue)
					throws Exception {
				final String ref = cellValue.getImageReference();
				final String source = cellValue.getSource();

				final ImgSource imgSource = ImgSourcePool.getImgSource(source);

				return m_imgCellFactory.createCell((ImgPlus<T>) imgSource
						.getImg(ref, 0));
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected void prepareExecute(final ExecutionContext exec) {
				m_imgCellFactory = new ImgPlusCellFactory(exec);
			}
		};
	}

}
