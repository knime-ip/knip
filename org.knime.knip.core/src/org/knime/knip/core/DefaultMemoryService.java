package org.knime.knip.core;

import java.util.WeakHashMap;

import org.knime.core.data.util.memory.MemoryAlert;
import org.knime.core.data.util.memory.MemoryAlertListener;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

@Plugin(type = MemoryService.class)
public class DefaultMemoryService extends AbstractService implements MemoryService {

    @Parameter
    private LogService log;

    private WeakHashMap<MemoryAlertable, MemoryAlertable> registered;

    public DefaultMemoryService() {
        MemoryAlertSystem.getInstance().addListener(new MemoryAlertListener() {

            @Override
            protected boolean memoryAlert(final MemoryAlert alert) {
                log.info(getClass().getName() + " released memory for " + registered.size() + " objects");
                for (final MemoryAlertable alertable : registered.values()) {
                    alertable.memoryLow();
                }
                return false;
            }

        });
        registered = new WeakHashMap<MemoryAlertable, MemoryAlertable>();
    }

    @Override
    public void register(final MemoryAlertable alertable) {
        registered.put(alertable, alertable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long limit() {
        return MemoryAlertSystem.getMaximumMemory();
    }
}
