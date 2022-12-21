package net.corda.samples.tokenizedhouse.flows;

import co.paralleluniverse.fibers.Suspendable;
import kotlin.Unit;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokensHandler;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.tokenizedhouse.states.FungibleCassinoTokenState;

public class CassinoEvolvableFungibleTokenFlow {
    /**
     * Create Fungible Token for a cassino asset on ledger
     */
    @StartableByRPC
    public static class CreateCassinoTokenFlow extends FlowLogic<SignedTransaction> {

        // valuation property of a cassino change hence we are considering cassino as a evolvable asset
        private final int valuation;
        private final String symbol;

        public CreateCassinoTokenFlow (String symbol, int valuation) {
            this.valuation = valuation;
            this.symbol = symbol;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // Obtain a reference to a notary we wish to use.
            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
            //create token type
            FungibleCassinoTokenState evolvableTokenType = new FungibleCassinoTokenState(valuation, getOurIdentity(),
                    new UniqueIdentifier(), 0, this.symbol);

            //wrap it with transaction state specifying the notary
            TransactionState<FungibleCassinoTokenState> transactionState = new TransactionState<>(evolvableTokenType, notary);

            //call built in sub flow CreateEvolvableTokens. This can be called via rpc or in unit testing
            return subFlow(new CreateEvolvableTokens(transactionState));
        }
    }

    /**
     *  Issue Fungible Token against an evolvable cassino asset on ledger
     */
    @StartableByRPC
    public static class IssueCassinoTokenFlow extends FlowLogic<SignedTransaction>{
        private final String symbol;
        private final int quantity;
        private final String holder;

        public IssueCassinoTokenFlow(String symbol, int quantity, String holder) {
            this.symbol = symbol;
            this.quantity = quantity;
            this.holder = holder;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //Generate accountinfo & AnonymousParty object for transaction
            AccountInfo holderAccountInfo = UtilitiesKt.getAccountService(this).accountInfo(holder).get(0).getState().getData();
            Party holderAccount = holderAccountInfo.getHost();
            //get cassino states on ledger with uuid as input tokenId
            StateAndRef<FungibleCassinoTokenState> stateAndRef = getServiceHub().getVaultService().
                    queryBy(FungibleCassinoTokenState.class).getStates().stream()
                    .filter(sf->sf.getState().getData().getSymbol().equals(symbol)).findAny()
                    .orElseThrow(()-> new IllegalArgumentException("FungibleCassinoTokenState symbol=\""+symbol+"\" not found from vault"));

            //get the RealEstateEvolvableTokenType object
            FungibleCassinoTokenState evolvableTokenType = stateAndRef.getState().getData();

            //create fungible token for the cassino token type
            FungibleToken fungibleToken = new FungibleTokenBuilder()
                    .ofTokenType(evolvableTokenType.toPointer(FungibleCassinoTokenState.class)) // get the token pointer
                    .issuedBy(getOurIdentity())
                    .heldBy(holderAccount)
                    .withAmount(quantity)
                    .buildFungibleToken();

            //use built in flow for issuing tokens on ledger
            return subFlow(new IssueTokens(ImmutableList.of(fungibleToken)));
        }
    }

    /**
     *  Move created fungible tokens to other party
     */
    @StartableByRPC
    @InitiatingFlow
    public static class MoveCassinoTokenFlow extends FlowLogic<SignedTransaction> {
        private final String symbol;
        private final int quantity;
        private final String toAccount;

        public MoveCassinoTokenFlow(String symbol, int quantity, String toAccount) {
            this.symbol = symbol;
            this.quantity = quantity;
            this.toAccount = toAccount;
        }


        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            AccountInfo toAccountInfo = UtilitiesKt.getAccountService(this).accountInfo(toAccount).get(0).getState().getData();
            Party toAccountParty = toAccountInfo.getHost();

            //get cassino states on ledger with uuid as input tokenId
            StateAndRef<FungibleCassinoTokenState> stateAndRef = getServiceHub().getVaultService().
                    queryBy(FungibleCassinoTokenState.class).getStates().stream()
                    .filter(sf->sf.getState().getData().getSymbol().equals(symbol)).findAny()
                    .orElseThrow(()-> new IllegalArgumentException("FungibleCassinoTokenState=\""+symbol+"\" not found from vault"));

            //get the RealEstateEvolvableTokenType object
            FungibleCassinoTokenState tokenstate = stateAndRef.getState().getData();

            /*  specify how much amount to transfer to which holder
             *  Note: we use a pointer of tokenstate because it of type EvolvableTokenType
             */
            Amount<TokenType> amount = new Amount<>(quantity, tokenstate.toPointer(FungibleCassinoTokenState.class));
            //PartyAndAmount partyAndAmount = new PartyAndAmount(holder, amount);

            //use built in flow to move fungible tokens to holder
            return subFlow(new MoveFungibleTokens(amount,toAccountParty));
        }
    }

    @InitiatedBy(CassinoEvolvableFungibleTokenFlow.MoveCassinoTokenFlow.class)
    public static class MoveCassinoEvolvableFungibleTokenFlowResponder extends FlowLogic<Unit>{

        private FlowSession counterSession;

        public MoveCassinoEvolvableFungibleTokenFlowResponder(FlowSession counterSession) {
            this.counterSession = counterSession;
        }

        @Suspendable
        @Override
        public Unit call() throws FlowException {
            // Simply use the MoveFungibleTokensHandler as the responding flow
            return subFlow(new MoveFungibleTokensHandler(counterSession));
        }
    }
}
