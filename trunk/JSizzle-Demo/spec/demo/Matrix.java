package demo;

import static com.google.common.base.Predicates.compose;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.all;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsizzle.Invariant;
import org.jsizzle.Schema;


@Schema class Matrix
{
    class Name {}
    class Value {}
    
    class Category
    {
        Name name;
        List<Name> items;
        
        @Invariant boolean ok()
        {
            return !items.isEmpty();
        }
    }
    
    class Address
    {
        Map<Category, Name> categoryItems;
        
        Set<Category> categories()
        {
            return categoryItems.keySet();
        }
        
        @Invariant boolean ok()
        {
            for (Category c : categoryItems.keySet())
            {
                if (!c.items.contains(categoryItems.get(c)))
                    return false;
            }
            return true;
        }
    }
    
    Set<Category> categories;
    Map<Address, Value> cells;
    
    @Invariant boolean ok()
    {
        // All addresses must include all categories from the matrix
        return all(cells.keySet(), compose(equalTo(categories), Address.categories));
    }
    
    @Invariant boolean atLeastOneCategory()
    {
        return !categories.isEmpty();
    }
}
