package net.corda.samples.tokenizedhouse.contracts;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.tokenizedhouse.states.FungibleCassinoTokenState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;
import static net.corda.testing.node.NodeTestUtils.ledger;

public class ContractTestsCassino {
    private final MockServices ledgerServices = new MockServices();
    TestIdentity Operator = new TestIdentity(new CordaX500Name("Alice",  "TestLand",  "US"));

    //sample tests
    @Test
    public void valuationCannotBeZero() {
        FungibleCassinoTokenState tokenPass = new FungibleCassinoTokenState(10000,Operator.getParty(),
                new UniqueIdentifier(),
                0,"NYCHelena");
        FungibleCassinoTokenState tokenFail = new FungibleCassinoTokenState(0,Operator.getParty(),
                new UniqueIdentifier(),
                0,"NYCHelena");
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(CassinoTokenStateContract.CONTRACT_ID, tokenFail);
                tx.command(Operator.getPublicKey(), new com.r3.corda.lib.tokens.contracts.commands.Create());
                return tx.fails();
            });
            l.transaction(tx -> {
                tx.output(CassinoTokenStateContract.CONTRACT_ID, tokenPass);
                tx.command(Operator.getPublicKey(), new com.r3.corda.lib.tokens.contracts.commands.Create());
                return tx.verifies();
            });
            return null;
        });
    }
}