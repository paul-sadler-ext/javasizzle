package demo;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import QAPI.Model;
import QAPI.Quantrix;
import QAPI.QuantrixFactory;
import QAPI.events.QuantrixEvent;
import QAPI.events.QuantrixEventListener;
import QAPI.events.QuantrixEventType;

public class DemoActivator implements BundleActivator
{
    @Override public void start(BundleContext c) throws Exception
    {
        final Quantrix quantrix = QuantrixFactory.getQuantrix();
        quantrix.addListener(new QuantrixEventListener()
        {
            @Override public void notify(QuantrixEvent event)
            {
                if (event.getType() == QuantrixEventType.cModelDidOpen)
                    new ModelSpecMapping((Model)event.getContext());
            }
        });
    }

    @Override public void stop(BundleContext c) throws Exception
    {
    }
}
