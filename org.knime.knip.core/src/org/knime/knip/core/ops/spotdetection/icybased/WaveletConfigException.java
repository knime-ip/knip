/*
 * Copyright 2010, 2011 Institut Pasteur.
 * 
 * This file is part of ICY.
 * 
 * ICY is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ICY is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ICY. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * copied by
 * Michael Zinsmaier, University of Konstanz
 * 
 * We use this class together with B3SplineUDWT because it is needed internally.
 * 
 */
package org.knime.knip.core.ops.spotdetection.icybased;

public class WaveletConfigException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 7526927005039578733L;

    public WaveletConfigException(final String message) {
        super(message);
    }

}
