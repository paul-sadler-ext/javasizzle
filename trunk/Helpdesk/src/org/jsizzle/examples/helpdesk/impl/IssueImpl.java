/**
 * 
 */
package org.jsizzle.examples.helpdesk.impl;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static org.jsizzle.examples.helpdesk.impl.HelpdeskImpl.specAnalyst;
import static org.jsizzle.examples.helpdesk.impl.HelpdeskImpl.specCustomer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsizzle.AsFunction;
import org.jsizzle.Delta;
import org.jsizzle.examples.helpdesk.HelpdeskSpec;
import org.jsizzle.examples.helpdesk.Issue;
import org.jsizzle.examples.helpdesk.Note;
import org.jsizzle.examples.helpdesk.Person;

import com.google.common.collect.Lists;

class IssueImpl implements Issue
{
    private Person analyst;
    private final Person customer;
    private final List<Note> notes = new ArrayList<Note>();
    private final Set<Issue> references = new HashSet<Issue>();
    private boolean open = true;

    IssueImpl(Person analyst, Person customer)
    {
        this.analyst = analyst;
        this.customer = customer;
    }

    @Override
    public boolean isOpen()
    {
        return open;
    }

    @Override
    public Set<? extends Issue> getReferences()
    {
        return unmodifiableSet(references);
    }

    @Override
    public List<? extends Note> getNotes()
    {
        return unmodifiableList(notes);
    }

    @Override
    public Person getCustomer()
    {
        return customer;
    }

    @Override
    public Person getAnalyst()
    {
        return analyst;
    }

    @Override
    public void addReference(Issue ref)
    {
        references.add(ref);
    }

    @Override
    public void addNote(final Person analyst, final Resolution resolution)
            throws IllegalStateException
    {
        addNote(new Note.Resolution()
        {
            @Override
            public Person getAnalyst()
            {
                return analyst;
            }
            
            @Override
            public Issue.Resolution getResolution()
            {
                return resolution;
            }
        });
    }

    @Override
    public void addNote(final Person analyst, final byte[] data)
            throws IllegalStateException
    {
        addNote(new Note.Attachment()
        {
            @Override
            public Person getAnalyst()
            {
                return analyst;
            }

            @Override
            public byte[] getData()
            {
                return data;
            }
        });
    }

    @Override
    public void addNote(final Person analyst, final String text)
            throws IllegalStateException
    {
        addNote(new Note.Text()
        {
            @Override
            public Person getAnalyst()
            {
                return analyst;
            }
            
            @Override
            public String getText()
            {
                return text;
            }
        });
    }

    @Override
    public void close() throws IllegalStateException
    {
        if (notes.isEmpty() || !(notes.get(notes.size() - 1) instanceof Note.Resolution))
            throw new IllegalStateException("Cannot close unresolved issue");
        
        open = false;
    }
    
    @Override
    public void setAnalyst(Person analyst)
    {
        this.analyst = analyst;
    }

    private void addNote(final Note note)
    {
        if (!this.analyst.equals(note.getAnalyst()))
            throw new IllegalArgumentException("Only the current analyst can add notes");
        
        if (!notes.isEmpty() && notes.get(notes.size() - 1) instanceof Note.Resolution)
            throw new IllegalStateException("Cannot add notes after a resolution");
        
        notes.add(note);
    }
    
    class Instrumented implements Issue
    {
        private final HelpdeskImpl helpdesk;
        
        Instrumented(HelpdeskImpl helpdesk)
        {
            super();
            this.helpdesk = helpdesk;
        }

        @Override
        public void addNote(Person analyst, String text)
                throws IllegalStateException
        {
            final HelpdeskSpec helpdeskBefore = helpdesk.specHelpdesk();
            final HelpdeskSpec.Issue issueBefore = specIssue();
            IssueImpl.this.addNote(analyst, text);
            new HelpdeskSpec.AddIssueNote(new Delta<HelpdeskSpec>(helpdeskBefore, helpdesk.specHelpdesk()),
                                          new Delta<HelpdeskSpec.Issue>(issueBefore, specIssue()),
                                          specId(),
                                          specNote(analyst, text)).checkInvariant();
        }

        @Override
        public void addNote(Person analyst, byte[] data)
                throws IllegalStateException
        {
            final HelpdeskSpec helpdeskBefore = helpdesk.specHelpdesk();
            final HelpdeskSpec.Issue issueBefore = specIssue();
            IssueImpl.this.addNote(analyst, data);
            new HelpdeskSpec.AddIssueNote(new Delta<HelpdeskSpec>(helpdeskBefore, helpdesk.specHelpdesk()),
                                          new Delta<HelpdeskSpec.Issue>(issueBefore, specIssue()),
                                          specId(),
                                          specNote(analyst, data)).checkInvariant();
        }

