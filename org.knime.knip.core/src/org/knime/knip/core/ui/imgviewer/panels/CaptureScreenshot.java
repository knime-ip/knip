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
package org.knime.knip.core.ui.imgviewer.panels;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.events.AWTImageChgEvent;

/**
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author muethingc
 */
public class CaptureScreenshot extends ViewerComponent {

    private EventService m_eventService = new EventService();

    private Image m_currentImage;

    private final JTextField m_fieldName = new JTextField();

    private JButton m_captureButton = new JButton(new ImageIcon(getClass().getResource("CameraIcon.png")));

    private JButton m_dirButton = new JButton(new ImageIcon(getClass().getResource("FolderIcon.png")));

    private final JFileChooser m_chooser = new JFileChooser();

    /**
     *
     */
    public CaptureScreenshot() {
        super("Screenshot", false);

        setUpFileChooser();
        setUpTextFields();
        setUpButtons();

        final JSeparator separator = new JSeparator();

        final GroupLayout layout = new GroupLayout(this);

        final GroupLayout.SequentialGroup horizontal0 =
                layout.createSequentialGroup().addComponent(m_fieldName)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(m_dirButton);

        layout.setHorizontalGroup(layout.createParallelGroup().addGroup(horizontal0).addComponent(separator)
                .addComponent(m_captureButton));

        final GroupLayout.SequentialGroup vertical0 =
                layout.createSequentialGroup().addComponent(m_fieldName).addComponent(separator)
                        .addComponent(m_captureButton);

        layout.setVerticalGroup(layout.createParallelGroup().addGroup(vertical0).addComponent(m_dirButton));

        setLayout(layout);
    }

    private void setUpButtons() {
        m_captureButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (m_currentImage != null) {
                    try {
                        final RenderedImage ri = (RenderedImage)m_currentImage;
                        ImageIO.write(ri, "png", getFile());
                    } catch (final ClassCastException exception) {
                        System.err.println("Could not cast image to RenderedImage, not writing image");
                    } catch (final IOException exception) {
                        System.err.println("Could not write image");
                    }
                }
            }
        });

        m_dirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                //TODO: Handle return value?
                m_chooser.showOpenDialog(CaptureScreenshot.this);
            }
        });
    }

    private File getFile() {
        final String path = getFileName();
        File file = new File(path + ".png");

        int counter = 1;
        while (file.exists()) {
            file = new File(path + "_" + Integer.toString(counter++) + ".png");
        }

        return file;
    }

    private String getFileName() {
        return m_chooser.getSelectedFile() + System.getProperty("file.separator") + m_fieldName.getText();
    }

    private void setUpFileChooser() {
        m_chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        m_chooser.setDialogTitle("Choose a directory");
        m_chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        m_chooser.setAcceptAllFileFilterUsed(false);
    }

    private void setUpTextFields() {
        m_fieldName.setText("Image");
    }

    @EventListener
    public void onImageChg(final AWTImageChgEvent event) {
        m_currentImage = event.getImage();
    }

    @Override
    public void setEventService(final EventService eventService) {
        if (eventService == null) {
            m_eventService = new EventService();
        } else {
            m_eventService = eventService;
        }

        m_eventService.subscribe(this);
    }

    @Override
    public Position getPosition() {
        return Position.SOUTH;
    }

    @Override
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        // not used
    }

    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        // not used
    }

}
