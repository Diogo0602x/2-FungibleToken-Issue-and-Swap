package net.corda.samples.tokenizedhouse.contracts;

import net.corda.samples.tokenizedhouse.states.FungibleCassinoTokenState;
import net.corda.testing.node.MockServices;
import org.junit.Test;

public class CassinoStateTests {
    private final MockServices ledgerServices = new MockServices();

    //sample State tests
    @Test
    public void hasConstructionAreaFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        FungibleCassinoTokenState.class.getDeclaredField("symbol");
        // Is the message field of the correct type?
        assert(FungibleCassinoTokenState.class.getDeclaredField("symbol").getType().equals(String.class));
    }
}