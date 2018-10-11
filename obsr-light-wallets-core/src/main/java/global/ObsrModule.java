package global;

import org.obsrj.core.Address;
import org.obsrj.core.Coin;
import org.obsrj.core.InsufficientMoneyException;
import org.obsrj.core.Peer;
import org.obsrj.core.Sha256Hash;
import org.obsrj.core.StoredBlock;
import org.obsrj.core.Transaction;
import org.obsrj.core.TransactionInput;
import org.obsrj.core.TransactionOutput;
import org.obsrj.crypto.DeterministicKey;
import org.obsrj.crypto.MnemonicException;
import org.obsrj.wallet.DeterministicKeyChain;
import org.obsrj.wallet.Wallet;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import global.exceptions.CantSweepBalanceException;
import global.exceptions.ContactAlreadyExistException;
import global.exceptions.NoPeerConnectedException;
import global.exceptions.UpgradeException;
import global.wrappers.InputWrapper;
import global.wrappers.TransactionWrapper;
import wallet.exceptions.CantRestoreEncryptedWallet;
import wallet.exceptions.InsufficientInputsException;
import wallet.exceptions.TxNotFoundException;

/**
 * Created by mati on 18/04/17.
 */

public interface ObsrModule {

    /**
     * Initialize the module
     */
    void start() throws IOException;

    /**
     * ...
     */
    void createWallet();

    boolean backupWallet(File backupFile, String password) throws IOException;

    /**
     *
     *
     * @param backupFile
     */
    void restoreWallet(File backupFile) throws IOException;

    void restoreWalletFromEncrypted(File file, String password) throws CantRestoreEncryptedWallet, IOException;

    void restoreWallet(List<String> mnemonic, long timestamp,boolean bip44) throws IOException, MnemonicException;

    /**
     * If the wallet already exist
     * @return
     */
    boolean isWalletCreated();

    /**
     * Return a new address.
     */
    Address getReceiveAddress();

    Address getFreshNewAddress();

    boolean isAddressUsed(Address address);

    long getAvailableBalance();

    Coin getAvailableBalanceCoin();

    Coin getUnnavailableBalanceCoin();

    boolean isWalletWatchOnly();

    BigDecimal getAvailableBalanceLocale();

    boolean isTransactionRelatedToWatchedAddress(Transaction tx);

    List<Transaction> getWatchedSpent();

    List<Transaction> getMineSpent();

    /******    Address Label          ******/

    List<AddressLabel> getContacts();

    AddressLabel getAddressLabel(String address);

    void saveContact(AddressLabel addressLabel) throws ContactAlreadyExistException;

    void saveContactIfNotExist(AddressLabel addressLabel);

    void deleteAddressLabel(AddressLabel data);


    /******   End Address Label          ******/


    boolean chechAddress(String addressBase58);

    Transaction buildSendTx(String addressBase58, Coin amount, String memo,Address changeAddress) throws InsufficientMoneyException;
    Transaction buildSendTx(String addressBase58, Coin amount,Coin feePerKb, String memo,Address changeAddress) throws InsufficientMoneyException;

    WalletConfiguration getConf();
    TransactionWrapper getTxWrapper(String txId);
    List<TransactionWrapper> listTx();

    Coin getValueSentFromMe(Transaction transaction, boolean excludeChangeAddress);

    void commitTx(Transaction transaction);

    List<Peer> listConnectedPeers();

    int getChainHeight();

    StoredBlock getChainHead();

    ObsrRate getRate(String selectedRateCoin);

    /**
     * Don't use this..
     * @return
     */
    @Deprecated
    Wallet getWallet();

    List<InputWrapper> listUnspentWrappers();

    Set<InputWrapper> convertFrom(List<TransactionInput> list) throws TxNotFoundException;

    Transaction getTx(Sha256Hash txId);

    List<String> getMnemonic();

    String getWatchingPubKey();
    DeterministicKey getWatchingKey();

    DeterministicKey getKeyPairForAddress(Address address);

    TransactionOutput getUnspent(Sha256Hash parentTxHash, int index) throws TxNotFoundException;

    List<TransactionOutput> getRandomUnspentNotInListToFullCoins(List<TransactionInput> inputs, Coin amount) throws InsufficientInputsException;

    Transaction completeTx(Transaction transaction,Address changeAddress,Coin fee) throws InsufficientMoneyException;
    Transaction completeTx(Transaction transaction) throws InsufficientMoneyException;

    Transaction completeTxWithCustomFee(Transaction transaction,Coin fee) throws InsufficientMoneyException;

    Coin getUnspentValue(Sha256Hash parentTransactionHash, int index);

    boolean isAnyPeerConnected();

    long getConnectedPeerHeight();

    int getProtocolVersion();

    void checkMnemonic(List<String> mnemonic) throws MnemonicException;

    boolean isSyncWithNode() throws NoPeerConnectedException;

    void watchOnlyMode(String xpub, DeterministicKeyChain.KeyChainType keyChainType) throws IOException;

    boolean isBip32Wallet();

    boolean sweepBalanceToNewSchema() throws InsufficientMoneyException, CantSweepBalanceException;

    boolean upgradeWallet(String upgradeCode) throws UpgradeException;

    List<ObsrRate> listRates();

    List<String> getAvailableMnemonicWordsList();

}
