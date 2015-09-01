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
package org.knime.knip.io.nodes.imgreader2.readfromdialog;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.io.ScifioGateway;
import org.knime.knip.io.node.dialog.DialogComponentMultiFileChooser;
import org.knime.knip.io.nodes.imgreader2.AbstractImgReaderNodeDialog;

/**
 * Dialog for the ImageReader to select the files and choose some additional
 * options.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 * @author <a href="mailto:gabriel.einsdorf@uni.kn"> Gabriel Einsdorf</a>
 * @author Daniel Seebacher, University of Konstanz.
 */
public class ImgReaderWithoutInputNodeDialog extends AbstractImgReaderNodeDialog {

	/**
	 * Key to store the directory history.
	 */
	public static final String CFG_DIR_HISTORY = "imagereader_dirhistory";

	private static final FileFilter FILEFILTER;

	static {
		FILEFILTER = new FileNameExtensionFilter("BioFormats files", ScifioGateway.getAllSuffixes());
	}

	private final DialogComponentMultiFileChooser m_filechooser;

	ImgReaderWithoutInputNodeDialog() {
		createNewGroup("");
		m_filechooser = new DialogComponentMultiFileChooser(ImgReaderWithoutInputNodeModel.createFileListModel(),
				FILEFILTER, CFG_DIR_HISTORY);
		addDialogComponent(m_filechooser);
		closeCurrentGroup();

		createNewTab("Additional Options");

		super.buildRemainingGUI();
		createNewGroup("Keys");
		addDialogComponent(new DialogComponentBoolean(ImgReaderWithoutInputNodeModel.createCompletePathRowKeyModel(),
				"Use complete file path as row key"));
		closeCurrentGroup();

	}

	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
			throws NotConfigurableException {
		super.loadAdditionalSettingsFrom(settings, specs);
	}

	@Override
	public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		super.saveAdditionalSettingsTo(settings);
	}
}
