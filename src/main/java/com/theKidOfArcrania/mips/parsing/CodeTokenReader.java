package com.theKidOfArcrania.mips.parsing;

import com.theKidOfArcrania.mips.Constants;
import com.theKidOfArcrania.mips.util.FallibleFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.theKidOfArcrania.mips.parsing.Range.characterRange;
import static com.theKidOfArcrania.mips.parsing.Range.tokenRange;
import static com.theKidOfArcrania.mips.parsing.TokenType.*;
import static com.theKidOfArcrania.mips.runner.Registers.*;
import static com.theKidOfArcrania.mips.util.FallibleFunction.tryOptional;
import static java.lang.Character.isJavaIdentifierStart;
import static java.lang.Character.isWhitespace;
import static java.lang.Integer.parseInt;

/**
 * Reads in token words for each line of code. This splits an existing body of code into lines, and it parses each
 * line individually as needed. This contains token type recognition and it will pre-parse these token types. This
 * will also provide a flexible error logging system whenever an parsing error occurs that should flag the user's
 * attention.
 * <p>
 * A token word is defined as a sequence of word characters delimited by word boundaries (such as a whitespace). This
 * reads in the entire code line by line and looks at each line individually.
 * <p>
 * Note that this is NOT synchronization safe. Specifically, concurrent modifications to the line while parsing a
 * line of code is not allowed. If concurrent parsing needs to be done, what can be suggested is to maintain a backlog
 * of all the change made while parsing.
 *
 * @author Henry Wang
 */
public class CodeTokenReader implements Constants {
    private static final Pattern NEW_LINE = Pattern.compile("\r?\n");
    private static final Pattern INDIRECT = Pattern.compile("^(-?[0-9]+)?\\((\\$[a-zA-Z0-9]+)\\)$");
    private static final String[] REG_ALIAS = {"$zero", "$at", "$v0", "$v1", "$a1", "$a1", "$a2", "$a3", "$t0", "$t1",
            "$t2", "$t3", "$t4", "$t5", "$t6", "$t7", "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7", "$t8",
            "$t9", "$k0", "$k1", "$gp", "$sp", "$fp", "$ra"};


    private final ArrayList<String> lines;
    private String line;

    private int lineNum;
    private int colNum;
    private int commentStartInd;

    private int tokenNum;
    private boolean tokenError;
    private final ArrayList<Integer> prevTokens;

    private TokenType tokenType;
    private String token;
    private Object tokenVal;
    private int tokenStartIndex;
    private int tokenEndIndex;

    private boolean firstArgument;
    private boolean hasArgumentSeparator;
    private boolean argumentError;

    private final ArrayList<ErrorLogger> errLogs;

    private final ErrorLogger delegateLogger;

    /**
     * Constructs a CodeTokenReader reading from the specified code body.
     *
     * @param code the code body to read from.
     */
    public CodeTokenReader(String code) {
        lines = new ArrayList<>();
        Collections.addAll(lines, NEW_LINE.split(code));
        line = null;
        lineNum = 0;

        errLogs = new ArrayList<>();

        prevTokens = new ArrayList<>();
        resetLine();

        this.delegateLogger = new ErrorLogger() {
            @Override
            public void logError(String description, Range highlight) {
                error(description, highlight);
            }

            @Override
            public void logWarning(String description, Range highlight) {
                warning(description, highlight);
            }
        };
    }

    /**
     * Obtains line at the particular line number
     *
     * @param lineNum line number.
     * @return the line string.
     */
    public String getLine(int lineNum) {
        return lines.get(lineNum - 1);
    }

    public String getLine() {
        return line;
    }

    public int getLineCount() {
        return lines.size();
    }

    /**
     * Moves the token reader to the next line of code.
     *
     * @throws NoSuchElementException if there is no next line.
     */
    public void nextLine() {
        if (!hasNextLine()) {
            throw new NoSuchElementException();
        }
        line = lines.get(lineNum++);
        resetLine();
    }

    /**
     * Resets the current line, as if nothing was ever read on this line.
     */
    public void resetLine() {
        colNum = 0;
        commentStartInd = -1;

        tokenStartIndex = -1;
        tokenEndIndex = -1;
        tokenType = null;
        token = null;
        firstArgument = true;

        prevTokens.clear();
        tokenNum = -1;
    }

