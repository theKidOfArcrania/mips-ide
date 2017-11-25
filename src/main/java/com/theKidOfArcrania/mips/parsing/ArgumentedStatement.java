package com.theKidOfArcrania.mips.parsing;


import java.util.Iterator;

import static com.theKidOfArcrania.mips.parsing.Range.lineRange;

/**
 * Represents any code statements that consists of a list of arguments.
 *
 * @author Henry Wang
 */
public abstract class ArgumentedStatement extends CodeStatement implements Iterable<Argument> {
    protected final CodeTokenReader reader;
    private final Argument[] args;
    private final Range lineRange;

    /**
     * Creates a new argumented statement.
     *
     * @param reader the token-reader with this statement.
     * @param args   the arguments of this statement
     */
    public ArgumentedStatement(CodeTokenReader reader, Argument[] args) {
        this.reader = reader;
        this.args = args.clone();
        lineRange = Range.lineRange(reader);
    }

    @Override
    public Iterator<Argument> iterator() {
        return new Iterator<Argument>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return index < args.length;
            }

            @Override
            public Argument next() {
                return args[index++];
            }
        };
    }

    public Argument[] getArgs() {
        return args.clone();
    }

    /**
     * Obtains the argument descriptor at the particular index
     *
     * @param ind the index of the argument.
     * @return the argument descriptor.
     */
    public Argument getArg(int ind) {
        return args[ind];
    }

    /**
     * Obtains the argument's exact type at the particular index.
     *
     * @param ind the index of the argument.
     * @return the exact parameter type.
     * @see ParamType#getExactType(CodeTokenReader)
     * @see Argument#getExactType()
     */
    public ParamType getArgExactType(int ind) {
        return getArg(ind).getExactType();
    }

    /**
     * Obtains the token range of the particular argument
     *
     * @param ind the index of the argument
     * @return a valid range that describes the position of the argument token.
     */
    public Range getArgPos(int ind) {
        return getArg(ind).getTokenPos();
    }

    /**
     * @return the number of arguments.
     */
    public int getArgSize() {
        return args.length;
    }

    /**
     * Obtains the parsed argument at the particular index.
     *
     * @param ind the index of the argument.
     * @return a parsed object of the argument.
     */
    public Object getArgValue(int ind) {
        return getArg(ind).getValue();
    }

    /**
     * Obtains the parsed argument at the particular index, ensuring that the object is of a particular type.
     *
     * @param ind  the index of the argument.
     * @param type the required type
     * @param <T>  generic that this object will automatically be cast to.
     * @return the casted object.
     * @throws IllegalArgumentException if object is not of correct type.
     */
    @SuppressWarnings("unchecked")
    public <T> T getArgValue(int ind, Class<T> type) {
        Object o = getArgValue(ind);
        if (!type.isInstance(o)) {
            throw new IllegalArgumentException();
        }
        return (T) o;
    }

    /**
     * Obtains the parsed argument as am integer value.
     *
     * @param ind the index of the argument
     * @return the integer argument.
     * @throws IllegalArgumentException if the argument cannot be converted into an integer without lossy conversion
     */
    public int getIntArgValue(int ind) {
        return getArgValue(ind, Integer.class);
    }

    public Range getLineRange() {
        return lineRange;
    }
}
