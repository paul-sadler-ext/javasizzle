package org.jsizzle;

public class Prime<T extends Binding<T>> extends Delta<T>
{
    public Prime(T after)
    {
        super(after, after);
    }
}
