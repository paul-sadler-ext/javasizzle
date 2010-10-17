package org.jsizzle.examples.helpdesk;

import static java.util.Collections.unmodifiableMap;

import java.util.Map;

public final class HelpdeskJavaZSpec
{
    public static class Customer {}
    public static class Analyst {}
    public static class Id {}
    
    public final Map<Id, Issue> issues;
    
    public enum Status { OPEN, CLOSED }
    
    public static final class Issue 
    {
        public final Customer customer;
        public final Analyst analyst;
        public final Status status;
        
        public static final class InitIssue
        {
            public final Issue after;
            public final Customer customer;
            public final Analyst analyst;
            
            public boolean customerAndAnalystAssignedAndStatusOpen()
            {
                return after.customer.equals(customer)
                        && after.analyst.equals(analyst)
                        && after.status == Status.OPEN;
            }

            public InitIssue(Issue after,
                             Customer customer,
                             Analyst analyst)
            {
                assert after != null;
                assert customer != null;
                assert analyst != null;
                
                this.after = after;
                this.customer = customer;
                this.analyst = analyst;
                
                assert customerAndAnalystAssignedAndStatusOpen();
            }
        }

        public Issue(Customer customer,
                     Analyst analyst,
                     Status status)
        {
            assert customer != null;
            assert analyst != null;
            assert status != null;
            
            this.customer = customer;
            this.analyst = analyst;
            this.status = status;
        }

        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((analyst == null) ? 0 : analyst.hashCode());
            result = prime * result + ((customer == null) ? 0 : customer.hashCode());
            result = prime * result + ((status == null) ? 0 : status.hashCode());
            return result;
        }

        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Issue other = (Issue) obj;
            if (analyst == null)
            {
                if (other.analyst != null)
                    return false;
            }
            else if (!analyst.equals(other.analyst))
                return false;
            if (customer == null)
            {
                if (other.customer != null)
                    return false;
            }
            else if (!customer.equals(other.customer))
                return false;
            if (status != other.status)
                return false;
            return true;
        }
    }

    public HelpdeskJavaZSpec(Map<Id, Issue> issues)
    {
        this.issues = unmodifiableMap(issues);
    }
}
