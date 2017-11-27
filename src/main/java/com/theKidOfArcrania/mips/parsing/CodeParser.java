package com.theKidOfArcrania.mips.parsing;

import com.theKidOfArcrania.mips.highlight.*;
import com.theKidOfArcrania.mips.parsing.directive.DirStatement;
import com.theKidOfArcrania.mips.parsing.inst.InstOpcodes;
import com.theKidOfArcrania.mips.parsing.inst.InstStatement;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.theKidOfArcrania.mips.parsing.Range.tokenRange;

/**
 * This parses the code using {@link CodeTokenReader} as the parser, and allows for continuous checks if necessary.
 * This will parse the code into a series of {@link CodeStatement} objects. This also provides a list of errors that
 * might have occurred while parsing.
 *
 * @author Henry Wang
 */
public class CodeParser {
    public static final CodeStatement INVALID_STATEMENT = new EmptyStatement();
    public static final CodeStatement DIRTY_STATEMENT = new EmptyStatement();

    private static final EnumMap<TokenType, Pattern> syntaxMatches;
    private static final EnumMap<TokenType, SyntaxType[]> syntaxScheme;

    private static final Pattern DEF_MAT = Pattern.compile(".*");

    static {
        syntaxScheme = new EnumMap<>(TokenType.class);
        syntaxMatches = new EnumMap<>(TokenType.class);

        syntaxScheme.put(TokenType.DECIMAL, new SyntaxType[] {SyntaxType.NUMBER});
        syntaxScheme.put(TokenType.HEXADECIMAL, new SyntaxType[] {SyntaxType.NUMBER});
        syntaxScheme.put(TokenType.REGISTER, new SyntaxType[] {SyntaxType.REGISTER});
        syntaxScheme.put(TokenType.IDENTIFIER, new SyntaxType[] {SyntaxType.IDENTIFIER});
        syntaxScheme.put(TokenType.STRING, new SyntaxType[] {SyntaxType.STRING});
        syntaxScheme.put(TokenType.INDIRECT, new SyntaxType[] {null, SyntaxType.NUMBER, SyntaxType.REGISTER});

        syntaxMatches.put(TokenType.INDIRECT, Pattern.compile("(-?[0-9]+)?\\((\\$[a-zA-Z0-9]+)\\)"));
    }

    private final CodeTokenReader reader;
    private final ArrayList<CodeStatement> parsedCode;
    private final Highlighter highlighter;

    /**
     * Constructs a CodeParser from the specified code body.
     *
     * @param code        the code body to read from.
     * @param highlighter the highlighter used to highlight syntax and tags.
     */
    public CodeParser(String code, Highlighter highlighter) {
        reader = new CodeTokenReader(code);

        this.highlighter = highlighter;

        int lines = reader.getLineCount();
        parsedCode = new ArrayList<>(lines);
        for (int i = 0; i < lines; i++) {
            parsedCode.add(DIRTY_STATEMENT);
            reader.nextLine();
            parseLine();
        }

        reader.addErrorLogger(new ErrorLogger() {
            @Override
            public void logError(String description, Range highlight) {
                highlighter.insertTag(new Tag(TagType.ERROR, highlight, description));
            }

            @Override
            public void logWarning(String description, Range highlight) {
                highlighter.insertTag(new Tag(TagType.WARNING, highlight, description));
            }
        });
    }

    public int getLineCount() {
        return reader.getLineCount();
    }

    /**
     * Inserts a new line of code at the particular line number. This new line will be marked dirty, but will not be
     * automatically parsed until a call to {@link #reparse(boolean)}. It will update the changed line references for
     * each code statement
     *
     * @param lineNum the 1-based line number.
     * @param line    the line to insert.
     */
    public void insertLine(int lineNum, String line) {
        reader.insertLine(lineNum, line);
        parsedCode.add(lineNum - 1, DIRTY_STATEMENT);
        for (int i = lineNum; i < parsedCode.size(); i++) {
            parsedCode.get(i).updateLinePos(i + 1);
        }
    }

    /**
     * Modifies a line of code. This will mark the current line as dirty, but will not reparse the code until a call
     * to {@link #reparse(boolean)}.
     *
     * @param lineNum the 1-based line number.
     * @param line    the line to modify to
     */
    public void modifyLine(int lineNum, String line) {
        reader.modifyLine(lineNum, line);
        parsedCode.set(lineNum - 1, DIRTY_STATEMENT);
    }

