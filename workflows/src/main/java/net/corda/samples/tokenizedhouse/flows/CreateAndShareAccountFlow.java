package net.corda.samples.tokenizedhouse.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount;
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;

import java.util.List;

@StartableByRPC
@InitiatingFlow
public class CreateAndShareAccountFlow extends FlowLogic<String> {

    private final String accountName;
    private final List<Party> partyToShareAccountInfoToList;

    public CreateAndShareAccountFlow(String accountName, List<Party>  partyToShareAccountInfoToList) {
        this.accountName = accountName;
        this.partyToShareAccountInfoToList = partyToShareAccountInfoToList;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {

        //Call inbuilt CreateAccount flow to create the AccountInfo object
        StateAndRef<AccountInfo> accountInfoStateAndRef = (StateAndRef<AccountInfo>) subFlow(new CreateAccount(accountName));

        //Share this AccountInfo object with the parties who want to transact with this account
        subFlow(new ShareAccountInfo(accountInfoStateAndRef, partyToShareAccountInfoToList));
        return "" + accountName +"has been created and shared to " +partyToShareAccountInfoToList+".";
    }
}