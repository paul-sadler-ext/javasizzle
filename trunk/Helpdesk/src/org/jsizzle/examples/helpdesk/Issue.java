package org.jsizzle.examples.helpdesk;

import java.util.List;
import java.util.Set;

public interface Issue
{
    enum Resolution
    {
        ERROR, BUG, ENHANCEMENT
    }

    Person getCustomer();

    Person getAnalyst();
    
    Set<? extends Issue> getReferences();

    List<? extends Note> getNotes();

    boolean isOpen();
    
    void setAnalyst(Person analyst);

    void addReference(Issue ref);

    void addNote(Person analyst, String text) throws IllegalStateException;

    void addNote(Person analyst, byte[] data) throws IllegalStateException;

    void addNote(Person analyst, Resolution resolution) throws IllegalStateException;
    
    void close() throws IllegalStateException;
}
