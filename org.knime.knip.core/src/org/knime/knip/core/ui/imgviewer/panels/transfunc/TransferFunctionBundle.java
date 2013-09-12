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
package org.knime.knip.core.ui.imgviewer.panels.transfunc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.imglib2.util.ValuePair;

/**
 * This class bundles a group of Transfer functions together.
 * 
 * It uses a number of maps to connect info between a String, a Color and a TransferFunction. Internaly it uses a
 * {@link LinkedHashMap}, so it guarantees an ordering of the keys in form of the String.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class TransferFunctionBundle implements Iterable<TransferFunction> {

    public enum Type {
        RGBA("RGBA"), RGB("RGB"), GREYA("GreyA"), GREY("Grey");

        private final String m_name;

        private Type(final String name) {
            m_name = name;
        }

        @Override
        public String toString() {
            return m_name;
        }
    }

    /**
     * Returns a new Bundle with four Transferfunction for alpha, red, green and blue.
     * 
     * @return the new bundle
     */
    public static TransferFunctionBundle newRGBABundle() {
        final TransferFunctionColor[] keys =
                {TransferFunctionColor.ALPHA, TransferFunctionColor.RED, TransferFunctionColor.GREEN,
                        TransferFunctionColor.BLUE};

        return moveAlpha(setUpBundle(keys, Type.RGBA), TransferFunctionColor.ALPHA);
    }

    /**
     * Returns a new Bundle with four Transferfunction for red, green and blue.
     * 
     * @return the new bundle
     */
    public static TransferFunctionBundle newRGBBundle() {
        final TransferFunctionColor[] keys =
                {TransferFunctionColor.RED, TransferFunctionColor.GREEN, TransferFunctionColor.BLUE};
        return setUpBundle(keys, Type.RGB);
    }

    /**
     * Returns a new Bundle with two Transferfunctions for gray.
     * 
     * @return the new bundle
     */
    public static TransferFunctionBundle newGBundle() {
        final TransferFunctionColor[] keys = {TransferFunctionColor.GREY};
        return setUpBundle(keys, Type.GREY);
    }

    /**
     * Returns a new Bundle with two Transferfunctions for alpha and gray.
     * 
     * @return the new bundle
     */
    public static TransferFunctionBundle newGABundle() {
        final TransferFunctionColor[] keys = {TransferFunctionColor.ALPHA, TransferFunctionColor.GREY};

        return moveAlpha(setUpBundle(keys, Type.GREYA), TransferFunctionColor.ALPHA);
    }

    /**
     * Move the second point of the alpha function.
     */
    private static TransferFunctionBundle moveAlpha(final TransferFunctionBundle bundle,
                                                    final TransferFunctionColor color) {
        assert (bundle.get(color).getClass() == PolylineTransferFunction.class);

        final PolylineTransferFunction func = (PolylineTransferFunction)bundle.get(color);
        final List<PolylineTransferFunction.Point> list = func.getPoints();
        func.movePoint(list.get(list.size() - 1), 1.0, 0.2);

        return bundle;
    }

    /**
     * Use this for static methods to return standard bundles.<br>
     * 
     * @param colors the keys
     * @return the set up bundle
     */
    private static TransferFunctionBundle setUpBundle(final TransferFunctionColor[] colors, final Type type) {
        final TransferFunctionBundle bundle = new TransferFunctionBundle(type);

        for (final TransferFunctionColor color : colors) {
            bundle.add(new PolylineTransferFunction(), color);
        }

        return bundle;
    }

    private final Map<TransferFunctionColor, TransferFunction> m_functions =
            new LinkedHashMap<TransferFunctionColor, TransferFunction>();

    private final Map<TransferFunction, TransferFunctionColor> m_names =
            new HashMap<TransferFunction, TransferFunctionColor>();

    private final Type m_type;

    /**
     * Set up a new TFBundle.
     */
    public TransferFunctionBundle(final Type type) {
        m_type = type;
    }

    /**
     * A copy constructor, creating a deep copy.
     * 
     * @param tfb the bundle to copy
     */
    public TransferFunctionBundle(final TransferFunctionBundle tfb) {

        // and copy all functions
        for (final TransferFunction old : tfb.getFunctions()) {
            final TransferFunction copy = old.copy();
            final TransferFunctionColor key = tfb.getColorOfFunction(old);

            m_functions.put(key, copy);
            m_names.put(copy, key);
        }

        m_type = tfb.m_type;
    }

    /**
     * Creates a deep copy of this instance.<br>
     * 
     * @return a copy of this bundle.
     */
    public TransferFunctionBundle copy() {
        return new TransferFunctionBundle(this);
    }

    /**
     * Add a bunch of new functions with the given names as keys and associate them with the given Colors in the order
     * of the Array.
     * 
     * @param list list of the keys and functions to add
     */
    public final void add(final List<ValuePair<TransferFunction, TransferFunctionColor>> list) {

        for (final ValuePair<TransferFunction, TransferFunctionColor> p : list) {
            add(p.a, p.b);
        }
    }

    /**
     * Add a function with the given color as key.
     * 
     * @param func the function that should be added
     * @param color the key to register the new function with
     */
    public final void add(final TransferFunction func, final TransferFunctionColor color) {
        m_functions.put(color, func);
        m_names.put(func, color);
    }

    /**
     * Get the transfer function mapped to this key.
     * 
     * @param color the key
     * @return the associated transfer function
     */
    public final TransferFunction get(final TransferFunctionColor color) throws IllegalArgumentException {
        final TransferFunction func = m_functions.get(color);

        if (func != null) {
            return func;
        } else {
            throw new IllegalArgumentException("No such function with this name: " + color.toString());
        }
    }

    /**
     * Move the function with this keys to the last position in the Map.
     * 
     * @param color the desired key
     * 
     * @throws IllegalArgumentException if no function is registered under this name
     */
    public final void moveToLast(final TransferFunctionColor color) throws IllegalArgumentException {

        if (m_functions.containsKey(color)) {
            final TransferFunction temp = m_functions.get(color);
            m_functions.remove(color);
            m_functions.put(color, temp);
        } else {
            throw new IllegalArgumentException("No such function with this name: " + color.toString());
        }
    }

    @Override
    public final String toString() {
        return m_type.toString();
    }

    public final Type getType() {
        return m_type;
    }

    /**
     * Get the Set of keys used.
     * 
     * As internally a {@link LinkedHashMap} is used, the order of this set is guranteed to be always the same.
     * 
     * @return the set of keys
     */
    public final Set<TransferFunctionColor> getKeys() {
        return m_functions.keySet();
    }

    /**
     * Get collection of all transfer functions.
     * 
     * @return all transfer functions
     */
    public final Collection<TransferFunction> getFunctions() {
        return m_functions.values();
    }

    /**
     * Get the color that is used as key for a transfer function.
     * 
     * @param func the transfer function
     * @return the key that belongs to the function
     * 
     * @throws IllegalArgumentException if no function is registered under this name
     */
    public final TransferFunctionColor getColorOfFunction(final TransferFunction func) throws IllegalArgumentException {

        final TransferFunctionColor name = m_names.get(func);
        if (name != null) {
            return name;
        } else {
            throw new IllegalArgumentException("This function is not one of the functions in this bundle");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Iterable#iterator()
     */
    @Override
    public Iterator<TransferFunction> iterator() {
        return m_functions.values().iterator();
    }
}
