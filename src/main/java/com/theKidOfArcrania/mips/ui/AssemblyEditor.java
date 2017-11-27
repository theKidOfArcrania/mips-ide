package com.theKidOfArcrania.mips.ui;

import com.theKidOfArcrania.mips.highlight.*;
import com.theKidOfArcrania.mips.parsing.CodeParser;
import com.theKidOfArcrania.mips.parsing.Position;
import com.theKidOfArcrania.mips.parsing.Range;
import com.theKidOfArcrania.mips.util.RangeSet;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.theKidOfArcrania.mips.parsing.Range.characterRange;
import static java.lang.String.join;
import static java.time.Duration.ofMillis;
import static javafx.stage.Screen.getScreensForRectangle;
import static org.fxmisc.richtext.MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN;

/**
 * This class represents the user interface for the user to modify a mips assembly code contents.
 *
 * @author Henry Wang
 */
public class AssemblyEditor extends StackPane {
    /**
     * This class is used to help view the method editor by itself.
     */
    public static class AssemblyEditorViewer extends Application {

        private static final int WIDTH = 800;
        private static final int HEIGHT = 600;

        @Override
        public void start(Stage primaryStage) throws Exception {
            AssemblyEditor editor = new AssemblyEditor("");
            StackPane root = new StackPane(editor);
            StackPane.setMargin(editor, new Insets(10));
            root.getStyleClass().add("main-dialog");

            Scene scene = new Scene(root, WIDTH, HEIGHT);
            scene.getStylesheets().add("com/theKidOfArcrania/mips/ui/style.css");
            primaryStage.setScene(scene);
            primaryStage.show();
        }
    }

    /**
     * Represents all the syntax stylizing for a particular line.
     */
    private class LineStyles {
        /**
         * Represents a single line styling data.
         */
        private class LineStyle {
            private final RangeSet<HighlightMark<?>> markers;
            private volatile boolean modified;
            private int guard;
            //tag

            /**
             * Constructs a new line style
             */
            public LineStyle() {
                markers = new RangeSet<>();
                modified = true;
                guard = Integer.MAX_VALUE;
            }
        }

        private Position cursorPos;
        private final ArrayList<LineStyle> lines;

        /**
         * Creates a new line styles
         */
        public LineStyles() {
            cursorPos = new Position(1,0);
            lines = new ArrayList<>();
        }

        public void setCursorPos(Position cursorPos) {
            if (this.cursorPos.getLineNumber() < lines.size())
                lines.get(this.cursorPos.getLineNumber() - 1).modified = true;
            lines.get(cursorPos.getLineNumber() - 1).modified = true;
            this.cursorPos = cursorPos;
        }

        /**
         * Adds a style marker to this line. If this exceeds the guard length, the values will clamp out.
         *
         * @param lineNum the line number
         * @param from    the starting range
         * @param to      the ending range
         * @param style   the marker object to add.
         */
        public synchronized void addMarker(int lineNum, int from, int to, HighlightMark<?> style) {
            LineStyle line = lines.get(lineNum - 1);
            int guard = line.guard;

            if (to <= 0 || from >= guard) {
                return;
            }
            if (from < 0) {
                from = 0;
            }
            if (to > guard) {
                to = guard;
            }
            if (line.markers.add(from, to, style)) {
                line.modified = true;
            }
        }

        /**
         * Obtains a list of all line markers at a position
         *
         * @param lineNum the line number
         * @param colNum  the column number or -1 if to obtain all highlight markers.
         * @return the set of markers.
         */
        public Set<HighlightMark<?>> getMarkersAt(int lineNum, int colNum) {
            if (lineNum > lines.size() || lineNum <= 0) {
                return new HashSet<>();
            }
            if (colNum == -1) {
                synchronized (this) {
                    HashSet<HighlightMark<?>> markers = new HashSet<>();
                    for (RangeSet<HighlightMark<?>>.RangeElement e : lines.get(lineNum - 1).markers)
                        markers.addAll(e.getItems());
                    return markers;
                }
            }
            return lines.get(lineNum - 1).markers.get(colNum);
        }

