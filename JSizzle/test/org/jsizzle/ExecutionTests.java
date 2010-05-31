package org.jsizzle;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({BindingTest.class, DeltaTest.class, InvariablesTest.class})
public class ExecutionTests
{
}
