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
package org.knime.knip.base.nodes.filter.convolver;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.Axes;
import net.imglib2.meta.DefaultCalibratedAxis;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.BinaryObjectFactory;
import net.imglib2.ops.operation.BinaryOutputOperation;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.core.types.OutOfBoundsStrategyEnum;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class ImgPlusAddDimConvolverExt<T extends RealType<T>, K extends RealType<K>, O extends RealType<O> & NativeType<O>>
        implements MultiKernelImageConvolverExt<T, K, O> {

    private static final String AXIS_NAME_CONFIG = "additional_axis_name";

    private final List<SettingsModel> m_additionalSettingsModels;

    protected OutOfBoundsStrategyEnum m_outOfBounds;

    private O m_resType;

    private final SettingsModelString m_smAxisConfig;

    public ImgPlusAddDimConvolverExt() {
        m_additionalSettingsModels = new ArrayList<SettingsModel>();

        m_smAxisConfig = new SettingsModelString(AXIS_NAME_CONFIG + getName(), "Filter Dimension");

        m_additionalSettingsModels.add(m_smAxisConfig);

    }

    @Override
    public BinaryObjectFactory<ImgPlus<T>, Img<K>[], ImgPlus<O>> bufferFactory() {

        return new BinaryObjectFactory<ImgPlus<T>, Img<K>[], ImgPlus<O>>() {

            private Img<O> createOutput(final ImgPlus<T> inputA, final int length, final O resType) {
                ImgFactory<O> imgFactory;
                try {
                    imgFactory = inputA.factory().imgFactory(resType);
                } catch (final IncompatibleTypeException e) {
                    imgFactory = new ArrayImgFactory<O>();
                }
                final long[] dims = new long[inputA.numDimensions() + 1];
                for (int d = 0; d < inputA.numDimensions(); d++) {
                    dims[d] = inputA.dimension(d);
                }
                dims[dims.length - 1] = length;
                return imgFactory.create(dims, resType);
            }

            @Override
            public ImgPlus<O> instantiate(final ImgPlus<T> inputA, final Img<K>[] inputB) {
                final ImgPlus<O> res = new ImgPlus<O>(createOutput(inputA, inputB.length, m_resType));

                // Copy metadata
                for (int d = 0; d < inputA.numDimensions(); d++) {
                    res.setCalibration(inputA.calibration(d), d);
                    res.setAxis(inputA.axis(d), d);
                }

                res.setAxis(new DefaultCalibratedAxis(Axes.get(m_smAxisConfig.getStringValue())),
                            res.numDimensions() - 1);
                res.setCalibration(1.0, res.numDimensions() - 1);
                res.setName(inputA.getName());
                res.setSource(inputA.getSource());

                return res;
            }

        };
    }

    @Override
    public void close() {
        // Nothing to cleanup
    }

    @Override
    public BinaryOutputOperation<ImgPlus<T>, Img<K>[], ImgPlus<O>> copy() {
        throw new UnsupportedOperationException("Copy not supported");
    }

    @Override
    public List<SettingsModel> getAdditionalSettingsModels() {
        return m_additionalSettingsModels;
    }

    @Override
    public DialogComponent getDialogComponent() {
        return new DialogComponent(new SettingsModelString(AXIS_NAME_CONFIG + getName(), "Filter dimension")) {

            private JTextField m_field;

            {
                final JPanel finalPanel = new JPanel(new BorderLayout());
                m_field = new JTextField(m_smAxisConfig.getStringValue(), 50);
                finalPanel.add(m_field, BorderLayout.CENTER);

                getComponentPanel().add(finalPanel);

                getModel().addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(final ChangeEvent arg0) {
                        updateComponent();
                    }
                });
                updateComponent();
            }

            @Override
            protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
                // TODO Auto-generated method stub

            }

            @Override
            protected void setEnabledComponents(final boolean enabled) {
                m_field.setEnabled(enabled);
            }

            @Override
            public void setToolTipText(final String text) {
                m_field.setToolTipText(text);
            }

            @Override
            protected void updateComponent() {
                // only update component if values are off
                final SettingsModelString model = (SettingsModelString)getModel();
                setEnabledComponents(model.isEnabled());
                model.setStringValue(m_field.getText());
            }

            /**
             * Transfers the current value from the component into the model.
             * 
             * @throws InvalidSettingsException if the string was not accepted.
             */
            private void updateModel() throws InvalidSettingsException {
                if (m_field.getText().isEmpty()) {
                    throw new InvalidSettingsException("Please enter a string value in the label expression.");
                }

                // we transfer the value from the field into the
                // model
                ((SettingsModelString)getModel()).setStringValue(m_field.getText());
            }

            @Override
            protected void validateSettingsBeforeSave() throws InvalidSettingsException {
                updateModel();
            }
        };
    }

    @Override
    public void setOutOfBounds(final OutOfBoundsStrategyEnum outOfBounds) {
        m_outOfBounds = outOfBounds;
    };

    @Override
    public void setResultType(final O resType) {
        m_resType = resType;
    }

}
