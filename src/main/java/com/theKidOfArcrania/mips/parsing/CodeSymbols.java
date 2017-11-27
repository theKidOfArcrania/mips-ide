package com.theKidOfArcrania.mips.parsing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.theKidOfArcrania.mips.Constants.*;

/**
 * Represents all the symbols of a particular piece of code.
 *
 * @author Henry Wang
 */
public class CodeSymbols {
    private final HashMap<String, CodeStatement> labels;
    private final HashMap<CodeStatement, Integer> statementAddrs;
    private final HashSet<String> pendingLabels;
    private final HashSet<String> global;

    private int currentSegment;
    private final int[] segmentAddrs;

    private int alignment;
    private int alignmentBits;


    /**
     * Constructs a new code symbol table.
     */
    public CodeSymbols() {
        labels = new HashMap<>();
        statementAddrs = new HashMap<>();
        pendingLabels = new HashSet<>();
        global = new HashSet<>();
        segmentAddrs = new int[] {ADDR_TEXT, ADDR_DATA, ADDR_GLOBL, ADDR_KTEXT, ADDR_KDATA};
        alignment = 0;
        currentSegment = -1;
    }

    /**
     * Adds a label to the list of global symbols. This label does not need to exist at this point
     * @param label the label to add
     */
    public void addGlobalSymbol(String label) {
        global.add(label);
    }

    /**
     * Queries whether if a label is a global symbol
     * @param label the label to check
     * @return true if it is global, false if not global.
     */
    public boolean isGlobalSymbol(String label) {
        return global.contains(label);
    }

    public int getCurrentSegment() {
        return currentSegment;
    }

    public void setCurrentSegment(int currentSegment) {
        if (currentSegment < 0 || currentSegment >= segmentAddrs.length) {
            throw new IllegalArgumentException("Invalid segment type");
        }
        this.currentSegment = currentSegment;
    }

    /**
     * Maps a code statement to the top of the current segment, possibly aligning the code statement if necessary.
     * @param smt the code statement
     * @param size the size of this code statement to allocate
     * @param defAlign the default alignment needed by this statement
     * @throws IllegalStateException if we are not in a segment yet.
     */
    public void pushToSegment(CodeStatement smt, int size, int defAlign) {
        if (currentSegment == -1) {
            throw new IllegalStateException("Not in a valid segment");
        }
        int start = alignAddress(segmentAddrs[currentSegment], defAlign);
        mapStatementToAddress(smt, start);
        segmentAddrs[currentSegment] = start + size;
    }

    /**
     * Obtains a code statement's associated address, or -1 if not associated at all
     * @param smt the code statement
     * @return the statement address or -1 if not found
     */
    public int getStatementAddress(CodeStatement smt) {
        return statementAddrs.getOrDefault(smt, -1);
    }


    public int getAlignment() {
        return alignmentBits;
    }

    /**
     * Sets the alignment to a number of bits.
     * @param bits the number of bits to align addresses to
     */
    public void setAlignment(int bits) {
        alignmentBits = bits;
        if (bits == -1) {
            alignment = -1;
        } else {
            alignment = (1 << bits) - 1;
        }
    }

    /**
     * Adds a pending label that will automatically map to the next statement that has an address associated with it.
     * @param lbl the string label
     * @return true if this is added, false if this label already exists.
     */
    public boolean addLabel(String lbl) {
        return !labels.containsKey(lbl) && pendingLabels.add(lbl);
    }


    /**
     * Aligns a particular address value by padding with some spaces
     * @param addr the address to align
     * @param defAlign default alignment to do (in number of bits)
     * @return an aligned address based on current alignment parameters
     */
    public int alignAddress(int addr, int defAlign) {
        int align = alignment;
        if (alignment == -1) {
            align = (1 << defAlign) - 1;
        }
        return (addr - align) & ~align;
    }

    /**
     * Clears all the pending labels
     */
    public void clearPending() {
        pendingLabels.clear();
    }

    /**
     * Obtains a copy of the set of labels currently pending to be added.
     * @return the set of pending labels
     */
    public Set<String> getPending() {
        return new HashSet<>(pendingLabels);
    }

    /**
     * Maps a code statement to an associated address of memory. This will also map any pending labels to this statement
     * @param smt the code statement
     * @param addr the address
     */
    public void mapStatementToAddress(CodeStatement smt, int addr) {
        if (!pendingLabels.isEmpty()) {
            for (String lbl : pendingLabels) {
                labels.put(lbl, smt);
            }
            pendingLabels.clear();
        }
        statementAddrs.put(smt, addr);
    }

    /**
     * Resolves a label's location to an address.
     * @param lbl name of the label
     * @return the address of the label or -1 if not resolved
     */
    public int resolveLabel(String lbl) {
        return statementAddrs.getOrDefault(labels.get(lbl), -1);
    }
}