    /**
     * Inserts a new line at the particular line number. It will automatically update the current line number if
     * inserting this line will shift the current line by one index.
     *
     * @param num  1-based index of the line number.
     * @param line the line to add
     */
    public void insertLine(int num, String line) {
        lines.add(num - 1, line);
        if (num <= lineNum) {
            lineNum++;
        }
    }

    /**
     * Deletes a line of code from this token reader. If this deletes the current line, this will invalidate any
     * parsing that might have occurred (and also invalidate the current line number) This will automatically update
     * the current line number if deleting this line will shift the current line by one index.
     *
     * @param num 1-based index of the line number.
     */
    public void deleteLine(int num) {
        if (lineNum == num) {
            resetLine();
            lineNum = 0;
            line = null;
        }
        lines.remove(num - 1);
        if (num < lineNum) {
            lineNum--;
        }
    }

    /**
     * Modifies a line of code to a new line. If the currently parsed line is selected, this will automatically
     * invalidate the current token, and will reset as if invoked from {@link #nextLine()}.
     *
     * @param num  1-based index of the line number.
     * @param line the new value to change to.
     */
    public void modifyLine(int num, String line) {
        if (lineNum == num) {
            resetLine();
        }
        lines.set(num - 1, line);
    }

    /**
     * Adds an error logger to this token reader.
     *
     * @param logger the error logger.
     */
    public void addErrorLogger(ErrorLogger logger) {
        errLogs.add(logger);
    }

    /**
     * Removes an error logger from this token reader.
     *
     * @param logger the error logger.
     */
    public void removeErrorLogger(ErrorLogger logger) {
        errLogs.remove(logger);
    }

    /**
     * Emits an error to all the error loggers.
     *
     * @param description the error message description
     * @param highlight   specifies the position that this error is highlighting, can be null.
     */
    public void error(String description, Range highlight) {
        if (tokenNum == prevTokens.size() - 1 || tokenNum == -1) //not visiting
        {
            for (ErrorLogger logger : errLogs)
                logger.logError(description, highlight);
        }
    }

    /**
     * Emits an expected error. This specifically refers to an error where the user fails to provide the correct type
     * of token where needed.
     *
     * @param type the type the user needs to specify.
     */
    public void errorExpected(String type) {
        error("Expected: valid " + type + ".", tokenNum == -1 ? Range.lineRange(this) : getTokenPos());
    }

    /**
     * Begins parsing the specified line number.
     *
     * @param lineNum the line number to parse.
     */
    public void beginLine(int lineNum) {
        line = lines.get(lineNum - 1);
        this.lineNum = lineNum;
        resetLine();
    }

    /**
     * Emits a warning to all the error loggers
     *
     * @param description the warning message description
     * @param highlight   specifies the position that this warning is highlighting, can be null.
     */
    public void warning(String description, Range highlight) {
        if (tokenNum == prevTokens.size() - 1 || tokenNum == -1) //not visiting
        {
            for (ErrorLogger logger : errLogs)
                logger.logWarning(description, highlight);
        }
    }

    /**
     * Obtains the delegate logger for this error logger. This is done whenever a program wants allow a
     * subsection to be able to log errors but not access the code token reader. This wraps the reader around a thin
     * opaque interface only capable of logging errors. This may not return new instances each time. In fact, this
     * error logger delegate will be reused each time, since this error logger is immutable.
     *
     * @return the delegate error logger.
     */
    public ErrorLogger getDelegateLogger() {
        return delegateLogger;
    }

    public int getLineNumber() {
        return lineNum;
    }

    public int getCommentStartIndex() {
        return commentStartInd;
    }

    /**
     * Gets the current token as a string
     *
     * @return the token string value
     * @throws IllegalStateException if no current token is selected.
     */
    public String getToken() {
        if (tokenNum == -1) {
            throw new IllegalStateException("No current token selected.");
        }
        return token;
    }

    public int getTokenNum() {
        return tokenNum;
    }

    public int getTokensRead() {
        return prevTokens.size();
    }

    /**
     * Gets the token type that is being parsed (INTEGER, FLOAT, STRING, LOCATION).
     *
     * @return an enum value representing the token type.
     * @throws IllegalStateException if no current token is selected.
     */
    public TokenType getTokenType() {
        if (tokenNum == -1) {
            throw new IllegalStateException("No current token selected.");
        }
        return tokenType;
    }

    /**
     * Gets the range that specifies the boundaries of this token
     *
     * @return the token boundaries
     * @throws IllegalStateException if no current token is selected.
     */
    public Range getTokenPos() {
        if (tokenNum == -1) {
            throw new IllegalStateException("No current token selected.");
        }
        return tokenRange(lineNum, getTokenStartIndex(), getTokenEndIndex());
    }

