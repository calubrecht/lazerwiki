package us.calubrecht.lazerwiki.syntax.framework;

import org.jetbrains.annotations.NotNull;

import java.nio.CharBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static com.ibm.icu.text.PluralRules.Operand.i;

public class ParseContext implements Iterable<String> {
    StringBuilder fullText;
    String nextLine;
    int lineStart = 0;
    int lineIdx = 0;
    int charIdx = 0;
    int charInLine = 0;
    List<String> lines;
    boolean readonly;
    int rootIdx = 0;

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

    public ParseContext(String fullText, int rootPosition) {
        this.fullText = new StringBuilder(fullText);
        lines = fullText.lines().toList();
        nextLine = lines.get(0);
        rootIdx = rootPosition;
        readonly = true;
    }

    public String peekLine() {
        return nextLine;
    }

    public void advanceLine() {
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
        return charIdx + rootIdx;
    }

    public boolean isEmpty() {
        return nextLine == null || lineIdx >= lines.size() || (lineIdx == lines.size() -1) && charInLine >= nextLine.length();
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

    public void setRoot(int rootPosition) {
        if (readonly) {
            throw new UnsupportedOperationException("Cannot setRoot on a readonly ParseContext");
        }
        rootIdx = rootPosition;
    }

    public void lock() {
        readonly = true;
    }

    // XXX: remove this
    public List<String> getLines() {
        return lines;
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

    @Override
    public Spliterator<String> spliterator() {
        return Iterable.super.spliterator();
    }

    public CharSequence subsequence() {
       return CharBuffer.wrap(fullText).subSequence(charIdx, fullText.length());
    }

    @Override
    public String toString() {
        return nextLine;
    }
}
