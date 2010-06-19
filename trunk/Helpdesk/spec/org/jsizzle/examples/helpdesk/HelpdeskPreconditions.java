package org.jsizzle.examples.helpdesk;

import java.util.Collections;

import org.jsizzle.Include;
import org.jsizzle.Prime;
import org.jsizzle.Schema;
import org.jsizzle.examples.helpdesk.HelpdeskSpec.Analyst;
import org.jsizzle.examples.helpdesk.HelpdeskSpec.Customer;
import org.jsizzle.examples.helpdesk.HelpdeskSpec.Id;
import org.jsizzle.examples.helpdesk.HelpdeskSpec.Issue;
import org.jsizzle.examples.helpdesk.HelpdeskSpec.Note;
import org.jsizzle.examples.helpdesk.HelpdeskSpec.Status;
import org.junit.Test;

public class HelpdeskPreconditions
{
    @Test
    public void preIssueInit()
    {
        new Issue.Init(new Prime<Issue>(new Issue(new Customer("Fred"),
                                                  new Analyst("Barney"),
                                                  Collections.<Id> emptySet(),
                                                  Collections.<Note> emptyList(),
                                                  Status.OPEN)),
                       new Customer("Fred"),
                       new Analyst("Barney")).checkInvariant();
    }
    
    @Schema
    class Fred
    {
        int x;
    }
    
    @Schema
    class Wilma
    {
        @Include Fred fred;
    }
}
