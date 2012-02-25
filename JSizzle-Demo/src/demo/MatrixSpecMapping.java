package demo;

import static QAPI.events.QuantrixEventType.cCategoryDidAdd;
import static QAPI.events.QuantrixEventType.cCategoryWillRemove;
import static QAPI.events.QuantrixEventType.cInputCellChanged;
import static QAPI.events.QuantrixEventType.cItemsDidAdd;
import static QAPI.events.QuantrixEventType.cItemsWillRemove;
import static QAPI.events.QuantrixEventType.cNodeDidRename;
import static QAPI.events.QuantrixEventType.cViewWillRemove;
import static com.google.common.base.Functions.compose;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Maps.transformValues;
import static com.google.common.collect.Maps.uniqueIndex;
import static java.util.Arrays.asList;
import static org.jcurry.ValueObjects.toSet;

import java.util.AbstractList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jcurry.AsFunction;

import QAPI.MatrixView;
import QAPI.Node;
import QAPI.QCell;
import QAPI.events.QuantrixEvent;
import QAPI.events.QuantrixEventListener;
import QAPI.events.QuantrixEventType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import demo.Matrix.Address;
import demo.Matrix.Category;
import demo.Matrix.Name;
import demo.Matrix.Value;

public class MatrixSpecMapping
{
    private static final List<QuantrixEventType> checkEvents =
        asList(cCategoryDidAdd,
               cCategoryWillRemove,
               cItemsDidAdd,
               cItemsWillRemove,
               cNodeDidRename,
               cInputCellChanged);
    
    public MatrixSpecMapping(final MatrixView matrix)
    {
        specMatrix(matrix).checkInvariant();
        matrix.addListener(new QuantrixEventListener()
        {
            @Override public void notify(QuantrixEvent event)
            {
                if (checkEvents.contains(event.getType()))
                    specMatrix(matrix).checkInvariant();
                
                if (event.getType() == cViewWillRemove)
                    matrix.removeListener(this);
            }
        });
    }

    private static Matrix specMatrix(final MatrixView matrixView)
    {
        final Set<Category> categories =
            toSet(transform(asList(matrixView.getCategories()), specCategory));
        
        final ImmutableList<QCell> allCells =
            copyOf(matrixView.getRange(matrixView.getCategories()[0]).getCellIterator());
        final ImmutableMap<Address, QCell> addressedCells =
            uniqueIndex(filter(allCells, cellHasValue), specAddress);
        final Map<Address, Value> cells = transformValues(addressedCells, cellValue);
        
        return new Matrix(categories, cells);
    }
    
    @AsFunction
    private static Matrix.Value cellValue(QCell cell)
    {
        return new Matrix.Value(cell.getValue());
    }
    
    @AsFunction
    private static boolean cellHasValue(QCell cell)
    {
        return !cell.getIsEmpty();
    }
    
    @AsFunction
    private static Matrix.Address specAddress(final QCell cell)
    {
        final Map<Category, Node> categoryItems =
            uniqueIndex(asList(cell.getItems()), compose(specCategory, getCategory));
        return new Address(transformValues(categoryItems, specItem));
    }
    
    @AsFunction
    private static Node getCategory(final Node item)
    {
        return item.getCategory();
    }
    
    @AsFunction
    private static Matrix.Name specItem(final Node item)
    {
        return new Matrix.Name(item.getName());
    }
    
    @AsFunction
    private static Matrix.Category specCategory(final Node category)
    {
        final List<Matrix.Name> items = new AbstractList<Matrix.Name>()
        {
            @Override public Name get(int index)
            {
                return specItem(category.getItemAt(index));
            }

            @Override public int size()
            {
                return category.getItemCount();
            }
        };
        return new Matrix.Category(new Matrix.Name(category.getName()), items);
    }
}