    public int getTokenStartIndex() {
        return tokenStartIndex;
    }

    public int getTokenEndIndex() {
        return tokenEndIndex;
    }

    /**
     * Parses the token value based on the token type that is being parsed.
     *
     * @return
     */
    public Object getTokenValue() {
        if (tokenNum == -1) {
            throw new IllegalStateException("No current token selected.");
        }
        return tokenVal;
    }

    /**
     * Determines whether if parsing the token produced an error.
     *
     * @return true if an error occurred, false otherwise.
     */
    public boolean hasTokenError() {
        return tokenError;
    }

    /**
     * Checks whether if this code body has a next line to read.
     *
     * @return true if there is a line remaining, false otherwise.
     */
    public boolean hasNextLine() {
        return lines.size() > lineNum;
    }

    /**
     * Moves to the next token on this current line. If there are no more tokens on this line, we will return
     * <code>false</code>. By default, this will not allow any commas between tokens. If a comma *may* be expected,
     * then the variant {@link #nextToken(boolean)} should be called.
     *
     * @return true if there is another token, false if no more tokens exist.
     * @throws IllegalStateException if this token reader isn't currently on a line.
     */
    public boolean nextToken() {
        return nextToken(false);
    }

    /**
     * Moves to the next token on this current line. If there are no more tokens on this line, we will return
     * <code>false</code>. This allows the client to bypass the comma check if necessary.
     *
     * @param allowComma true to allow commas, false to enforce no commas.
     * @return true if there is another token, false if no more tokens exist.
     * @throws IllegalStateException if this token reader isn't currently on a line.
     */
    public boolean nextToken(boolean allowComma) {
        readNextToken();
        if (!allowComma && hasArgumentSeparator && !argumentError) {
            int comma = tokenStartIndex == -1 ? (commentStartInd == -1 ? line.length() - 1 : commentStartInd)
                    : tokenStartIndex;
            while (line.charAt(comma) != ',')
                comma--;
            error("Unexpected comma.", characterRange(lineNum, comma));
        }
        return tokenStartIndex != -1;
    }

    /**
     * Bulk parses a series number of arguments with the specified parameter types.
     *
     * @param params the parameters to parse
     * @return the parsed arguments.
     */
    public Argument[] parseArguments(ParamType... params) {
        boolean error = false;
        Argument[] args = new Argument[params.length];
        for (int i = 0; i < params.length; i++) {
            if (!nextArgument()) {
                errorExpected(params[i].getName());
                return null;
            }
            if (!params[i].matches(this)) {
                errorExpected(params[i].getName());
                error = true;
            } else if (tokenError) {
                error = true;
            } else if (tokenVal == null) {
                errorExpected(params[i].getExactType(this).getName());
                error = true;
            } else if (params[i].checkToken(this)) {
                args[i] = new Argument(this, params[i]);
            } else {
                error = true;
            }
        }

        return error ? null : args;
    }

    /**
     * Moves to the next token on this current line. If there are no more tokens on this line, we will return
     * <code>false</code>. Unlike {@link #nextToken()}, this will also search for a preceding comma separator (if
     * this is not the first argument). If one does not exist, it will log an error. This will still return
     * <code>true</code>, as another argument is found, but it will flag the error.
     *
     * @return true if there is another token, false if no more tokens exist.
     * @throws IllegalStateException if this token reader isn't currently on a line.
     */
    public boolean nextArgument() {
        if (firstArgument && tokenNum == prevTokens.size() - 1) {
            firstArgument = false;
            return nextToken();
        }

        readNextToken();
        if (tokenStartIndex != -1 && !hasArgumentSeparator && tokenNum == prevTokens.size() - 1) {
            error("Expected a comma separator.", characterRange(lineNum, tokenStartIndex - 1));
        } else if (tokenStartIndex == -1 && hasArgumentSeparator && !argumentError) {
            int comma = colNum - 1;
            while (line.charAt(comma) != ',')
                comma--;
            error("Unexpected comma.", characterRange(lineNum, comma));
        }
        return tokenStartIndex != -1;
    }

    /**
     * Visits a previous token. If any parsing error/warning occurs, they will be silently ignored, since they
     * probably have already been emitted once already.
     *
     * @param tokenNum the token index to visit
     */
    public void visitToken(int tokenNum) {
        if (lineNum < 1) {
            throw new IllegalStateException("Not currently reading a line.");
        }

        colNum = prevTokens.get(tokenNum);
        this.tokenNum = tokenNum - 1;
        readNextToken();
    }

