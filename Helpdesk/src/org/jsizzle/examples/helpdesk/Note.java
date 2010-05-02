package org.jsizzle.examples.helpdesk;

public interface Note
{
    Person getAnalyst();
    
    interface Text extends Note
    {
        String getText();
    }
    
    interface Attachment extends Note
    {
        byte[] getData();
    }
    
    interface Resolution extends Note
    {
        Issue.Resolution getResolution();
    }
}
