package org.jsizzle.examples.helpdesk.impl;

import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.transformValues;
import static com.google.common.collect.Maps.uniqueIndex;
import static org.jsizzle.examples.helpdesk.impl.IssueImpl.specId;
import static org.jsizzle.examples.helpdesk.impl.IssueImpl.specIssue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jsizzle.Delta;
import org.jsizzle.Prime;
import org.jsizzle.Xi;
import org.jsizzle.examples.helpdesk.Helpdesk;
import org.jsizzle.examples.helpdesk.HelpdeskSpec;
import org.jsizzle.examples.helpdesk.Issue;
import org.jsizzle.examples.helpdesk.Person;

import com.google.common.collect.Lists;

public class HelpdeskImpl implements Helpdesk
{
    private final Collection<IssueImpl> issues = new ArrayList<IssueImpl>();
    
    @Override
    public Person addAnalyst(final String name)
    {
        return createPerson(name);
    }

    @Override
    public Person addCustomer(String name)
    {
        return createPerson(name);
    }

    @Override
    public Issue addIssue(final Person customer, final Person analyst)
    {
        final IssueImpl issue = new IssueImpl(analyst, customer);
        issues.add(issue);
        return issue;
    }

    @Override
    public List<? extends Issue> getAnalystOpenIssues(Person analyst)
    {
        final ArrayList<Issue> analystOpenIssues = Lists.newArrayList();
        for (Issue issue : issues)
        {
            if (analyst.equals(issue.getAnalyst()) && issue.isOpen())
                analystOpenIssues.add(issue);
        }
        return analystOpenIssues;
    }

    protected Person createPerson(final String name)
    {
        return new Person()
        {
            @Override
            public String getName()
            {
                return name;
            }
        };
    }
    
    class Instrumented implements Helpdesk
    {
        @Override
        public Person addAnalyst(String name)
        {
            specHelpdesk().checkInvariant();
            try
            {
                return HelpdeskImpl.this.addAnalyst(name);
            }
            finally
            {
                specHelpdesk().checkInvariant();
            }
        }

        @Override
        public Person addCustomer(String name)
        {
            specHelpdesk().checkInvariant();
            try
            {
                return HelpdeskImpl.this.addCustomer(name);
            }
            finally
            {
                specHelpdesk().checkInvariant();
            }
        }

        @Override
        public Issue addIssue(Person customer, Person analyst)
        {
            final HelpdeskSpec before = specHelpdesk();
            final IssueImpl issue = (IssueImpl)HelpdeskImpl.this.addIssue(customer, analyst);
            new HelpdeskSpec.CreateIssue(new Prime<HelpdeskSpec.Issue>(issue.specIssue()),
                                         specCustomer(customer),
                                         specAnalyst(analyst),
                                         new Delta<HelpdeskSpec>(before, specHelpdesk()),
                                         issue.specId()).checkInvariant();
            return ((IssueImpl)issue).new Instrumented(HelpdeskImpl.this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<? extends Issue> getAnalystOpenIssues(Person analyst)
        {
            final HelpdeskSpec helpdeskBefore = specHelpdesk();
            final List<IssueImpl> analystOpenIssues = (List<IssueImpl>)HelpdeskImpl.this.getAnalystOpenIssues(analyst);
            new HelpdeskSpec.ReportIssuesForAnalyst(new Xi<HelpdeskSpec>(helpdeskBefore, specHelpdesk()),
                                                    transform(analystOpenIssues, specIssue),
                                                    specAnalyst(analyst)).checkInvariant();
            return analystOpenIssues;
        }
    }
    
    HelpdeskSpec specHelpdesk()
    {
        return new HelpdeskSpec(newHashMap(transformValues(uniqueIndex(issues, specId), specIssue)));
    }
    
    static HelpdeskSpec.Customer specCustomer(Person customer)
    {
        return new HelpdeskSpec.Customer(customer.getName());
    }
    
    static HelpdeskSpec.Analyst specAnalyst(Person analyst)
    {
        return new HelpdeskSpec.Analyst(analyst.getName());
    }
}
