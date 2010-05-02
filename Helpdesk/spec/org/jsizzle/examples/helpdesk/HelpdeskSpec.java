package org.jsizzle.examples.helpdesk;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.compose;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Sets.union;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.jsizzle.ValueObjects.list;
import static org.jsizzle.ValueObjects.override;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsizzle.Delta;
import org.jsizzle.Include;
import org.jsizzle.Invariant;
import org.jsizzle.Prime;
import org.jsizzle.Schema;
import org.jsizzle.Xi;

@Schema
class HelpdeskSpec
{
    class Customer {}
    class Analyst {}
    class File {}
    class Id {}
    
    enum Status { OPEN, CLOSED }
    enum Resolution { ERROR, BUG, ENHANCEMENT }
    
    interface Note
    {
        Analyst getAnalyst();
    }
    
    class AbstractNote
    {
        Analyst analyst;
    }
    
    class TextNote implements Note
    {
        @Include AbstractNote note;
        String text;
    }
    
    class AttachmentNote implements Note
    {
        @Include AbstractNote note;
        File file;
    }
    
    class ResolutionNote implements Note
    {
        @Include AbstractNote note;
        Resolution resolution;
    }
    
    Map<Id, Issue> issues;
    
    class Issue
    {
        Customer customer;
        Analyst analyst;
        Set<Id> references;
        List<Note> notes;
        Status status;
        
        @Invariant boolean mustHaveResolutionToClose()
        {
            return status == Status.OPEN || any(notes, instanceOf(ResolutionNote.class));
        }
        
        @Invariant boolean resolutionIsLastNote()
        {
            final List<Note> resNotes = list(filter(notes, instanceOf(ResolutionNote.class)));
            return resNotes.isEmpty() || resNotes.equals(singletonList(getLast(notes)));
        }
        
        class Init
        {
            Prime<Issue> issue;
            Customer customer;
            Analyst analyst;
            
            @Invariant boolean customerAndAnalystAssignedAndStatusOpen()
            {
                return issue.after.customer.equals(customer) &&
                       issue.after.analyst.equals(analyst) &&
                       issue.after.references.isEmpty() &&
                       issue.after.notes.isEmpty() &&
                       issue.after.status == Status.OPEN;
            }
        }
        
        class SetAnalyst
        {
            Delta<Issue> issue;
            Analyst analyst;
            
            @Invariant boolean analystSet()
            {
                return issue.after.customer.equals(issue.before.customer) &&
                       issue.after.analyst.equals(analyst) &&
                       issue.after.references.equals(issue.before.references) &&
                       issue.after.notes.equals(issue.before.notes) &&
                       issue.after.status.equals(issue.before.status);
            }
        }
        
        class AddNote
        {
            Delta<Issue> issue;
            Note note;
            
            @Invariant boolean mustBeCurrentAnalyst()
            {
                return issue.before.analyst.equals(note.getAnalyst());
            }
            
            @Invariant boolean noteAdded()
            {
                return issue.after.customer.equals(issue.before.customer) &&
                       issue.after.analyst.equals(issue.before.analyst) &&
                       issue.after.references.equals(issue.before.references) &&
                       issue.after.notes.equals(list(concat(issue.before.notes, singleton(note)))) &&
                       issue.after.status.equals(issue.before.status);
            }
        }
        
        class AddReference
        {
            Delta<Issue> issue;
            Id reference;
            
            @Invariant boolean referenceAdded()
            {
                return issue.after.customer.equals(issue.before.customer) &&
                       issue.after.analyst.equals(issue.before.analyst) &&
                       issue.after.references.equals(union(issue.before.references, singleton(reference))) &&
                       issue.after.notes.equals(issue.before.notes) &&
                       issue.after.status.equals(issue.before.status);
            }
        }
        
        class Close
        {
            Delta<Issue> issue;
            
            @Invariant boolean closed()
            {
                return issue.after.customer.equals(issue.before.customer) &&
                       issue.after.analyst.equals(issue.before.analyst) &&
                       issue.after.references.equals(issue.before.references) &&
                       issue.after.notes.equals(issue.before.notes) &&
                       issue.after.status.equals(Status.CLOSED);
            }
        }
    }
    
    class PromoteIssue
    {
        Delta<HelpdeskSpec> helpdesk;
        Delta<Issue> issue;
        Id id;
        
        @Invariant boolean issuesUpdated()
        {
            return helpdesk.after.issues.equals(override(helpdesk.before.issues, singletonMap(id, issue.after)));
        }
    }
    
    class PromoteExistingIssue
    {
        @Include PromoteIssue promoteIssue;
        
        @Invariant boolean mustBeExistingIssue()
        {
            return helpdesk.before.issues.containsKey(id) &&
                   helpdesk.before.issues.get(id).equals(issue.before);
        }
    }

    class CreateIssue
    {
        @Include Issue.Init initIssue;
        @Include PromoteIssue promoteIssue;
        
        @Invariant boolean mustBeNewIssue()
        {
            return !helpdesk.before.issues.containsKey(id);
        }
    }
    
    class SetIssueAnalyst
    {
        @Include PromoteExistingIssue promoteIssue;
        @Include Issue.SetAnalyst setAnalyst;
    }
    
    class AddIssueNote
    {
        @Include PromoteExistingIssue promoteIssue;
        @Include Issue.AddNote addNote;
    }
    
    /**
     * Note that Issue references are not mutual.
     */
    class AddIssueReference
    {
        @Include PromoteExistingIssue promoteIssue;
        @Include Issue.AddReference addReference;
        
        @Invariant boolean referenceExists()
        {
            return helpdesk.before.issues.containsKey(reference);
        }
        
        @Invariant boolean referenceNotCircular()
        {
            return !id.equals(reference);
        }
    }
    
    class CloseIssue
    {
        @Include PromoteExistingIssue promoteIssue;
        @Include Issue.Close close;
    }
    
    class ReportIssuesForAnalyst
    {
        Xi<HelpdeskSpec> helpdesk;
        List<Issue> analystIssues;
        Analyst analyst;

        @Invariant boolean analystOpenIssuesReported()
        {
            return analystIssues.equals(list(filter(helpdesk.before.issues.values(),
                                                    and(compose(equalTo(analyst), Issue.getAnalyst),
                                                        compose(equalTo(Status.OPEN), Issue.getStatus)))));
        }
    }
}
