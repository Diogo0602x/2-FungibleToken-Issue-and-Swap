package net.corda.samples.tokenizedhouse.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import net.corda.core.contracts.TransactionState;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.samples.tokenizedhouse.states.FungibleHouseTokenState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.utilities.ProgressTracker;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class QueryTokens {

    @InitiatingFlow
    @StartableByRPC
    public static class GetTokenBalance extends FlowLogic<String> {
        private final ProgressTracker progressTracker = new ProgressTracker();
        private final String symbol;

        public GetTokenBalance(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Override
        @Suspendable
        public String call() throws FlowException {
            //get a set of the RealEstateEvolvableTokenType object on ledger with uuid as input tokenId
            Set<FungibleHouseTokenState> evolvableTokenTypeSet = getServiceHub().getVaultService().
                    queryBy(FungibleHouseTokenState.class).getStates().stream()
                    .filter(sf->sf.getState().getData().getSymbol().equals(symbol)).map(StateAndRef::getState)
                    .map(TransactionState::getData).collect(Collectors.toSet());
            if (evolvableTokenTypeSet.isEmpty()){
                throw new IllegalArgumentException("FungibleHouseTokenState symbol=\""+symbol+"\" not found from vault");
            }

            // Save the result
            String result="";

            // Technically the set will only have one element, because we are query by symbol.
            for (FungibleHouseTokenState evolvableTokenType : evolvableTokenTypeSet){
                //get the pointer pointer to the house
                TokenPointer<FungibleHouseTokenState> tokenPointer = evolvableTokenType.toPointer(FungibleHouseTokenState.class);
                //query balance or each different Token
                Amount<TokenType> amount = QueryUtilities.tokenBalance(getServiceHub().getVaultService(), tokenPointer);
                result += "\nYou currently have "+ amount.getQuantity()+ " " + symbol + " Tokens issued by "
                        +evolvableTokenType.getMaintainer().getName().getOrganisation()+"\n";
            }
            return result;
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class QueryTokensByAccount extends FlowLogic<String> {
        private final ProgressTracker progressTracker = new ProgressTracker();
        private final String symbol;
        private final String whoAmI;

        public QueryTokensByAccount(String whoAmI, String symbol) {
            this.whoAmI = whoAmI;
            this.symbol = symbol;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Override
        @Suspendable
        public String call() throws FlowException {
            AccountInfo myAccount = UtilitiesKt.getAccountService(this).accountInfo(whoAmI).get(0).getState().getData();
            UUID id = myAccount.getIdentifier().getId();
            QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria().withExternalIds(Arrays.asList(id));

            List<StateAndRef<FungibleHouseTokenState>> token2List = getServiceHub().getVaultService().queryBy(FungibleHouseTokenState.class).getStates();
            // Query for the account specified by the whoAmI parameter
            AccountInfo account = UtilitiesKt.getAccountService((FlowLogic<?>) getServiceHub()).accountInfo(whoAmI).get(0).getState().getData();
            // Query for the set of FungibleHouseTokenState objects on the ledger owned by the account and with the provided symbol
            Set<FungibleHouseTokenState> tokenStateSet = getServiceHub().getVaultService().queryBy(FungibleHouseTokenState.class, new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)).getStates().stream()
                    .filter(tokenState -> tokenState.getState().getData().getMaintainer().equals(account.getHost()) && tokenState.getState().getData().getSymbol().equals(symbol)).map(StateAndRef::getState)
                    .map(TransactionState::getData).collect(Collectors.toSet());

            // Save the result
            String result = "";

            // Iterate through the set of FungibleHouseTokenState objects
            for (FungibleHouseTokenState tokenState : tokenStateSet) {
                // Get a TokenPointer to the FungibleHouseTokenState object
                TokenPointer<FungibleHouseTokenState> tokenPointer = tokenState.toPointer(FungibleHouseTokenState.class);
                // Query for the balance of the token
                Amount<TokenType> amount = QueryUtilities.tokenBalance(getServiceHub().getVaultService(), tokenPointer);
                result += "\nYou currently have "+ amount.getQuantity()+ " " + symbol + " Tokens issued by "
                        +tokenState.getMaintainer().getName().getOrganisation()+"\n";
            }
            return result;
        }
    }
}