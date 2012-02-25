package demo;

import static QAPI.events.QuantrixEventType.cModelWillClose;
import static QAPI.events.QuantrixEventType.cViewDidAdd;
import QAPI.MatrixView;
import QAPI.Model;
import QAPI.events.QuantrixEvent;
import QAPI.events.QuantrixEventListener;

public class ModelSpecMapping
{
    public ModelSpecMapping(final Model model)
    {
        model.addListener(new QuantrixEventListener()
        {
            @Override public void notify(QuantrixEvent event)
            {
                if (event.getType() == cViewDidAdd)
                {
                    if (event.getContext() instanceof MatrixView)
                    {
                        new MatrixSpecMapping((MatrixView)event.getContext());
                    }
                }
                else if (event.getType() == cModelWillClose)
                {
                    model.removeListener(this);
                }
            }
        });
        for (int i = 0; i < model.getMatrixCount(); i++)
            new MatrixSpecMapping(model.getMatrixAt(i));
    }
}