        @Override
        public void addNote(Person analyst, Resolution resolution)
                throws IllegalStateException
        {
            final HelpdeskSpec helpdeskBefore = helpdesk.specHelpdesk();
            final HelpdeskSpec.Issue issueBefore = specIssue();
            IssueImpl.this.addNote(analyst, resolution);
            new HelpdeskSpec.AddIssueNote(new Delta<HelpdeskSpec>(helpdeskBefore, helpdesk.specHelpdesk()),
                                          new Delta<HelpdeskSpec.Issue>(issueBefore, specIssue()),
                                          specId(),
                                          specNote(analyst, resolution)).checkInvariant();
        }

        @Override
        public void addReference(Issue ref)
        {
            if (ref instanceof Instrumented)
                ref = ((Instrumented)ref).asIssueImpl();
            
            final HelpdeskSpec helpdeskBefore = helpdesk.specHelpdesk();
            final HelpdeskSpec.Issue issueBefore = specIssue();
            IssueImpl.this.addReference(ref);
            new HelpdeskSpec.AddIssueReference(new Delta<HelpdeskSpec>(helpdeskBefore, helpdesk.specHelpdesk()),
                                               new Delta<HelpdeskSpec.Issue>(issueBefore, specIssue()),
                                               specId(),
                                               ((IssueImpl)ref).specId()).checkInvariant();
        }

        @Override
        public void close() throws IllegalStateException
        {
            final HelpdeskSpec helpdeskBefore = helpdesk.specHelpdesk();
            final HelpdeskSpec.Issue issueBefore = specIssue();
            IssueImpl.this.close();
            new HelpdeskSpec.CloseIssue(new Delta<HelpdeskSpec>(helpdeskBefore, helpdesk.specHelpdesk()),
                                        new Delta<HelpdeskSpec.Issue>(issueBefore, specIssue()),
                                        specId()).checkInvariant();
        }

        @Override
        public Person getAnalyst()
        {
            return IssueImpl.this.getAnalyst();
        }

        @Override
        public Person getCustomer()
        {
            return IssueImpl.this.getCustomer();
        }

        @Override
        public List<? extends Note> getNotes()
        {
            return IssueImpl.this.getNotes();
        }

        @Override
        public Set<? extends Issue> getReferences()
        {
            return IssueImpl.this.getReferences();
        }

        @Override
        public boolean isOpen()
        {
            return IssueImpl.this.isOpen();
        }

        @Override
        public void setAnalyst(Person analyst)
        {
            final HelpdeskSpec helpdeskBefore = helpdesk.specHelpdesk();
            final HelpdeskSpec.Issue issueBefore = specIssue();
            IssueImpl.this.setAnalyst(analyst);
            new HelpdeskSpec.SetIssueAnalyst(new Delta<HelpdeskSpec>(helpdeskBefore, helpdesk.specHelpdesk()),
                                             new Delta<HelpdeskSpec.Issue>(issueBefore, specIssue()),
                                             specId(),
                                             specAnalyst(analyst)).checkInvariant();
        }
        
        private IssueImpl asIssueImpl()
        {
            return IssueImpl.this;
        }
    }
    
    @SuppressWarnings("unchecked")
    @AsFunction
    HelpdeskSpec.Issue specIssue()
    {
        return new HelpdeskSpec.Issue(specCustomer(getCustomer()),
                                      specAnalyst(getAnalyst()),
                                      newHashSet(transform((Set<IssueImpl>)getReferences(), specId)),
                                      newArrayList(Lists.transform(getNotes(), specNote)),
                                      isOpen() ? HelpdeskSpec.Status.OPEN : HelpdeskSpec.Status.CLOSED);
    }
    
    @AsFunction
    HelpdeskSpec.Id specId()
    {
        return new HelpdeskSpec.Id(this);
    }
    
    static HelpdeskSpec.TextNote specNote(Person analyst, String text)
    {
        return new HelpdeskSpec.TextNote(specAnalyst(analyst), text);
    }
    
    static HelpdeskSpec.AttachmentNote specNote(Person analyst, byte[] data)
    {
        return new HelpdeskSpec.AttachmentNote(specAnalyst(analyst), new HelpdeskSpec.File(Arrays.hashCode(data)));
    }
    
    static HelpdeskSpec.ResolutionNote specNote(Person analyst, Issue.Resolution resolution)
    {
        return new HelpdeskSpec.ResolutionNote(specAnalyst(analyst), HelpdeskSpec.Resolution.valueOf(resolution.name()));
    }
    
    @AsFunction
    static HelpdeskSpec.Note specNote(Note note)
    {
        if (note instanceof Note.Text)
        {
            return specNote(note.getAnalyst(), ((Note.Text)note).getText());
        }
        else if (note instanceof Note.Attachment)
        {
            return specNote(note.getAnalyst(), ((Note.Attachment)note).getData());
        }
        else if (note instanceof Note.Resolution)
        {
            return specNote(note.getAnalyst(), ((Note.Resolution)note).getResolution());
        }
        else
        {
            return null;
        }
    }
}