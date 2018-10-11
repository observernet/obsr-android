package wallet;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import org.obsrj.core.Address;
import org.obsrj.core.BlockChain;
import org.obsrj.core.Coin;
import org.obsrj.core.Context;
import org.obsrj.core.InsufficientMoneyException;
import org.obsrj.core.PeerGroup;
import org.obsrj.core.Sha256Hash;
import org.obsrj.core.Transaction;
import org.obsrj.core.TransactionInput;
import org.obsrj.core.TransactionOutput;
import org.obsrj.core.Utils;
import org.obsrj.core.listeners.TransactionConfidenceEventListener;
import org.obsrj.crypto.DeterministicKey;
import org.obsrj.crypto.LinuxSecureRandom;
import org.obsrj.crypto.MnemonicCode;
import org.obsrj.crypto.MnemonicException;
import org.obsrj.wallet.DeterministicKeyChain;
import org.obsrj.wallet.DeterministicSeed;
import org.obsrj.wallet.Protos;
import org.obsrj.wallet.SendRequest;
import org.obsrj.wallet.UnreadableWalletException;
import org.obsrj.wallet.Wallet;
import org.obsrj.wallet.WalletFiles;
import org.obsrj.wallet.WalletProtobufSerializer;
import org.obsrj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import global.CoinCoreContext;
import global.ContextWrapper;
import global.WalletConfiguration;
import global.utils.Io;
import wallet.exceptions.InsufficientInputsException;
import wallet.exceptions.TxNotFoundException;

import static global.CoinCoreContext.Files.WALLET_FILENAME_PROTOBUF;

/**
 * Created by furszy on 6/4/17.
 */

public class WalletManager {

    private static final Logger logger = LoggerFactory.getLogger(WalletManager.class);
    /**
     * Minimum entropy
     */
    private static final int SEED_ENTROPY_EXTRA = 256;
    private static final int ENTROPY_SIZE_DEBUG = -1;


    private Wallet wallet;
    private File walletFile;

    private WalletConfiguration conf;
    private ContextWrapper contextWrapper;

    public WalletManager(ContextWrapper contextWrapper, WalletConfiguration conf) {
        this.conf = conf;
        this.contextWrapper = contextWrapper;
    }

    // methods

    public Address newFreshReceiveAddress() {
        return wallet.freshReceiveAddress();
    }

    /**
     * Get the last address active which not appear on a tx.
     *
     * @return
     */
    public Address getCurrentAddress() {
        return wallet.currentReceiveAddress();
    }

    public List<Address> getIssuedReceiveAddresses() {
        return wallet.getIssuedReceiveAddresses();
    }

    /**
     * Method to know if an address is already used for receive coins.
     *
     * @return
     */
    public boolean isMarkedAddress(Address address) {
        return false;
    }

    public boolean isWatchingAddress(Address address) {
        return wallet.isAddressWatched(address);
    }

    public void completeSend(SendRequest sendRequest) throws InsufficientMoneyException {
        wallet.completeTx(sendRequest);
    }

    // init

    public void init() throws IOException {
        // init mnemonic code first..
        initMnemonicCode();

        restoreOrCreateWallet();
    }

    private void initMnemonicCode() {
        try {
            MnemonicCode.INSTANCE = new MnemonicCode(contextWrapper.openAssestsStream(CoinCoreContext.Files.BIP39_WORDLIST_FILENAME), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restoreOrCreateWallet() throws IOException {
        walletFile = contextWrapper.getFileStreamPath(WALLET_FILENAME_PROTOBUF);
        loadWalletFromProtobuf(walletFile);
    }


    private void loadWalletFromProtobuf(File walletFile) throws IOException {
        if (walletFile.exists()) {
            FileInputStream walletStream = null;
            try {
                walletStream = new FileInputStream(walletFile);
                wallet = new WalletProtobufSerializer().readWallet(walletStream);

                if (!wallet.getParams().equals(CoinCoreContext.NETWORK_PARAMETERS))
                    throw new UnreadableWalletException("bad wallet network parameters: " + wallet.getParams().getId());

            } catch (UnreadableWalletException e) {
                logger.error("problem loading wallet", e);
                wallet = restoreWalletFromBackup();
            } catch (FileNotFoundException e) {
                logger.error("problem loading wallet", e);
                //context.toast(e.getClass().getName());
                wallet = restoreWalletFromBackup();
            } finally {
                if (walletStream != null)
                    try {
                        walletStream.close();
                    } catch (IOException e) {
                        //nothing
                    }
            }
            if (!wallet.isConsistent()) {
                //contextWrapper.toast("inconsistent wallet: " + walletFile);
                logger.error("inconsistent wallet " + walletFile);
                wallet = restoreWalletFromBackup();
            }
            if (!wallet.getParams().equals(CoinCoreContext.NETWORK_PARAMETERS))
                throw new Error("bad wallet network parameters: " + wallet.getParams().getId());

            afterLoadWallet();

        } else {

            // generate wallet from random mnemonic
            wallet = generateRandomWallet();

            saveWallet();
            backupWallet();

//            config.armBackupReminder();
            logger.info("new wallet created");
        }

        wallet.addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction transaction, Coin coin, Coin coin1) {
                Context.propagate(CoinCoreContext.CONTEXT);
                saveWallet();
            }
        });
    }