    /**
     * This deletes a line of code. This will then update the lines references in each code statement
     *
     * @param lineNum the line number to remove.
     */
    public void deleteLine(int lineNum) {
        reader.deleteLine(lineNum);
        parsedCode.remove(lineNum - 1);
        for (int i = lineNum - 1; i < parsedCode.size(); i++) {
            parsedCode.get(i).updateLinePos(i + 1);
        }
    }

    /**
     * Obtains line at the particular line number
     *
     * @param lineNum line number.
     * @return the line string.
     */
    public String getLine(int lineNum) {
        return reader.getLine(lineNum);
    }

    /**
     * Re-parses all the lines of dirty code. This may emit any parsing errors if encountered. By definition this
     * function is successful if and only if every single line is parsed, and is not left dirty or invalid.
     *
     * @param parseInvalid determines whether to reparse any invalid lines.
     * @return true if re-parse was successful, false if some errors occurred while re-parsing.
     */
    public boolean reparse(boolean parseInvalid) {
        return reparse(parseInvalid, null);
    }

    /**
     * Re-parses all the lines of dirty code. This may emit any parsing errors if encountered. By definition this
     * function is successful if and only if every single line is parsed, and is not left dirty or invalid. This
     * particular variant will automatically cancel when the atomic boolean value gets flagged
     *
     * @param parseInvalid determines whether to reparse any invalid lines.
     * @param cancelled the atomic boolean prop to check if task was cancelled.
     * @return true if re-parse was successful, false if some errors occurred while re-parsing.
     */
    public boolean reparse(boolean parseInvalid, AtomicBoolean cancelled) {
        if (cancelled == null) {
            cancelled = new AtomicBoolean(false);
        }

        boolean success = true;
        for (int i = 0; i < parsedCode.size(); i++) {
            boolean invalid = parsedCode.get(i) == INVALID_STATEMENT;
            boolean dirty = parsedCode.get(i) == DIRTY_STATEMENT;

            if (dirty || invalid && parseInvalid) {
                try {
                    reader.beginLine(i + 1);
                    success &= parseLine();
                    if (cancelled.get()) {
                        return false;
                    }
                } catch (RuntimeException e) {
                    //TODO: Better error logging.
                    reader.error("Error occurred while parsing line: " + e.toString() + ".", Range.lineRange(reader));
                    e.printStackTrace();
                    success = false;
                }
            } else if (invalid) {
                success = false;
            }
        }
        return success;
    }


    /**
     * Ensures that all the symbols referred to by the code are resolved. This should be faster than the parsing
     * time, so this will be called on each parsed statement each time.
     *
     * @return true if resolution was successful, false if it failed.
     */
    public boolean resolveSymbols() {
        return resolveSymbols(null);
    }

    /**
     * Ensures that all the symbols referred to by the code are resolved. This should be faster than the parsing
     * time, so this will be called on each parsed statement each time. This particular variant will automatically
     * cancel when the atomic boolean value gets flagged
     *
     * @param cancelled the atomic boolean prop to check if task was cancelled
     * @return true if resolution was successful, false if it failed.
     */
    public boolean resolveSymbols(AtomicBoolean cancelled) {
        if (cancelled == null) {
            cancelled = new AtomicBoolean(false);
        }

        CodeSymbols symbols = new CodeSymbols();

        //Resolve statements
        boolean success = true;
        for (CodeStatement s : parsedCode) {
            success &= s.resolveSymbols(symbols);
            if (cancelled.get()) {
                return false;
            }
        }

        //Verify that all symbols are resolved
        for (CodeStatement s : parsedCode) {
            success &= s.verifySymbols(symbols);
            if (cancelled.get()) {
                return false;
            }
        }

        return success;
    }

    /**
     * Determines whether if a line is dirty. A line is defined as dirty if it has been modified since the last time
     * it was parsed.
     *
     * @param line the 1-based line number
     * @return true if dirty, false if not dirty.
     */
    public boolean isLineDirty(int line) {
        return parsedCode.get(line - 1) == DIRTY_STATEMENT;
    }

    /**
     * Determines whether if a line is malformed. A line is malformed a parsing error occurred the last time it was
     * parsed.
     *
     * @param line the 1-based line number.
     * @return true if malformed, false if not malformed.
     */
    public boolean isLineMalformed(int line) {
        return parsedCode.get(line - 1) == INVALID_STATEMENT;
    }

