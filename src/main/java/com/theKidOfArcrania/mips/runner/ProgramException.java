package com.theKidOfArcrania.mips.runner;

/**
 * Represents some error that has occurred while running some MIPS code
 * @author Henry Wang
 */
public class ProgramException extends Exception {

    private static final long serialVersionUID = 8832951543958703500L;

    /**
     * Represents all the possible exceptions that can occur inside the MIPS code
     */
    public enum ErrorType {
        INT(0, "External interrupt"),
        ADDRL(4, "Load from an illegal address"),
        ADDRS(5, "Store to an illegal address"),
        IBUS(6, "Instruction bus error"),
        DBUS(7, "Data reference bus error"),
        SYSCALL(8, "Syscall"),
        BKPT(9, "Hit breakpoint"),
        RI(10, "Reserved instruction"),
        ARIH(12, "Arithmetic exception"),
        EXIT(13, "Program halting");

        private final int number;
        private final String description;

        /**
         * Constructor for error types
         * @param number error number
         * @param description short description of error.
         */
        ErrorType(int number, String description) {
            this.number = number;
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final ErrorType type;

    /**
     * Constructs a program exception
     * @param type the error type to throw
     */
    public ProgramException(ErrorType type) {
        super(type.name() + ": " + type.getDescription());
        this.type = type;
    }

    public ErrorType getType() {
        return type;
    }
}