        /**
         * Clears all styles for a particular line.
         *
         * @param lineNum the line number
         */
        public synchronized void clearStyles(int lineNum) {
            LineStyle line = lines.get(lineNum - 1);
            line.markers.clear();
            line.modified = true;
        }

        /**
         * Clears all tag styles for a particular line.
         *
         * @param lineNum the line number
         */
        public synchronized void clearTags(int lineNum) {
            LineStyle line = lines.get(lineNum - 1);
            if (line.markers.removeIf(mark -> mark.getType() instanceof TagType)) {
                line.modified = true;
            }
        }

        /**
         * Deletes the line at the specified line number.
         *
         * @param lineNum the line number.
         */
        public void deleteLine(int lineNum) {
            lines.remove(lineNum - 1);
        }

        /**
         * Adds the guard length for a line. This signifies where the line ends. Any indexes beyond this line will be
         * clamped at the line length.
         *
         * @param lineNum the line number to set
         * @param length  the length of the line
         */
        public synchronized void guardLine(int lineNum, int length) {
            LineStyle line = lines.get(lineNum - 1);
            line.guard = length;
            line.markers.retainRange(0, length);
        }

        /**
         * Inserts a new blank line with initially no styling.
         *
         * @param lineNum the line number to insert at.
         */
        public void insertLine(int lineNum) {
            lines.add(lineNum - 1, new LineStyle());
        }

        /**
         * Removes a particular type of style marker from a particular line.
         *
         * @param lineNum    the line number
         * @param markerType the marker type to remove
         */
        public synchronized void removeMarker(int lineNum, Enum<?> markerType) {
            LineStyle line = lines.get(lineNum - 1);
            if (line.markers.removeIf(h -> h.getType().equals(markerType))) {
                line.modified = true;
            }
        }

        /**
         * Applies all pending marker styles to the code area.
         */
        public void applyStyles() {
            if (!Platform.isFxApplicationThread()) {
                throw new IllegalStateException("Not in application FX thread");
            }

            //Compute real-time line offsets.
            int off = 0;
            for (int i = 0; i < lines.size(); i++) {
                int length = parser.getLine(i + 1).length();
                LineStyle line = lines.get(i);

                if (length < line.guard) {
                    guardLine(i + 1, length);
                }

                RangeSet<HighlightMark<?>> markers;
                synchronized(this) {
                    markers = new RangeSet<>(line.markers);
                }

                if (line.modified && line.guard > 0) {
                    line.modified = false;
                    if (cursorPos.getLineNumber() - 1 == i) {
                        findPair(i + 1, cursorPos.getColumnNumber() - 1, ')', markers);
                        findPair(i + 1, cursorPos.getColumnNumber(), '(', markers);
                    }

                    int last = 0;
                    StyleSpansBuilder<Collection<String>> ssb = new StyleSpansBuilder<>();
                    for (RangeSet<HighlightMark<?>>.RangeElement ele : markers) {
                        if (last < ele.getFrom()) {
                            ssb.add(Collections.emptyList(), ele.getFrom() - last);
                        }

                        last = ele.getTo();
                        ssb.add(ele.getItems().stream().map(HighlightMark::getType).map(Enum::toString)
                                .collect(Collectors.toSet()), last - ele.getFrom());
                    }

                    if (last < line.guard) {
                        ssb.add(Collections.emptyList(), line.guard - last);
                    }
                    codeArea.setStyleSpans(off, ssb.create());
                }
                off += parser.getLine(i + 1).length() + 1;
            }
        }

        /**
         * Finds the corresponding pair of parenthesis starting at the specified index, and adds the appropriate
         * color styling to the range set
         * @param lineNum the line number to start from
         * @param ind     the column index to start from
         * @param search  the character, either close or open parenthesis to match.
         * @param styles  the range set of styles
         */
        private void findPair(int lineNum, int ind, char search, RangeSet<HighlightMark<?>> styles) {
            String line = parser.getLine(lineNum);
            if (ind >= 0 && ind < line.length()) {
                int depth = 0;
                char ch = line.charAt(ind);
                if (ch != search)
                    return;

                int i = ind;
                do {
                    if (line.charAt(i) == '(')
                        depth++;
                    else if (line.charAt(i) == ')')
                        depth--;
                    if (depth == 0)
                        break;
                    i += ch == '(' ? 1 : -1;
                } while (i >= 0 && i < line.length());

                if (depth == 0) {
                    styles.add(ind, ind + 1, new Syntax(SyntaxType.PPAIR, characterRange(lineNum, ind)));
                    styles.add(i, i + 1, new Syntax(SyntaxType.PPAIR, characterRange(lineNum, i)));
                } else {
                    styles.add(ind, ind + 1, new Syntax(SyntaxType.PBROKEN, characterRange(lineNum, ind)));
                }
            }
        }
    }

