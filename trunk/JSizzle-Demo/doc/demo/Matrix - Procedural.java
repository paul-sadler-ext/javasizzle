package demo;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsizzle.Invariant;
import org.jsizzle.Schema;

@Schema class Matrix
{
    class Value {}
    class Name {}
    
    class Category
    {
        Name name;
        List<Name> items;
        
        @Invariant boolean atLeastOneItem()
        {
            return !items.isEmpty();
        }
    }
    
    Set<Category> categories;
    
    class Address
    {
        Map<Category, Name> categoryItems;
        
        @Invariant boolean itemsValid()
        {
            for (Category c : categoryItems.keySet())
            {
                if (!c.items.contains(categoryItems.get(c)))
                    return false;
            }
            return true;
        }
    }
    
    Map<Address, Value> cells;
    
    @Invariant boolean addressesOk()
    {
        for (Address a : cells.keySet())
        {
            if (!a.categoryItems.keySet().equals(categories))
                return false;
        }
        return true;
    }
}
