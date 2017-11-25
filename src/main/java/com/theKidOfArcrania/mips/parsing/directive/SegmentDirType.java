package com.theKidOfArcrania.mips.parsing.directive;

import com.theKidOfArcrania.mips.parsing.CodeSymbols;
import com.theKidOfArcrania.mips.parsing.ErrorLogger;

import static com.theKidOfArcrania.mips.Constants.*;
import static com.theKidOfArcrania.mips.parsing.BasicParamType.STRING;
import static com.theKidOfArcrania.mips.parsing.directive.DirTypes.addDirective;


/**
 * This directive type represents the set of directives that switches between memory segments. Currently, these
 * directives do not support the optional address argument to change the segment address. This
 * includes the following directives and the following syntaxes:
 * <pre>
 *   .text
 *   .data
 *   .ktext
 *   .kdata
 * </pre>
 * Each segment represents a different part of the program (text for instructions, data for misc data), and the
 * prefix <code>k</code> represents the kernel counterparts of these segments
 * @author Henry Wang
 */
public class SegmentDirType extends DirType {

    /**
     * Initializes all the directives
     */
    static void init() {
        addDirective("text", new SegmentDirType(SEG_TEXT));
        addDirective("data", new SegmentDirType(SEG_DATA));
        addDirective("ktext", new SegmentDirType(SEG_KTEXT));
        addDirective("kdata", new SegmentDirType(SEG_KDATA));
    }

    private final int segment;

    /**
     * Constructs a string-data directive-type
     * @param segment the segment index represented by this directive
     */
    private SegmentDirType(int segment) {
        super(-1, false);
        this.segment = segment;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This will switch to the memory segment specified by this directive. This will also reset the alignment value.
     *
     * @return Always returns <code>true</code>
     */
    @Override
    public boolean resolveSymbols(ErrorLogger logger, DirStatement dir, CodeSymbols symbols) {
        if (!super.resolveSymbols(logger, dir, symbols)) {
            return false;
        }

        symbols.setAlignment(-1);
        symbols.setCurrentSegment(segment);
        return true;
    }
}