    public Wallet generateRandomWallet() {
        if (Utils.isAndroidRuntime()) {
            new LinuxSecureRandom();
        }
        List<String> words = generateMnemonic(SEED_ENTROPY_EXTRA);
        DeterministicSeed seed = new DeterministicSeed(words, null, "", System.currentTimeMillis());
        return Wallet.fromSeed(CoinCoreContext.NETWORK_PARAMETERS, seed, DeterministicKeyChain.KeyChainType.BIP44_COIN_ONLY);
    }

    public static List<String> generateMnemonic(int entropyBitsSize) {
        byte[] entropy;
        if (ENTROPY_SIZE_DEBUG > 0) {
            entropy = new byte[ENTROPY_SIZE_DEBUG];
        } else {
            entropy = new byte[entropyBitsSize / 8];
        }
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(entropy);
        return bytesToMnemonic(entropy);
    }

    public static List<String> bytesToMnemonic(byte[] bytes) {
        List<String> mnemonic;
        try {
            mnemonic = MnemonicCode.INSTANCE.toMnemonic(bytes);
        } catch (MnemonicException.MnemonicLengthException e) {
            throw new RuntimeException(e); // should not happen, we have 16 bytes of entropy
        }
        return mnemonic;
    }


    private void afterLoadWallet() throws IOException {
        wallet.autosaveToFile(walletFile, CoinCoreContext.Files.WALLET_AUTOSAVE_DELAY_MS, TimeUnit.MILLISECONDS, new WalletAutosaveEventListener(conf));
        try {
            // clean up spam
            wallet.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // make sure there is at least one recent backup
        if (!contextWrapper.getFileStreamPath(CoinCoreContext.Files.WALLET_KEY_BACKUP_PROTOBUF).exists())
            backupWallet();

        logger.info("Wallet loaded.");
    }

    /**
     * Restore wallet from backup
     *
     * @return
     */
    private Wallet restoreWalletFromBackup() {

        InputStream is = null;
        try {
            is = contextWrapper.openFileInput(CoinCoreContext.Files.WALLET_KEY_BACKUP_PROTOBUF);
            final Wallet wallet = new WalletProtobufSerializer().readWallet(is, true, null);
            if (!wallet.isConsistent())
                throw new Error("Inconsistent backup");
            // todo: acá tengo que resetear la wallet
            //resetBlockchain();
            //context.toast("Your wallet was reset!\\\\nIt will take some time to recover.");
            logger.info("wallet restored from backup: '" + CoinCoreContext.Files.WALLET_KEY_BACKUP_PROTOBUF + "'");
            return wallet;
        } catch (final IOException e) {
            throw new Error("cannot read backup", e);
        } catch (UnreadableWalletException e) {
            throw new Error("cannot read backup", e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                // nothing
            }
        }
    }

    public void restoreWalletFrom(List<String> mnemonic, long timestamp, boolean bip44) throws IOException, MnemonicException {
        MnemonicCode.INSTANCE.check(mnemonic);
        wallet = Wallet.fromSeed(
                CoinCoreContext.NETWORK_PARAMETERS,
                new DeterministicSeed(mnemonic, null, "", timestamp),
                bip44 ? DeterministicKeyChain.KeyChainType.BIP44_COIN_ONLY : DeterministicKeyChain.KeyChainType.BIP32
        );
        restoreWallet(wallet);
    }

    /**
     * Este metodo puede tener varias implementaciones de guardado distintas.
     */
    public void saveWallet() {
        try {
            protobufSerializeWallet(wallet);
        } catch (final IOException x) {
            throw new RuntimeException(x);
        }
    }

    /**
     * Save wallet file
     *
     * @param wallet
     * @throws IOException
     */
    private void protobufSerializeWallet(final Wallet wallet) throws IOException {
        logger.info("trying to serialize: " + walletFile.getAbsolutePath());
        wallet.saveToFile(walletFile);
        // make wallets world accessible in test mode
        //if (conf.isTest())
        //    Io.chmod(walletFile, 0777);

        logger.info("wallet saved to: '{}', took {}", walletFile);
    }


    /**
     * Backup wallet
     */
    private void backupWallet() {

        final Protos.Wallet.Builder builder = new WalletProtobufSerializer().walletToProto(wallet).toBuilder();

        // strip redundant
        builder.clearTransaction();
        builder.clearLastSeenBlockHash();
        builder.setLastSeenBlockHeight(-1);
        builder.clearLastSeenBlockTimeSecs();
        final Protos.Wallet walletProto = builder.build();

        OutputStream os = null;

        try {
            os = contextWrapper.openFileOutputPrivateMode(CoinCoreContext.Files.WALLET_KEY_BACKUP_PROTOBUF);
            walletProto.writeTo(os);
        } catch (FileNotFoundException e) {
            logger.error("problem writing wallet backup", e);
        } catch (IOException e) {
            logger.error("problem writing wallet backup", e);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                // nothing
            }
        }
    }

