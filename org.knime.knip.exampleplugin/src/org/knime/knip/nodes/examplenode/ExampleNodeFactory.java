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
 *
 */
package org.knime.knip.nodes.examplenode;

import java.io.IOException;
import java.util.List;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.subset.views.ImgView;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.core.types.NativeTypes;
import org.knime.knip.core.util.EnumListProvider;
import org.knime.knip.core.util.ImgUtils;
import org.knime.knip.ops.ExampleOp;
import org.knime.knip.ops.PersistentConverter;

/**
 * Converts ImgPlusValue<T>s to ImgPlusCells<T>
 * 
 * @author dietzc
 */
public class ExampleNodeFactory<T extends RealType<T>, K extends IntegerType<K>>
		extends ValueToCellNodeFactory<ImgPlusValue<T>> {

	/*
	 * SettingsModels dienen als Kommunikationslayer zwischen Dialog und Model.
	 * Moment ist das definitiv nicht optimal gel�st, da KNIME vieles
	 * automatisieren k�nnte (Annotationen etc.), aber auch nicht weiter
	 * tragisch.
	 * 
	 * Wichtig ist, dass die SettingsModels im Dialog/Model verschiedene Objekte
	 * mit der gleichen (beliebigen) ID sind.
	 */

	static SettingsModelString createResTypeModel() {
		return new SettingsModelString("res_type",
				NativeTypes.SHORTTYPE.toString());
	}

	static SettingsModelIntegerBounded createMinInValueModel() {
		return new SettingsModelIntegerBounded("mininvalue", 0, 0,
				Integer.MAX_VALUE);
	}

	static SettingsModelIntegerBounded createMaxInValueModel() {
		return new SettingsModelIntegerBounded("maxinvalue", 1, 0,
				Integer.MAX_VALUE);
	}

	static SettingsModelBoolean createPersistModel() {
		return new SettingsModelBoolean("darktobright", false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<K>> createNodeModel() {
		/*
		 * Das NodeModel ist das Herzst�ck von einem Knoten. Hier wird bestimmt,
		 * was f�r ein Output (meistens Tabelle) erzeugt wird bzw. wie diese
		 * aufgebaut ist. In diesem Fall hier nutzen wir das
		 * ValueToCellNodeModel, d.h. es wird erwartet, dass irgendein Value
		 * eingeht und irgendeine Zelle erzeugt wird (jeweils spezifiziert �ber
		 * Generics). Dank dieser Annahme k�nnen wir uns einiges an
		 * Implementierungsarbeit sparen (Column selection im Dialog usw).
		 * 
		 * Sprich ValueToCellNodeModel und TwoValuesToCellNodeModel etc stammen
		 * von mir und martin. F�r generelle Implementierungen von NodeModel,
		 * NodeFactory und NodeDialog siehe
		 * http://tech.knime.org/developer-guide.
		 */
		return new ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<K>>() {

			/*
			 * LabelingCellFactory wird ben�tigt um eine passende LabelingCell
			 * zu erzeugen Jede Cell entspricht eben genau einer Zelle in einer
			 * Tabelle, welche von Knoten zu Knoten weiter gereicht wird.
			 * 
			 * Es gibt verschiedene Arten von Zellimplementierungen z.B.
			 * BlobCells und FileCells. Eine Zelle implementiert immer einen
			 * oder mehrere Values (daher auch ValueToCell) und k�mmert sich um
			 * ihre Serialisierung bzw. Deserialisierung.
			 * 
			 * KNIME triggert bei den BlobCells selbst wann eine
			 * deserialisierung notwendig ist. FileCells haben den Vorteil, dass
			 * man nicht in den generellen KNIME Stream schreibt, sondern eben
			 * in eine selbst angelegte (und von KNIME verwaltete) File.
			 * 
			 * Unsere ImgPlusCell bzw. LabelingCell sind beides FileCells (Gott
			 * sei Dank). ;))
			 */
			private ImgPlusCellFactory m_imgCellFactory;

			private SettingsModelString m_resType = createResTypeModel();

			private SettingsModelInteger m_minInValue = createMinInValueModel();

			private SettingsModelInteger m_maxInValue = createMaxInValueModel();

			private SettingsModelBoolean m_persist = createPersistModel();

			/*
			 * SettingsModels need to be validated/loaded/saved in the
			 * NodeModel... in our implementations we do it like this
			 */
			@Override
			protected void addSettingsModels(List<SettingsModel> settingsModels) {
				settingsModels.add(m_persist);
				settingsModels.add(m_resType);
				settingsModels.add(m_minInValue);
				settingsModels.add(m_maxInValue);
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected void prepareExecute(ExecutionContext exec) {
				m_imgCellFactory = new ImgPlusCellFactory(exec);
			}

			/**
			 * {@inheritDoc}
			 * 
			 * @throws IllegalArgumentException
			 */
			@SuppressWarnings("unchecked")
			// Execute method is called if you run a knime node
			// This compute method is called by the execute method from
			// ValueToCellNodeModel
			@Override
			protected ImgPlusCell<K> compute(ImgPlusValue<T> cellValue)
					throws IOException {

				ImgPlus<T> img = cellValue.getImgPlus();

				K typeInstance = (K) NativeTypes.valueOf(
						m_resType.getStringValue()).getTypeInstance();

				Converter<T, K> converter = new ExampleOp<T, K>(
						m_minInValue.getIntValue(), m_maxInValue.getIntValue(),
						(long) typeInstance.getMinValue(),
						(long) typeInstance.getMaxValue());

				if (m_persist.getBooleanValue()) {
					PersistentConverter<T, K> persistentConverter = new PersistentConverter<T, K>(
							converter);
					Img<K> output = (Img<K>) persistentConverter.compute(img,
							ImgUtils.createEmptyCopy(img, typeInstance));

					return m_imgCellFactory.createCell(output, img);
				} else {
					return m_imgCellFactory.createCell(
							new ImgView<K>(Converters.convert(
									(RandomAccessibleInterval<T>) img,
									converter, typeInstance),
									(ImgFactory<K>) img.factory()), img);
				}

			}

		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
		return new ValueToCellNodeDialog<ImgPlusValue<T>>() {

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void addDialogComponents() {

				// Add several dialog components
				addDialogComponent("Options", "", new DialogComponentNumber(
						createMinInValueModel(), "Min in Value", 0));

				addDialogComponent("Options", "", new DialogComponentNumber(
						createMaxInValueModel(), "Max in Value", 1));

				addDialogComponent("Expert Options", "",
						new DialogComponentBoolean(createPersistModel(),
								"Persist directly?"));

				addDialogComponent(
						"Options",
						"",
						new DialogComponentStringSelection(
								createResTypeModel(), "Result Type",
								EnumListProvider.getStringList(
										NativeTypes.SHORTTYPE,
										NativeTypes.BITTYPE,
										NativeTypes.BYTETYPE,
										NativeTypes.INTTYPE,
										NativeTypes.UNSIGNEDSHORTTYPE,
										NativeTypes.UNSIGNEDINTTYPE,
										NativeTypes.UNSIGNEDBYTETYPE)));

			}
		};
	}

}