    /**
     * Identifies whether if the current position represents the token starting character. If it is, this will parse
     * the token fully, and change the appropriate indexes to point to the character after the token.
     *
     * @return true if this is a token start, false otherwise.
     */
    private boolean identifyTokenStart() {
        char ch = line.charAt(colNum);
        if (ch == '$') {
            parseToken("");
            parseRegister();
        } else if ((ch >= '0' && ch <= '9') || ch == '-') {
            parseToken("+-()$");
            if (token.contains("(")) {
                parseRegIndirect();
            } else {
                parseNumber();
            }
        } else if (ch == '"') {
            parseStringToken();
        } else if (isJavaIdentifierStart(ch)) {
            tokenType = TokenType.IDENTIFIER;
            parseToken(":");
            if (token.contains(":")) {
                tokenVal = token.substring(0, token.length() - 1);
                verifyLabel();
                tokenType = TokenType.LABEL;
            } else {
                tokenVal = token;
            }
        } else if (ch == '.') {
            tokenType = DIRECTIVE;
            parseToken("");
            tokenVal = token.substring(1);
        } else if (ch == ',') {
            if (hasArgumentSeparator) {
                error("Unexpected comma.", characterRange(lineNum, colNum));
                argumentError = true;
            }
            hasArgumentSeparator = true;
            return false;
        } else if (isWhitespace(ch)) {
            return false;
        } else if (ch == '#') {
            commentStartInd = colNum;
            colNum = line.length();
            return false;
        } else {
            error("Illegal character.", characterRange(lineNum, colNum));
            tokenError = true;
            return false;
        }
        return true;
    }

    /**
     * Increments character position, searching for the next token beginning.
     *
     * @throws IllegalStateException if this token reader isn't currently on a line.
     */
    private void readNextToken() {
        if (lineNum < 1) {
            throw new IllegalStateException("Not currently reading a line.");
        }

        tokenStartIndex = -1;
        tokenEndIndex = -1;
        hasArgumentSeparator = false;
        argumentError = false;
        tokenError = false;
        while (colNum < line.length()) {
            if (identifyTokenStart()) {
                break;
            }
            colNum++;
        }

        //If we read past just token errors we need to make sure that we marked as such an error
        if (tokenError && tokenStartIndex == -1) {
            tokenStartIndex = line.length() - 1;
        }

        if (tokenStartIndex != -1) {
            tokenNum++;
            if (tokenNum >= prevTokens.size()) {
                prevTokens.add(tokenStartIndex);
            }
        } else {
            tokenNum = -1;
        }
    }

    /**
     * Verifies that this label identifier has the correct syntax. This will emit any errors if necessary.
     */
    private void verifyLabel() {
        String val = (String) tokenVal;
        for (int i = 0; i < val.length(); i++) {
            char ch = val.charAt(i);
            if (i == 0 ? !isJavaIdentifierStart(ch) : !Character.isJavaIdentifierPart(ch)) {
                error("Illegal character.", characterRange(lineNum, tokenStartIndex + i));
                tokenError = true;
                return;
            }
        }
    }

    /**
     * Parses a register from the current token. The token value is set to an integer representing the register index.
     */
    private void parseRegister() {
        Integer val = FallibleFunction.tryOptional(Integer::parseInt, token.substring(1)).orElse(null);
        if (val == null) {
            for (int i = 0; i < REG_ALIAS.length; i++) {
                if (REG_ALIAS[i].equals(token)) {
                    val = i;
                    break;
                }
            }
        }

        tokenType = REGISTER;
        if (val == null || val < 0 || val > REGISTER_COUNT) {
            tokenVal = null;
        } else {
            tokenVal = val;
            if (val == REG_AT || val == REG_K0 || val == REG_K1) {
                tokenError = true;
                error("Reserved register", tokenRange(lineNum, tokenStartIndex, tokenEndIndex));
            }
        }
    }

    /**
     * Parses an indirect addressing mode operand. This sets the token value to an integer with the lower 16 bits set
     * to offset, and the next 5 bits set to the register, and the upper 11 bits set to zero.
     */
    private void parseRegIndirect() {
        String tmp = token;
        Matcher match = INDIRECT.matcher(token);
        if (!match.matches()) {
            tokenType = TokenType.INDIRECT;
            tokenVal = null;
            return;
        }

        String str = match.group(1);
        int offset = (str == null) ? 0 : parseInt(str);

        token = match.group(2);
        parseRegister();
        token = tmp;
        if (tokenVal != null) {
            tokenVal = new RegIndirect(offset, (Integer)tokenVal);
        }

        tokenType = TokenType.INDIRECT;
    }


