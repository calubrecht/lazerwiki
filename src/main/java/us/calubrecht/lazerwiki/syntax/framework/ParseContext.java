package us.calubrecht.lazerwiki.syntax.framework;

import org.jetbrains.annotations.NotNull;

import java.nio.CharBuffer;
import java.util.*;
import java.util.function.Consumer;

public class ParseContext implements Iterable<String> {
    final StringBuilder fullText;
    String nextLine;
    int lineStart = 0;
    int lineIdx = 0;
    int charIdx = 0;
    int charInLine = 0;
    final List<String> lines;
    boolean readonly;
    int rootIdx = 0;
    ParseContext rootContext = this;

    public ParseContext(String fullText) {
        this.fullText = new StringBuilder(fullText);
        lines = fullText.lines().toList();
        nextLine = lines.get(0);
        readonly = true;
    }

    public ParseContext() {
        readonly = false;
        lines = new ArrayList<>();
        fullText = new StringBuilder();
    }

    public ParseContext(String fullText, int rootPosition, ParseContext rootContext) {
        this.fullText = new StringBuilder(fullText);
        lines = fullText.lines().toList();
        nextLine = lines.get(0);
        rootIdx = rootPosition;
        this.rootContext = rootContext.getRootContext();
        readonly = true;
    }

    /**
     * Clone the context, shares rootContext and text and starts at same position, but does not move
     * root parser;
     * @param rootContext
     */
    public ParseContext(ParseContext rootContext) {
        this.fullText = new StringBuilder(rootContext.fullText);
        nextLine = rootContext.nextLine;
        lineStart = rootContext.lineStart;
        lineIdx = rootContext.lineIdx;
        charIdx = rootContext.charIdx;
        charInLine = rootContext.charInLine;
        lines = new ArrayList<>(rootContext.lines);
        readonly = true;
        rootIdx = rootContext.rootIdx;
        this.rootContext = rootContext.getRootContext();
    }

    public String peekLine() {
        return nextLine;
    }

    public String remainingLine() {
        return nextLine.substring(charInLine);
    }

    public void advanceLine() {
        if (isEmpty()) {
            return;
        }
        lineIdx++;
        lineStart += nextLine.length() + 1;
        charIdx = lineStart;
        charInLine = 0;
        nextLine = lineIdx < lines.size() ? lines.get(lineIdx) : null;
    }

    public Character peekChar() {
        if (charInLine == nextLine.length()) {
            return '\n';
        }
        return nextLine.charAt(charInLine);
    }

    /** Checks if the sequence at the current location starts with the given string.
     * Does not support newlines
     */
    public boolean remainingStartsWith(String s) {
       for (int i = 0; i < s.length(); i++) {
           if (charInLine + i >= nextLine.length()) {
               // end of line with no match
               return false;
           }
           char parseChar = nextLine.charAt(charInLine + i);
           if (parseChar != s.charAt(i)) {
               return false;
           }

       }
       return true;
    }

    public void advanceChar() {
        if (charInLine == nextLine.length()) {
            advanceLine();
            return;
        }
        charInLine++;
        charIdx++;
    }

    public void advanceChars(int number) {
        for (int i = 0 ; i < number; i++) {
            advanceChar();
        }
    }

    public int getPosition() {
        if (charIdx >= fullText.length()) {
           // Handle end of string issues
           return rootIdx + fullText.length() -1;
        }
        return charIdx + rootIdx;
    }

    public boolean isEmpty() {
        return nextLine == null || (lineIdx == lines.size() -1) && charInLine >= nextLine.length();
    }

    public void addLine(String line) {
        if (readonly) {
            throw new UnsupportedOperationException("Cannot add to a readonly ParseContext");
        }
        if (!fullText.isEmpty()) {
            fullText.append('\n');
        }
        fullText.append(line);
        lines.add(line);
        nextLine = lines.get(0);
    }

    public void setRoot(ParseContext root, int rootPosition) {
        if (readonly) {
            throw new UnsupportedOperationException("Cannot setRoot on a readonly ParseContext");
        }
        rootContext = root.getRootContext();
        rootIdx = rootPosition;
    }

    public ParseContext getRootContext() {
        return rootContext;
    }

    public void lock() {
        readonly = true;
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return lines.iterator();
    }

    @Override
    public void forEach(Consumer<? super String> action) {
        Iterable.super.forEach(action);
    }

    public CharSequence subsequence() {
       return CharBuffer.wrap(fullText).subSequence(charIdx, fullText.length());
    }

    @Override
    public String toString() {
        return nextLine;
    }

    public String getFullText() {
        return fullText.toString();
    }
}
