package org.knime.knip.core;

import org.scijava.service.Service;

public interface MemoryService extends Service {

	void register(final MemoryAlertable alertable);

    /**
     * @return
     */
         long limit();

}
