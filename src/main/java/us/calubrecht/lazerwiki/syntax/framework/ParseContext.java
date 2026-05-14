package us.calubrecht.lazerwiki.syntax.framework;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class ParseContext implements Iterable<String> {
    StringBuilder fullText;
    String nextLine;
    int lineStart = 0;
    int lineIdx = 0;
    int charIdx = 0;
    int charInLine = 0;
    List<String> lines;
    boolean readonly;

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

    public String peekLine() {
        return nextLine;
    }

    public String advanceLine() {
        lineIdx++;
        lineStart += nextLine.length() + 1;
        charIdx = lineStart;
        charInLine = 0;
        nextLine = lineIdx < lines.size() ? lines.get(lineIdx) : null;
        return nextLine;
    }

    public Character peekChar() {
        return nextLine.charAt(charInLine);
    }

    public Character advanceChar() {
        charInLine++;
        charIdx++;
        return charInLine < nextLine.length() ? nextLine.charAt(charInLine) : null;
    }

    public int getPosition() {
        return charIdx;
    }

    public boolean isEmpty() {
        return nextLine == null || lineIdx >= lines.size() || (lineIdx == lines.size() -1) && charInLine >= nextLine.length();
    }

    public void addLine(String line) {
        if (readonly) {
            throw new UnsupportedOperationException("Cannot add to a readonly ParseContext");
        }
        fullText.append('\n').append(line);
        lines.add(line);
    }

    public void setRoot(int root) {
        if (readonly) {
            throw new UnsupportedOperationException("Cannot setRoot on a readonly ParseContext");
        }
        lineIdx = root;
        charIdx = root;
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

    @Override
    public Spliterator<String> spliterator() {
        return Iterable.super.spliterator();
    }
}
