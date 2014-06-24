package org.knime.knip.scijava.services;

import org.knime.knip.scijava.adapters.ModuleAdapterFactory;
import org.scijava.module.ModuleInfo;
import org.scijava.service.Service;

public interface AdapterService extends Service {

	ModuleAdapterFactory getAdapter(final ModuleInfo module);

}