    private static final Duration MOUSE_OVER_DELAY = ofMillis(200);
    private static final Duration PARSE_DELAY = ofMillis(300);
    private static final int MOVE_TOOLTIP_RANGE = 10;

    /**
     * Helper method that chains a parameter object with an action
     * @param param   the parameter to chain
     * @param running the function to run with this object
     * @param <P>     the parameter object type
     * @return the parameter itself
     */
    private static <P> P chain(P param, Consumer<P> running) {
        running.accept(param);
        return param;
    }

    private final List<Tag> highlightTags;
    private final List<Syntax> highlightSyntaxes;
    private final LineStyles styles;

    private final ArrayList<Integer> linePos;
    private final CodeParser parser;
    private final CodeArea codeArea;
    private final Tooltip tagMsg;

    private int moveCount = 10;


    /**
     * Constructs a new method editor object.
     *
     * @param code the associated code from this method.
     */
    public AssemblyEditor(String code) {
        getStylesheets().add("com/theKidOfArcrania/mips/ui/syntax-def.css");
        getStyleClass().add("method-editor");

        highlightSyntaxes = new ArrayList<>();
        highlightTags = new ArrayList<>();
        styles = new LineStyles();

        ExecutorService executor = Executors.newFixedThreadPool(1, r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
        parser = new CodeParser("", new Highlighter() {

            @Override
            public void insertTag(Tag tag) {
                highlightTags.add(tag);
            }

            @Override
            public void insertSyntax(Syntax syn) {
                highlightSyntaxes.add(syn);
            }
        });

        linePos = new ArrayList<>();
        linePos.add(0);
        styles.insertLine(1);

        tagMsg = new Tooltip();
        tagMsg.setWrapText(true);
        //tagMsg.getStyleClass().add("tag-label");

        codeArea = new CodeArea();
        codeArea.setUseInitialStyleForInsertion(false);
        codeArea.plainTextChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .map(this::computeChanges)
                .reduceSuccessions((a, b) -> b, PARSE_DELAY)
                .mapToTask(t -> chain(t, executor::execute))
                .await()
                .subscribe(res -> res.ifFailure(Throwable::printStackTrace));
        codeArea.caretPositionProperty().addListener(val -> {
            int pos = codeArea.getCaretPosition();
            int line = searchLine(pos);
            int column = pos - linePos.get(line - 1);
            styles.setCursorPos(new Position(line, column));
            styles.applyStyles();
        });
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea)); //TODO: line number factory + tag id.
        codeArea.setMouseOverTextDelay(MOUSE_OVER_DELAY);
        codeArea.addEventHandler(MOUSE_OVER_TEXT_BEGIN, e -> {
            int chIdx = e.getCharacterIndex();
            Point2D pos = e.getScreenPosition();

            int lineNum = searchLine(chIdx);
            int colNum = chIdx - linePos.get(lineNum - 1);

            showTagMsgs(pos, lineNum, colNum);
        });

