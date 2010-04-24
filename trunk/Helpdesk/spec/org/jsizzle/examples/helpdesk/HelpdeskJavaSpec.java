package org.jsizzle.examples.helpdesk;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.getLast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// To animate, would need client to manage identity, or implement equals/hashCode
// Cannot tie to implementation because cannot instantiate intermediate state
public class HelpdeskJavaSpec
{
    public static class Customer {}
    public static class Analyst {}
    
    enum Status { OPEN, CLOSED }
    enum Resolution { ERROR, BUG, ENHANCEMENT }
    
    public static abstract class Note
    {
        public final Analyst analyst;

        private Note(Analyst analyst)
        {
            this.analyst = analyst;
        }
    }
    
    public static class TextNote extends Note
    {
        public final String text;

        private TextNote(Analyst analyst, String text)
        {
            super(analyst);
            this.text = text;
        }
    }
    
    public static class AttachmentNote extends Note
    {
        public final byte[] data;

        private AttachmentNote(Analyst analyst, byte[] data)
        {
            super(analyst);
            this.data = data;
        }
    }
    
    public static class ResolutionNote extends Note
    {
        public final Resolution resolution;

        private ResolutionNote(Analyst analyst, Resolution resolution)
        {
            super(analyst);
            this.resolution = resolution;
        }
    }
    
    // Issue is not static so we can manage helpdesk state
    public class Issue
    {
        private final Customer customer;
        private Analyst analyst;
        private Status status;
        private final List<Note> notes;
        private final Set<Issue> references;
        
        public Issue(Customer customer, Analyst analyst)
        {
            assert customer != null;
            assert analyst != null;
            
            this.customer = customer;
            this.analyst = analyst;
            this.status = Status.OPEN;
            this.notes = new ArrayList<Note>();
            this.references = new HashSet<Issue>();
            
            issues.add(this);
            
            // Not clear how to do invariants
        }
        
        public void setAnalyst(Analyst analyst)
        {
            assert analyst != null;
            
            this.analyst = analyst;
        }
        
        public void addNote(Note note)
        {
            assert note != null;
            assert note.analyst.equals(analyst);
            assert !any(notes, instanceOf(ResolutionNote.class)); // Precondition enforcing invariant
            
            notes.add(note);
        }
        
        public void addReference(Issue reference)
        {
            assert reference != null;
            assert reference != this; // Assuming entity reference pattern
            assert issues.contains(reference);
            
            references.add(reference);
        }
        
        public void close()
        {
            assert getLast(notes) instanceof ResolutionNote; // Precondition enforcing invariant
            
            status = Status.CLOSED;
        }

        // Getters necessary to avoid mutation for non-final fields
        public Customer getCustomer()
        {
            return customer;
        }

        public Analyst getAnalyst()
        {
            return analyst;
        }

        public Status getStatus()
        {
            return status;
        }
    }

    private final Set<Issue> issues;
    
    public HelpdeskJavaSpec()
    {
        this.issues = new HashSet<Issue>(); // Assuming entity reference pattern
    }
    
    public List<Issue> analystIssues(Analyst analyst)
    {
        // Procedural postcondition
        final List<Issue> analystIssues = new ArrayList<Issue>();
        for (Issue issue : issues)
        {
            if (issue.getAnalyst() == analyst && issue.getStatus() == Status.OPEN)
                analystIssues.add(issue);
        }
        return analystIssues;
    }
}
