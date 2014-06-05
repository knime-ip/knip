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
    boolean isOs(final String osname);

    /**
     * @return representative name of the operating system. this name is also used in the project name.
     */
    String shortOSName();

    /**
     * @return names of libraries to be loaded without suffix!
     */
    String[] libs();
}