    /**
     * Parse the currently selected line in the reader.
     *
     * @return true the parsing line was successful, false if an error occurred.
     * @throws IllegalStateException if this token reader isn't currently on a line.
     */
    private boolean parseLine() {
        if (reader.getLineNumber() < 1) {
            throw new IllegalStateException("Not currently reading a line.");
        }

        boolean success = true;
        int lineInd = reader.getLineNumber() - 1;
        if (!reader.nextToken()) {
            parsedCode.set(lineInd, new EmptyStatement());
        } else if (reader.hasTokenError()) {
            parsedCode.set(lineInd, INVALID_STATEMENT);
            return false;
        } else {
            CodeStatement line;
            switch (reader.getTokenType()) {
                case DIRECTIVE:
                    line = DirStatement.parseStatement(reader);
                    break;
                case IDENTIFIER:
                    line = InstStatement.parseStatement(reader);
                    break;
                case LABEL:
                    line = LabelStatement.parseStatement(reader);
                    break;
                default:
                    reader.errorExpected("label, instruction, or directive");
                    line = null;
            }
            success = line != null;
            parsedCode.set(lineInd, success ? line : INVALID_STATEMENT);
        }

        //Syntax highlighting.
        parseSyntaxHighlight();
        return success;
    }

    /**
     * Parses all the syntax highlights of the current line.
     */
    private void parseSyntaxHighlight() {
        int prevEnd = -1;
        if (reader.getTokensRead() > 0) {
            reader.visitToken(0);
        }

        for (int i = 0; i < reader.getTokensRead(); i++) {
            SyntaxType type = null;
            if (i == 0) {
                String token = reader.getToken();
                if (token.startsWith(".")) {
                    type = SyntaxType.DIRECTIVE;
                } else if (token.endsWith(":")) {
                    type = SyntaxType.LABEL;
                } else {
                    if (InstOpcodes.fetchOpcode(token) != null) {
                        type = SyntaxType.INSTRUCTION;
                    }
                }
            } else {
                int offset = reader.getTokenStartIndex();
                TokenType tokType = reader.getTokenType();
                if (tokType != null) {
                    Matcher mat = syntaxMatches.getOrDefault(tokType, DEF_MAT).matcher(reader.getToken());
                    if (mat.find()) {
                        SyntaxType[] scheme = syntaxScheme.getOrDefault(tokType, new SyntaxType[mat.groupCount() + 1]);
                        for (int j = 0; j < mat.groupCount() + 1; j++) {
                            if (mat.group(j) != null && scheme[j] != null) {
                                highlighter.insertSyntax(new Syntax(scheme[j], tokenRange(reader.getLineNumber(),
                                        offset + mat.start(j), offset + mat.end(j))));
                            }
                        }
                    }
                }
                characterSyntax(prevEnd, reader.getTokenStartIndex(), ',', SyntaxType.COMMA);
            }

            if (type != null) {
                highlighter.insertSyntax(new Syntax(type, reader.getTokenPos()));
            }

            prevEnd = reader.getTokenEndIndex();
            reader.nextToken(true);
        }

        characterSyntax(0, prevEnd, '(', SyntaxType.PARENTHESIS);
        characterSyntax(0, prevEnd, ')', SyntaxType.PARENTHESIS);

        int len = reader.getLine().length();
        int commentStart = reader.getCommentStartIndex();
        if (commentStart != -1) {
            highlighter.insertSyntax(new Syntax(SyntaxType.COMMENT,
                    tokenRange(reader.getLineNumber(), commentStart, len)));
        }
    }

    /**
     * Marks the character syntax of a specific character between a start (inclusive) and end (exclusive) point
     * @param start     the inclusive starting index
     * @param end       the exclusive ending index
     * @param ch        the character to highlight
     * @param highlight the type of highlight to tag with character
     */
    private void characterSyntax(int start, int end, char ch, SyntaxType highlight) {
        String line = reader.getLine();
        start = line.indexOf(ch, start);
        while (start != -1 && start < end) {
            highlighter.insertSyntax(new Syntax(highlight, Range.characterRange(reader.getLineNumber(), start)));
            start = line.indexOf(ch, start + 1);
        }
    }
}
