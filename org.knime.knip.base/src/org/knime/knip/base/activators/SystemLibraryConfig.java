package org.knime.knip.base.activators;
/**
 * A configuration to be loaded by the activator.
 *
 * @author Christian Dietz
 */
public interface SystemLibraryConfig {

    /**
     * @param osname name of the operating system has to be checked
     * @return true, if some kind of rules matches (i.e. osName.contains("win"))
     */
    boolean matchesOSName(final String osname);

    /**
     * @return names of libraries to be loaded without suffix!
     */
    String[] libs();
}