        codeArea.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            if (moveCount == -1) {
                return;
            }
            moveCount++;
            if (moveCount >= MOVE_TOOLTIP_RANGE) {
                tagMsg.hide();
                moveCount = -1;
            }
        });


        VirtualizedScrollPane<CodeArea> scroll = new VirtualizedScrollPane<>(codeArea);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        getChildren().addAll(scroll);

        codeArea.insertText(0, code);
    }

    /**
     * Shows all the tag messages at a particular location.
     *
     * @param pos     position of the cursor.
     * @param lineNum the line number corresponding to cursor
     * @param colNum  the column number corresponding to cursor, or -1 if at beginning of line.
     */
    private void showTagMsgs(Point2D pos, int lineNum, int colNum) {
        ArrayList<String> lines = new ArrayList<>();
        for (HighlightMark<?> mark : styles.getMarkersAt(lineNum, colNum)) {
            if (mark instanceof Tag) {
                lines.add(((Tag) mark).getTagDescription());
            }
        }
        if (!lines.isEmpty()) {
            double xPos = pos.getX();
            double yPos = pos.getY() + 5;
            Rectangle2D bounds = getScreensForRectangle(xPos, yPos, 0, 0).get(0).getVisualBounds();

            tagMsg.setText(join("\n", lines));
            tagMsg.setMaxWidth(bounds.getWidth());

            xPos -= tagMsg.prefWidth(-1) / 2;
            tagMsg.show(codeArea, xPos, yPos);
            moveCount = 0;
        }
    }

    /**
     * This processes the resulting line styles (syntax highlighting and tags) that have been emitted by our code
     * parser into our line styles object. Note: THIS METHOD IS NOT THREAD-SAFE, AND THE CALLER METHOD SHOULD ALREADY
     * MAKE SYNCHRONIZED LOCKS
     * @param cancelled the atomic boolean prop to check if task is cancelled
     */
    @SuppressWarnings("unchecked")
    private void processLineStyles(AtomicBoolean cancelled) {
        if (cancelled.get())
            return;

        highlightSyntaxes.clear();
        highlightTags.clear();
        if (parser.reparse(false, cancelled))
            parser.resolveSymbols(cancelled);

        if (cancelled.get())
            return;

        List<HighlightMark> markList = new ArrayList<>(highlightSyntaxes);
        markList.addAll(highlightTags);

        //Compute real-time line offsets.
        String[] lines = codeArea.getText().split("\n");
        int[] offsets = new int[lines.length];
        int ind = 0;
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = ind;
            ind += lines[i].length() + 1;
        }

        if (cancelled.get())
            return;

        //Compute all the highlighting
        for (int i = 0; i < lines.length; i++)
            styles.guardLine(i + 1, lines[i].length());

        boolean[] invalidated = new boolean[lines.length];
        for (HighlightMark mark : markList) {
            Range span = mark.getSpan();
            int startLine = span.getStart().getLineNumber();
            int endLine = span.getEnd().getLineNumber();
            for (int line = startLine; line <= endLine; line++) {
                if (!invalidated[line - 1]) {
                    for (TagType type : TagType.values())
                        styles.removeMarker(line, type);
                    invalidated[line - 1] = true;
                }
                int start = line == startLine ? span.getStart().getColumnNumber() : 0;
                int end = line == endLine ? span.getEnd().getColumnNumber() : lines[line - 1].length();
                styles.addMarker(line, start, end, mark);
            }
        }
    }

    /**
     * Recomputes with the change listed, re-parses the affected lines, and reanalyzes the code for symbolic errors.
     *
     * @param change the change that occurred
     * @return the pending computing task.
     */
    private synchronized Task<Void> computeChanges(PlainTextChange change) {
        //Update change
        if (!change.getRemoved().isEmpty()) {
            removeRange(change.getRemoved(), change.getPosition());
        }
        if (!change.getInserted().isEmpty()) {
            insertRange(change.getInserted(), change.getPosition());
        }

        //Remove styles for the dirty lines
        for (int i = 1; i <= parser.getLineCount(); i++) {
            if (parser.isLineDirty(i)) {
                styles.clearStyles(i);
            } else if (!parser.isLineMalformed(i)) {
                styles.clearTags(i);
            }
        }

//        System.out.println("***");
//        for (int i = 0; i < parser.getLineCount(); i++)
//            System.out.println(parser.getLine(i + 1));
//        System.out.println("---");

        //Execute parsing as a separate task
        AtomicBoolean cancelled = new AtomicBoolean(false);
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                codeArea.plainTextChanges().subscribeForOne(chg -> this.cancel());
                synchronized (AssemblyEditor.this) {
                    processLineStyles(cancelled);
                    if (!isCancelled()) {
                        Platform.runLater(styles::applyStyles);
                    }
                    return null;
                }
            }

            @Override
            protected void cancelled() {
                cancelled.set(true);
            }
        };
    }

    /**
     * Inserts a range of code from a specified position.
     *
     * @param added    the text added.
     * @param position the position to start adding from.
     */
    private void insertRange(String added, int position) {
        int length = added.length();
        int firstLineNum = searchLine(position);
        String[] lines = added.split("\n", -1);

        //Modify the first line where we start adding stuff.
        int headOffset = position - linePos.get(firstLineNum - 1);
        String firstLine = parser.getLine(firstLineNum);
        String modLine;
        if (headOffset < firstLine.length()) {
            modLine = firstLine.substring(0, headOffset) + lines[0];
        } else {
            modLine = firstLine + lines[0];
        }
        parser.modifyLine(firstLineNum, modLine);

        //Add subsequent lines
        int pos = linePos.get(firstLineNum - 1) + modLine.length() + 1;
        for (int i = 1; i < lines.length; i++) {
            parser.insertLine(firstLineNum + i, lines[i]);
            styles.insertLine(firstLineNum + i);
            linePos.add((firstLineNum - 1) + i, pos);
            pos += lines[i].length() + 1;
        }

        //Modify last line in parser
        String tail = firstLine.substring(headOffset);
        if (!tail.isEmpty()) {
            int lastLineNum = firstLineNum + lines.length - 1;
            String lastLine = parser.getLine(lastLineNum);
            parser.modifyLine(lastLineNum, lastLine + tail);
        }

        //Move down the line position of subsequent untouched lines.
        for (int i = (firstLineNum - 1) + lines.length; i < linePos.size(); i++)
            linePos.set(i, linePos.get(i) + length);
    }

    /**
     * Removes a range of code from a specified position.
     *
     * @param removed  the text removed.
     * @param position the position to start removing from.
     */
    private void removeRange(String removed, int position) {
        int length = removed.length();
        int firstLineNum = searchLine(position);
        int removedLines = countLines(removed) - 1;

        //Modify the first line where we start deleting stuff.
        int headOffset = position - linePos.get(firstLineNum - 1);
        String firstLine = parser.getLine(firstLineNum);
        String modLine = firstLine;
        if (headOffset < firstLine.length()) {
            modLine = firstLine.substring(0, headOffset);
        }

        //Append any trailing text after removal range
        int lastLineNum = firstLineNum + removedLines;
        int tailOffset = (position + length) - linePos.get(lastLineNum - 1);
        String lastLine = parser.getLine(lastLineNum);
        if (tailOffset < lastLine.length()) {
            modLine += lastLine.substring(tailOffset);
        }

        //Modify the line
        if (!modLine.equals(firstLine)) {
            parser.modifyLine(firstLineNum, modLine);
        }

        //Delete subsequent lines.
        for (int i = 0; i < removedLines; i++) {
            if (linePos.size() >= firstLineNum) {
                parser.deleteLine(firstLineNum + 1);
                styles.deleteLine(firstLineNum + 1);
                linePos.remove(firstLineNum);
            } else {
                System.err.println("Unable to remove line position.");
            }
        }

        //Move up the line position of subsequent untouched lines.
        for (int i = firstLineNum; i < linePos.size(); i++)
            linePos.set(i, linePos.get(i) - length);
    }

    /**
     * Counts the number of lines this string will span.
     *
     * @param str the string to count
     * @return the number of lines.
     */
    private int countLines(String str) {
        int lines = 1;
        for (char c : str.toCharArray()) {
            if (c == '\n')
                lines++;
        }
        return lines;
    }

    /**
     * Using a modified binary search algorithm, searches for the line number of this character position.
     *
     * @param pos the position to search line number
     * @return the respective line number. (1-based)
     */
    private int searchLine(int pos) {
        int low = 0;
        int high = linePos.size() - 1;

        while (low < high) {
            int mid = (high + low) / 2;
            int cmp = pos - linePos.get(mid);
            if (cmp > 0) {
                low = mid + 1;
            } else if (cmp < 0) {
                high = mid - 1;
            } else { //if (cmp == 0)
                return mid + 1;
            }
        }
        return pos >= linePos.get(high) ? high + 1 : high;
    }
}
