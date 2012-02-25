package demo;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Iterables.transform;

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
        
        Set<Category> categories()
        {
            return categoryItems.keySet();
        }
        
        @Invariant boolean itemsValid()
        {
            return all(categories(), itemValid.apply(this));
        }
        
        boolean itemValid(Category c)
        {
            return c.items.contains(categoryItems.get(c));
        }
    }
    
    Map<Address, Value> cells;
    
    @Invariant boolean addressesOk()
    {
        return all(transform(cells.keySet(), Address.categories), equalTo(categories));
    }
}
