package org.jsizzle.examples.helpdesk.impl;

import org.jsizzle.examples.helpdesk.Issue;
import org.jsizzle.examples.helpdesk.Person;
import org.junit.Test;


public class HelpdeskImplTest
{
    private final HelpdeskImpl.Instrumented helpdesk =
        new HelpdeskImpl().new Instrumented();

    /* When a customer calls in to the support hotline, an analyst
     * will create an open issue in the system for that customer. The
     * issue is initially assigned to the analyst that created it. */
    @Test public void addIssue()
    {
        helpdesk.addIssue(helpdesk.addCustomer("Fred"),
                          helpdesk.addAnalyst("Barney"));
    }

    /* It must be possible to assign the issue to another analyst. */
    @Test public void reassignIssue()
    {
        final Issue issue = helpdesk.addIssue(
                                helpdesk.addCustomer("Fred"),
                                helpdesk.addAnalyst("Barney"));
        issue.setAnalyst(helpdesk.addAnalyst("Wilma"));
    }

    /* A number of different notes can be added to the issue as the
     * analyst works through it. */
    @Test public void addDifferentNotes()
    {
        final Person barney = helpdesk.addAnalyst("Barney");
        final Issue issue = helpdesk.addIssue(
                                helpdesk.addCustomer("Fred"), barney);
        issue.addNote(barney, "Note1");
        issue.addNote(barney, "Note2");
        issue.addNote(barney, "Note3");
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotAddNoteFromWrongAnalyst()
    {
        final Issue issue = helpdesk.addIssue(
                                helpdesk.addCustomer("Fred"),
                                helpdesk.addAnalyst("Barney"));
        issue.addNote(helpdesk.addAnalyst("Wilma"), "Note1");
    }

    /* An issue can be cross-referenced to a number of other issues if the
     * analyst finds out that they are related. */
    @Test public void crossReferenceIssue()
    {
        final Person fred = helpdesk.addCustomer("Fred");
        final Person barney = helpdesk.addAnalyst("Barney");
        final Issue issue1 = helpdesk.addIssue(fred, barney);
        final Issue issue2 = helpdesk.addIssue(fred, barney);
        final Issue issue3 = helpdesk.addIssue(fred, barney);
        issue1.addReference(issue2);
        issue1.addReference(issue3);
    }

    /* There are several types of note.
     * - A text note allows the analyst to enter free text only.
     * - An attachment note allows the analyst to attach any file.
     * - A resolution note allows the analyst to record a reason of “user
     * error”, “bug” or “enhancement” against the issue. */
    @Test public void addAllNoteTypes()
    {
        final Person barney = helpdesk.addAnalyst("Barney");
        final Issue issue = helpdesk.addIssue(helpdesk.addCustomer("Fred"), barney);
        issue.addNote(barney, "Note1");
        issue.addNote(barney, new byte[] {0});
        issue.addNote(barney, Issue.Resolution.BUG);
    }
    
    /* A resolution note... should be the last note added to the issue. */
    @Test(expected = IllegalStateException.class) public void cannotAddNoteAfterResolution()
    {
        final Person barney = helpdesk.addAnalyst("Barney");
        final Issue issue = helpdesk.addIssue(helpdesk.addCustomer("Fred"), barney);
        issue.addNote(barney, Issue.Resolution.BUG);
        issue.addNote(barney, "Note1");
    }
    
    /* Once an issue is resolved, it can be closed. */
    @Test public void closeResolvedIssue()
    {
        final Person barney = helpdesk.addAnalyst("Barney");
        final Issue issue = helpdesk.addIssue(helpdesk.addCustomer("Fred"), barney);
        issue.addNote(barney, Issue.Resolution.BUG);
        issue.close();
    }

    @Test(expected = IllegalStateException.class) public void cannotCloseUnresolvedIssue()
    {
        final Person barney = helpdesk.addAnalyst("Barney");
        final Issue issue = helpdesk.addIssue(helpdesk.addCustomer("Fred"), barney);
        issue.close();
    }
    
    /* A list of the issues that an analyst currently has open. */
    @Test public void reportOpenAnalystIssues()
    {
        final Person barney = helpdesk.addAnalyst("Barney");
        final Person fred = helpdesk.addCustomer("Fred");
        helpdesk.addIssue(fred, helpdesk.addAnalyst("Wilma"));
        final Issue issue2 = helpdesk.addIssue(fred, barney);
        issue2.addNote(barney, Issue.Resolution.BUG);
        issue2.close();
        helpdesk.addIssue(fred, barney);
        helpdesk.getAnalystOpenIssues(barney);
    }
}
