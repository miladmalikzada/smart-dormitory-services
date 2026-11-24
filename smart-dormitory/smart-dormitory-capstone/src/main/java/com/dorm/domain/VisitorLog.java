package com.dorm.domain;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Iterator pattern (L6): the log can be traversed in two ways without exposing its list:
 * all entries (for-each) or only visitors still inside ({@link #inside()}).
 */
public class VisitorLog implements Iterable<VisitorEntry> {
    private final List<VisitorEntry> entries;

    public VisitorLog(List<VisitorEntry> entries) { this.entries = List.copyOf(entries); }

    @Override public Iterator<VisitorEntry> iterator() { return entries.iterator(); }

    public Iterable<VisitorEntry> inside() { return () -> new InsideIterator(entries.iterator()); }

    private static final class InsideIterator implements Iterator<VisitorEntry> {
        private final Iterator<VisitorEntry> source;
        private VisitorEntry next;

        InsideIterator(Iterator<VisitorEntry> source) { this.source = source; advance(); }

        private void advance() {
            next = null;
            while (source.hasNext()) {
                VisitorEntry e = source.next();
                if (e.isInside()) { next = e; return; }
            }
        }

        @Override public boolean hasNext() { return next != null; }

        @Override public VisitorEntry next() {
            if (next == null) throw new NoSuchElementException();
            VisitorEntry result = next;
            advance();
            return result;
        }
    }
}
