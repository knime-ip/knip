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
package org.knime.knip.io.nodes.annotation;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.core.types.ImgFactoryTypes;
import org.knime.knip.core.types.NativeTypes;
import org.knime.knip.core.util.EnumListProvider;

/**
 * Dialog for the Point Picker Node.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class AnnotatorNodeDialog<T extends RealType<T> & NativeType<T>, L extends Comparable<L>>
		extends DefaultNodeSettingsPane {

	private final DialogComponentAnnotator<T> m_dialogComponentAnnotator;

	public AnnotatorNodeDialog() {
		super();

		removeTab("Options");
		createNewTab("Selection");
		createNewGroup("Image Annotation");

		m_dialogComponentAnnotator = new DialogComponentAnnotator<T>(
				new SettingsModelAnnotator<String>(
						AnnotatorNodeModel.CFG_POINTS));
		addDialogComponent(m_dialogComponentAnnotator);
		closeCurrentGroup();

		createNewTab("Label Settings");
		setHorizontalPlacement(true);
		createNewGroup("Options");

		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				AnnotatorNodeModel.CFG_ADD_SEGMENT_ID, true),
				"Add unique segment id as label"));
		addDialogComponent(new DialogComponentStringSelection(
				new SettingsModelString(AnnotatorNodeModel.CFG_FACTORY_TYPE,
						ImgFactoryTypes.ARRAY_IMG_FACTORY.toString()),
				"Factory Type", EnumListProvider.getStringList(ImgFactoryTypes
						.values())));
		addDialogComponent(new DialogComponentStringSelection(
				new SettingsModelString(AnnotatorNodeModel.CFG_LABELING_TYPE,
						NativeTypes.SHORTTYPE.toString()), "Storage Img Type",
				EnumListProvider.getStringList(NativeTypes.intTypeValues())));

		closeCurrentGroup();
	}

	@Override
	public void onClose() {
		// can result in errors because the dialog is a ConfigureDialog
		// and will not be rebuild on reopening
		// m_dialogComponentAnnotator.close();
	}
}
