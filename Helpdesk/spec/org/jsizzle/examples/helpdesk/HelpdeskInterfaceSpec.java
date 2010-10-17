package org.jsizzle.examples.helpdesk;

public interface HelpdeskInterfaceSpec
{
    interface Analyst {}
    interface Customer {}
    
    void createOpenIssue(Analyst analyst, Customer customer);
}
