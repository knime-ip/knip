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
 * ---------------------------------------------------------------------
 *
 * Created on 18.09.2013 by zinsmaie
 */
package org.knime.knip.core.ui.imgviewer.panels.providers;

import java.awt.Image;

import org.knime.knip.core.ui.event.EventService;

/**
 * Encapsulates a (parameter) state and allows to create an image from a given state. Parameters
 * (normalization / source /...) are retrieved by listening to the {@link EventService} and can change.
 * A {@link RenderUnit} listens to changes of all parameters that are important for the creation of its
 * {@link Image}, creates a {@link Image} if asked and provides a hashCode that summarizes its sate.<br>
 * <br>
 * The contract is that if the {@link #generateHashCode() hashCode} changes {@link #createImage()
 * createImage} will return a different image. <br><br>
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public interface RenderUnit {

    /**
     * Renders something (e.g. a labeling) into a {@link Image}. White will be treated as
     * transparent color during blending!
     * @return the created image.
     */
    public Image createImage();

    /**
     * Returns a hashCode that represents the complete state that leads to the image
     * created by {@link #createImage}. (I.e. if the hashCode is the same the resulting image is the same) <br>
     * <br>
     * the hashCode should be created like this:  super.hashCode + object1.hashCode * 31 + object2.hashCode * 31<br>
     * <br>
     * IMPORTANT: hashCode generation has to be fast! (the hashCode is used for caching decisions)
     *
     * @return hashCode that identifies the image created by {@link #createImage()}
     */
    public int generateHashCode();

    /**
     * @return false => createImage won't produce something useful given the current (parameter) state
     */
    public boolean isActive();

    /**
     * @param service registers at the provided service to listen to parameter changes.
     */
    public void setEventService(EventService service);

}