    /**
     * Backup wallet file with a given password
     *
     * @param file
     * @param password
     * @throws IOException
     */

    public boolean backupWallet(File file, final String password) throws IOException {
        return backupWallet(wallet, file, password);
    }

    /**
     * Backup wallet file with a given password
     *
     * @param file
     * @param password
     * @throws IOException
     */
    public boolean backupWallet(Wallet wallet, File file, final String password) throws IOException {

        final Protos.Wallet walletProto = new WalletProtobufSerializer().walletToProto(wallet);

        Writer cipherOut = null;

        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            walletProto.writeTo(baos);
            baos.close();
            final byte[] plainBytes = baos.toByteArray();

            cipherOut = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8);
            cipherOut.write(Crypto.encrypt(plainBytes, password.toCharArray()));
            cipherOut.flush();

            logger.info("backed up wallet to: '" + file + "'");

            return true;
        } finally {
            if (cipherOut != null) {
                try {
                    cipherOut.close();
                } catch (final IOException x) {
                    // swallow
                }
            }
        }
    }

    public List<Address> getWatchedAddresses() {
        return wallet.getWatchedAddresses();
    }

    public void reset() {
        wallet.reset();
    }

    public long getEarliestKeyCreationTime() {
        return wallet.getEarliestKeyCreationTime();
    }

    public void addWalletFrom(PeerGroup peerGroup) {
        peerGroup.addWallet(wallet);
    }

    public void addWalletFrom(BlockChain blockChain) {
        blockChain.addWallet(wallet);
    }

    public void removeWalletFrom(PeerGroup peerGroup) {
        peerGroup.removeWallet(wallet);
    }

    public int getLastBlockSeenHeight() {
        return wallet.getLastBlockSeenHeight();
    }

    public Transaction getTransaction(Sha256Hash hash) {
        return wallet.getTransaction(hash);
    }

    public void addCoinsReceivedEventListener(WalletCoinsReceivedEventListener coinReceiverListener) {
        wallet.addCoinsReceivedEventListener(coinReceiverListener);
    }

    public void removeCoinsReceivedEventListener(WalletCoinsReceivedEventListener coinReceiverListener) {
        wallet.removeCoinsReceivedEventListener(coinReceiverListener);
    }

    public Coin getAvailableBalance() {
        return wallet.getBalance(Wallet.BalanceType.AVAILABLE_SPENDABLE);
    }

    public Coin getValueSentFromMe(Transaction transaction) {
        return transaction.getValueSentFromMe(wallet);
    }

    public Coin getValueSentToMe(Transaction transaction) {
        return transaction.getValueSentToMe(wallet);
    }


    public void restoreWalletFromProtobuf(final File file) throws IOException {
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            restoreWallet(WalletUtils.restoreWalletFromProtobuf(is, CoinCoreContext.NETWORK_PARAMETERS));
            logger.info("successfully restored unencrypted wallet: {}", file);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final IOException x2) {
                    // swallow
                }
            }
        }
    }

    private void restoreWallet(final Wallet wallet) throws IOException {

        replaceWallet(wallet);

        //config.disarmBackupReminder();
        // en vez de hacer esto acá hacerlo en el module..
        /*if (listener!=null)
            listener.onWalletRestored();*/

    }

    public void replaceWallet(final Wallet newWallet) throws IOException {
        resetBlockchain();

        try {
            wallet.shutdownAutosaveAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
        wallet = newWallet;
        //conf.maybeIncrementBestChainHeightEver(newWallet.getLastBlockSeenHeight());
        afterLoadWallet();

        // todo: Nadie estaba escuchando esto.. Tengo que ver que deberia hacer despues
//        final IntentWrapper intentWrapper = new IntentWrapperAndroid(WalletConstants.ACTION_WALLET_REFERENCE_CHANGED);
//        intentWrapper.setPackage(context.getPackageName());
//        context.sendLocalBroadcast(intentWrapper);
    }

    private void resetBlockchain() {
        contextWrapper.stopBlockchain();
    }

    public void restoreWalletFromEncrypted(File file, String password) throws IOException {
        final BufferedReader cipherIn = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charsets.UTF_8));
        final StringBuilder cipherText = new StringBuilder();
        Io.copy(cipherIn, cipherText, CoinCoreContext.BACKUP_MAX_CHARS);
        cipherIn.close();

        final byte[] plainText = Crypto.decryptBytes(cipherText.toString(), password.toCharArray());
        final InputStream is = new ByteArrayInputStream(plainText);

        restoreWallet(WalletUtils.restoreWalletFromProtobufOrBase58(is, CoinCoreContext.NETWORK_PARAMETERS, CoinCoreContext.BACKUP_MAX_CHARS));

        logger.info("successfully restored encrypted wallet: {}", file);
    }

    /**
     * Restart the wallet and re create it in a watch only mode.
     *
     * @param xpub
     */
    public void watchOnlyMode(String xpub, DeterministicKeyChain.KeyChainType keyChainType) throws IOException {
        Wallet wallet = Wallet.fromWatchingKeyB58(CoinCoreContext.NETWORK_PARAMETERS, xpub, 0, keyChainType);
        restoreWallet(wallet);
    }

    public Set<Transaction> listTransactions() {
        return wallet.getTransactions(true);
    }

    /**
     * Return true is this wallet instance built the transaction
     *
     * @param transaction
     */
    public boolean isMine(Transaction transaction) {
        return getValueSentFromMe(transaction).longValue() > 0;
    }

    public void commitTx(Transaction transaction) {
        wallet.maybeCommitTx(transaction);
    }

    public Coin getUnspensableBalance() {
        return wallet.getBalance(Wallet.BalanceType.ESTIMATED).minus(wallet.getBalance(Wallet.BalanceType.AVAILABLE));
    }

    public boolean isAddressMine(Address address) {
        return wallet.isPubKeyHashMine(address.getHash160());
    }

    public void addOnTransactionsConfidenceChange(TransactionConfidenceEventListener transactionConfidenceEventListener) {
        wallet.addTransactionConfidenceEventListener(transactionConfidenceEventListener);
    }

    public void removeTransactionConfidenceChange(TransactionConfidenceEventListener transactionConfidenceEventListener) {
        wallet.removeTransactionConfidenceEventListener(transactionConfidenceEventListener);
    }

    /**
     * Don't use this, it's just for the ErrorReporter.
     *
     * @return
     */
    @Deprecated
    public Wallet getWallet() {
        return wallet;
    }

    public List<TransactionOutput> listUnspent() {
        return wallet.calculateAllSpendCandidates();
    }

    public List<String> getMnemonic() {
        return wallet.getActiveKeyChain().getMnemonicCode();
    }

    public DeterministicKey getKeyPairForAddress(Address address) {
        DeterministicKey deterministicKey = wallet.getActiveKeyChain().findKeyFromPubHash(address.getHash160());
        logger.info("Key pub: " + deterministicKey.getPublicKeyAsHex());
        return deterministicKey;
    }

    /**
     * If the wallet doesn't contain any private key.
     *
     * @return
     */
    public boolean isWatchOnly() {
        return wallet.isWatching();
    }

    public TransactionOutput getUnspent(Sha256Hash parentTxHash, int index) throws TxNotFoundException {
        Transaction tx = wallet.getTransaction(parentTxHash);
        if (tx == null)
            throw new TxNotFoundException("tx " + parentTxHash.toString() + " not found");
        return tx.getOutput(index);
    }

    public List<TransactionOutput> getRandomListUnspentNotInListToFullCoins(List<TransactionInput> inputs, Coin amount) throws InsufficientInputsException {
        List<TransactionOutput> list = new ArrayList<>();
        Coin total = Coin.ZERO;
        for (TransactionOutput transactionOutput : wallet.calculateAllSpendCandidates()) {
            boolean found = false;
            if (inputs != null) {
                for (TransactionInput input : inputs) {
                    if (input.getConnectedOutput().getParentTransactionHash().equals(transactionOutput.getParentTransactionHash())
                            &&
                            input.getConnectedOutput().getIndex() == transactionOutput.getIndex()) {
                        found = true;
                    }
                }
            }
            if (!found) {
                if (total.isLessThan(amount)) {
                    list.add(transactionOutput);
                    total = total.add(transactionOutput.getValue());
                }
                if (total.isGreaterThan(amount)) {
                    return list;
                }
            }
        }
        throw new InsufficientInputsException("No unspent available", amount.minus(total));
    }

    public Coin getUnspentValue(Sha256Hash parentTransactionHash, int index) {
        Transaction tx = wallet.getTransaction(parentTransactionHash);
        if (tx == null) return null;
        return tx.getOutput(index).getValue();
    }

    public void checkMnemonic(List<String> mnemonic) throws MnemonicException {
        MnemonicCode.INSTANCE.check(mnemonic);
    }

    public DeterministicKey getWatchingPubKey() {
        return wallet.getWatchingKey();
    }

    public String getExtPubKey() {
        return wallet.getWatchingKey().serializePubB58(CoinCoreContext.NETWORK_PARAMETERS);
    }

    public boolean isBip32Wallet() {
        return wallet.getActiveKeyChain().getKeyChainType() == DeterministicKeyChain.KeyChainType.BIP32;
    }

    public List<Transaction> getWatchedSpent() {
        LinkedList<Transaction> candidates = Lists.newLinkedList();
        for (Transaction tx : wallet.getTransactions(true)) {
            if (!tx.isMature()) continue;
            if (tx.getValueSentFromWatched(wallet).signum() > 0) {
                candidates.add(tx);
            }
        }
        return candidates;
    }

    public List<Transaction> getMineSpent() {
        LinkedList<Transaction> candidates = Lists.newLinkedList();
        for (Transaction tx : wallet.getTransactions(true)) {
            if (!tx.isMature()) continue;
            if (tx.getValueSentFromMe(wallet,false).signum() > 0) {
                candidates.add(tx);
            }
        }
        return candidates;
    }

    public List<Transaction> getMineReceived() {
        LinkedList<Transaction> candidates = Lists.newLinkedList();
        for (Transaction tx : wallet.getTransactions(true)) {
            if (!tx.isMature()) continue;
            if (tx.getValueSentToMe(wallet,false).signum() > 0) {
                candidates.add(tx);
            }
        }
        return candidates;
    }

    public boolean isTransactionOnlyRelatedToWatchedAddress(Transaction tx){
        return wallet.isTransactionRelevantToWatched(tx) && !wallet.isTransactionRelevantToMe(tx);
    }

    /**
     * Create a clean transaction from the wallet balance to the sweep address
     *
     * @param sweepAddress
     * @return
     */
    public Transaction createCleanWalletTx(Address sweepAddress) throws InsufficientMoneyException {
        SendRequest sendRequest = SendRequest.emptyWallet(sweepAddress);
        wallet.completeTx(sendRequest);
        return sendRequest.tx;
    }

    public List<String> getAvailableMnemonicWordsList() {
        return MnemonicCode.INSTANCE.getWordList();
    }

    private static final class WalletAutosaveEventListener implements WalletFiles.Listener {

        WalletConfiguration conf;

        public WalletAutosaveEventListener(WalletConfiguration walletConfiguration) {
            conf = walletConfiguration;
        }

        @Override
        public void onBeforeAutoSave(final File file) {
        }

        @Override
        public void onAfterAutoSave(final File file) {
            // make wallets world accessible in test mode
            //if (conf.isTest())
            //    Io.chmod(file, 0777);
        }
    }

}