    /**
     * Parses a numeric value of current token.
     */
    private void parseNumber() {
        boolean hex = token.startsWith("0x");

        FallibleFunction<Integer, String> parsing;
        if (hex) {
            parsing = str -> parseInt(str.substring(2), HEX_RADIX);
            tokenType = HEXADECIMAL;
        } else {
            parsing = Integer::parseInt;
            tokenType = DECIMAL;
        }

        tokenVal = tryOptional(parsing, token).orElse(null);
    }

    /**
     * Parses a normal token, consisting of any valid identifiers and specified special characters. This will stop
     * when it encounters a whitespace or a pound-sign comment (#). This also allows a special case (allowing ':') if
     * the token type is identifier to account for labels. Reaching the end of the colon will then act as a word break.
     *
     * @param specialChars special characters that should also be acceptable.
     */
    private void parseToken(String specialChars) {
        tokenStartIndex = colNum;

        StringBuilder sb = new StringBuilder();
        sb.append(line.charAt(colNum));
        colNum++; //ignore the first character
        while (colNum < line.length()) {
            char ch = line.charAt(colNum);
            if (isWhitespace(ch) || ch == '#' || ch == ',') {
                break;
            } else if (!Character.isJavaIdentifierPart(ch) && specialChars.indexOf(ch) == -1) {
                tokenError = true;
                error("Illegal character.", characterRange(lineNum, colNum));
            } else {
                sb.append(ch);
            }
            colNum++;
        }
        tokenEndIndex = colNum;
        token = sb.toString();
    }

    /**
     * Parses a string token that has been demarcated by quotation marks ("). This will ensure that all valid escape
     * codes will be parsed correctly. If at any point the provided string token is malformed, this will log an error
     * and return a null string.
     */
    private void parseStringToken()
    {
        tokenStartIndex = colNum;
        if (line.charAt(colNum) != '"')
            throw new IllegalStateException("Not a string token.");

        StringBuilder ret = new StringBuilder(line.length() - colNum);
        String errorMsg = null;
        boolean quoted = false;
        boolean escaped = false;
        mainLoop: while (++colNum < line.length())
        {
            char c = line.charAt(colNum);
            if (escaped)
            {
                int charSize = 4;
                escaped = false;
                switch (c)
                {
                    case '"': ret.append('"'); break;
                    case '\'': ret.append('\''); break;
                    case '\\': ret.append('\\'); break;
                    case '0': ret.append('\000'); break;
                    case 'n': ret.append('\n'); break;
                    case 'r': ret.append('\r'); break;
                    case 't': ret.append('\t'); break;
                    case 'x':
                        charSize = 2;
                        //fall-through
                    case 'u':
                        if (line.length() - colNum - 1 < charSize) {
                            if (errorMsg == null)
                                errorMsg = "Invalid hexadecimal.";
                            continue mainLoop;
                        }
                        String point = line.substring(colNum + 1, colNum + 1 + charSize).toUpperCase();
                        for (char hex : point.toCharArray())
                        {
                            if (!Character.isLetterOrDigit(hex) || hex > 'F') {
                                if (errorMsg == null)
                                    errorMsg = "Invalid hexadecimal.";
                                continue mainLoop;
                            }
                        }
                        ret.append((char)Integer.parseInt(point, HEX_RADIX));
                        colNum += charSize;
                        break;
                    default:
                        if (errorMsg == null)
                            errorMsg = "Invalid escape code.";
                }
            }
            else if (c == '"')
            {
                quoted = true;
                colNum++;
                break;
            }
            else if (c == '\\')
                escaped = true;
            else
                ret.append(c);
        }
        if (escaped)
            errorMsg = "Unexpected end of input: open escape.";
        else if (!quoted)
            errorMsg = "Unexpected end of input: no end of quote.";

        tokenEndIndex = colNum;
        tokenType = TokenType.STRING;
        token = line.substring(tokenStartIndex, tokenEndIndex);
        if (errorMsg == null)
            tokenVal = ret.toString();
        else
        {
            tokenError = true;
            error(errorMsg, tokenRange(lineNum, tokenStartIndex, colNum));
            tokenVal = null;
        }
    }
}
