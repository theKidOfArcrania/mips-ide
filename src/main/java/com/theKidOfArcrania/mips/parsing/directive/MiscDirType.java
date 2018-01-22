package com.theKidOfArcrania.mips.parsing.directive;

import com.theKidOfArcrania.mips.parsing.CodeSymbols;
import com.theKidOfArcrania.mips.parsing.ErrorLogger;
import com.theKidOfArcrania.mips.parsing.ParamType;

import static com.theKidOfArcrania.mips.Constants.SEG_DATA;
import static com.theKidOfArcrania.mips.parsing.BasicParamType.*;
import static com.theKidOfArcrania.mips.parsing.directive.DirTypes.addDirective;


/**
 * Represents all the other miscellaneous directive types. Includes the following:
 * <pre>
 *   .extern sym size
 *   .globl sym
 *   .space n
 * </pre>
 *
 * @author Henry Wang
 */
public abstract class MiscDirType extends DirType {

    /**
     * Initializes all the directives
     */
    static void init() {
        addDirective("align", new MiscDirType(SEG_DATA, SHAMT) {
            @Override
            public boolean resolveSymbols(ErrorLogger logger, DirStatement dir, CodeSymbols symbols) {
                if (!super.resolveSymbols(logger, dir, symbols)) {
                    return false;
                }
                symbols.setAlignment(dir.getIntArgValue(0));
                return true;
            }
        });
        addDirective("extern", new MiscDirType(-1, LOCATION, SIZE) {
            @Override
            public boolean resolveSymbols(ErrorLogger logger, DirStatement dir, CodeSymbols symbols) {
                if (!super.resolveSymbols(logger, dir, symbols)) {
                    return false;
                }
                //TODO: not fully implemented
                return true;
            }
        });
        addDirective("globl", new MiscDirType(-1, LOCATION) {
            @Override
            public boolean resolveSymbols(ErrorLogger logger, DirStatement dir, CodeSymbols symbols) {
                if (!super.resolveSymbols(logger, dir, symbols)) {
                    return false;
                }
                symbols.addGlobalSymbol(dir.getArgValue(0, String.class));
                return true;
            }

            @Override
            public boolean verifySymbols(ErrorLogger logger, DirStatement dir, CodeSymbols resolved) {
                if (!super.resolveSymbols(logger, dir, resolved)) {
                    return false;
                }

                if (resolved.resolveLabel(dir.getArgValue(0, String.class)) == -1) {
                    logger.logError("Unresolved global symbol", dir.getLineRange());
                    return false;
                }
                return true;
            }
        });
        addDirective("space", new MiscDirType(SEG_DATA, SIZE) {
            @Override
            public boolean resolveSymbols(ErrorLogger logger, DirStatement dir, CodeSymbols symbols) {
                if (!super.resolveSymbols(logger, dir, symbols)) {
                    return false;
                }
                int align = symbols.getAlignment();
                symbols.pushToSegment(dir, dir.getIntArgValue(0), 0);
                symbols.setAlignment(align);
                return true;
            }

            @Override
            public boolean verifySymbols(ErrorLogger logger, DirStatement dir, CodeSymbols resolved) {
                if (!super.resolveSymbols(logger, dir, resolved)) {
                    return false;
                }

                if (resolved.resolveLabel(dir.getArgValue(0, String.class)) == -1) {
                    logger.logError("Unresolved global symbol", dir.getLineRange());
                    return false;
                }
                return true;
            }
        });
    }

    /**
     * Constructs a miscellaneous directive type.
     *
     * @param expectedSegment the expected section that this directive type should be in
     * @param params          the parameter types with this directive
     */
    private MiscDirType(int expectedSegment, ParamType... params) {
        super(expectedSegment, false, params);
    }
}
