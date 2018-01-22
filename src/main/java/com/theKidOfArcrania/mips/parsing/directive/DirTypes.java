package com.theKidOfArcrania.mips.parsing.directive;

import java.util.HashMap;

/**
 * This maintains a list of all the legal directive types in the MIPS language, and it will automatically call all
 * the sub-types and make sure they are initialized.
 *
 * @author Henry Wang
 */
public final class DirTypes {
    private static final HashMap<String, DirType> dirs;

    static {
        dirs = new HashMap<>();
        BinaryDataDirType.init();
        StringDataDirType.init();
        SegmentDirType.init();
        MiscDirType.init();
    }

    /**
     * Locates the directive with the particular name.
     *
     * @param name the name of directive (without dollar-sign prepend).
     * @return the directive type if found.
     */
    public static DirType fetchDirective(String name) {
        return dirs.get(name.toUpperCase());
    }

    /**
     * Adds a specific directive type and maps it to the name. This is called internally by subclasses of
     * {@link DirType} to add those directives into our collection of DirTypes. The DirType instance should be
     * completely stateless and immutable in the sense that it would process each directive statement of its type in
     * the same manner.
     *
     * @param name the name of the directive
     * @param dir  the directive type
     */
    static void addDirective(String name, DirType dir) {
        dirs.put(name.toUpperCase(), dir);
    }
}
