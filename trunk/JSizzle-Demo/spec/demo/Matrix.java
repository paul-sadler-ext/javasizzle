package demo;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsizzle.Invariant;
import org.jsizzle.Schema;

@Schema class Matrix
{
    class Value {}
    
    class Category
    {
        String name;
        List<String> items;
        
        @Invariant boolean atLeastOneItem()
        {
            return !items.isEmpty();
        }
    }
    
    Set<Category> categories;
    Map<Map<Category, String>, Value> cells;
    
    @Invariant boolean addressesOk()
    {
        for (Map<Category, String> address : cells.keySet())
        {
            if (!address.keySet().equals(categories))
                return false;
            
            for (Map.Entry<Category, String> e : address.entrySet())
            {
                if (!e.getKey().items.contains(e.getValue()))
                    return false;
            }
        }
        return true;
    }
}
