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
package org.knime.knip.core.ui.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Manager to queue arbitrary objects to be propagated to the listeners.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventService.class);

    private final Map<Class<? extends KNIPEvent>, List<ProxyEventSubscriber<?>>> m_typeToSubscriber;

    private final PriorityBlockingQueue<KNIPEvent> m_eventQueue;

    private boolean m_openQueue = true;

    public EventService() {
        final Comparator<KNIPEvent> comparator = new KNIPEventComparator();
        m_eventQueue = new PriorityBlockingQueue<KNIPEvent>(20, comparator);
        m_typeToSubscriber = new HashMap<Class<? extends KNIPEvent>, List<ProxyEventSubscriber<?>>>();
    }

    /**
     * Subscribes all of the given object's @{@link EventListener} annotated methods. This allows a single class to
     * subscribe to multiple types of events by implementing multiple event handling methods and annotating each one
     * with @{@link EventListener}.
     */

    public void subscribe(final Object obj) {
        subscribeRecursively(obj.getClass(), obj);
    }

    /**
     * @param <E>
     * @param event
     */
    public <E extends KNIPEvent> void publish(final E event) {

        // test for redundant events i.e. events that have the
        // same effect than already inserted event. (E.g. ImgRedrawEvent
        // should only occur once)
        boolean redundant = false;
        for (final KNIPEvent eve : m_eventQueue) {
            if (event.isRedundant(eve)) {
                redundant = true;
                break;
            }
        }
        if (!redundant) {
            m_eventQueue.add(event);
        }

        if (m_openQueue) {
            m_openQueue = false;
            while (!m_eventQueue.isEmpty()) {
                processQueue();
            }

            m_openQueue = true;
        }
    }

    // -- Helper methods --

    /*
     * Recursively scans for @{@link EventListener} annotated methods, and
     * subscribes them to the event service.
     */
    private void subscribeRecursively(final Class<?> type, final Object o) {
        if ((type == null) || (type == Object.class)) {
            return;
        }
        for (final Method m : type.getDeclaredMethods()) {
            final EventListener ann = m.getAnnotation(EventListener.class);
            if (ann == null) {
                continue; // not an event handler method
            }

            final Class<? extends KNIPEvent> eventClass = getEventClass(m);
            if (eventClass == null) {
                LOGGER.warn("Invalid EventHandler method: " + m);
                continue;
            }

            subscribe(eventClass, o, m);
        }
        subscribeRecursively(type.getSuperclass(), o);
    }

    /* Gets the event class parameter of the given method. */
    private Class<? extends KNIPEvent> getEventClass(final Method m) {
        final Class<?>[] c = m.getParameterTypes();
        if ((c == null) || (c.length != 1)) {
            return null; // wrong number of args
        }
        if (!KNIPEvent.class.isAssignableFrom(c[0])) {
            return null; // wrong class
        }

        @SuppressWarnings("unchecked")
        final Class<? extends KNIPEvent> eventClass = (Class<? extends KNIPEvent>)c[0];
        return eventClass;
    }

    private <E extends KNIPEvent> void subscribe(final Class<E> c, final Object o, final Method m) {
        final ProxyEventSubscriber<E> subscriber = new ProxyEventSubscriber<E>(o, m);
        // Mapping Listeners to Topics
        List<ProxyEventSubscriber<?>> ref = m_typeToSubscriber.get(c);

        if (ref == null) {
            ref = new ArrayList<ProxyEventSubscriber<?>>();
            m_typeToSubscriber.put(c, ref);
        }

        ref.add(subscriber);

    }

    private void processQueue() {
        while (!m_eventQueue.isEmpty()) {
            // priority queue => higher priority events first
            final KNIPEvent event = m_eventQueue.poll();
            Class<?> clazz = event.getClass();

            while (KNIPEvent.class.isAssignableFrom(clazz)) {
                final List<ProxyEventSubscriber<?>> suscribers = m_typeToSubscriber.get(clazz);

                if (suscribers != null) {
                    for (final ProxyEventSubscriber l : suscribers) {
                        l.onEvent(event);
                    }
                    LOGGER.debug("Event " + event + " processed");
                } else {
                    LOGGER.debug("Nobody is listening to: " + event);
                }
                clazz = clazz.getSuperclass();
            }

            m_eventQueue.remove(event);
        }
    }

    private static class ProxyEventSubscriber<E extends KNIPEvent> {

        private final Method m_method;

        private final Object m_subscriber;

        public ProxyEventSubscriber(final Object subscriber, final Method method) {
            m_method = method;
            m_subscriber = subscriber;
        }

        public void onEvent(final E event) {

            try {
                m_method.invoke(m_subscriber, event);
            } catch (final Exception e) {
                throw new RuntimeException(
                        "InvocationTargetException when invoking annotated method from EventService publication. Eata: "
                                + event + ", subscriber:" + m_subscriber, e);
            }
        }
    }

    private class KNIPEventComparator implements Comparator<KNIPEvent> {
        @Override
        public int compare(final KNIPEvent a, final KNIPEvent b) {
            if (a.getExecutionOrder().ordinal() == b.getExecutionOrder().ordinal()) {
                return 0;
            } else if (a.getExecutionOrder().ordinal() < b.getExecutionOrder().ordinal()) {
                return -1;
            }
            return 1;
        }
    }

}
