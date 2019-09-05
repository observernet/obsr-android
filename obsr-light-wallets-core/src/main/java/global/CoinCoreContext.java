package global;

import android.os.Environment;
import android.text.format.DateUtils;

import com.observer.wallet.core.BuildConfig;

import org.obsrj.core.Context;
import org.obsrj.core.NetworkParameters;
import org.obsrj.params.MainNetParams;
import org.obsrj.params.TestNet3Params;

import java.io.File;

/**
 * Created by furszy on 6/4/17.
 */

public class CoinCoreContext {

    public static final boolean IS_TEST = BuildConfig.FLAVOR == "obsrtest";
    public static final NetworkParameters NETWORK_PARAMETERS = IS_TEST? TestNet3Params.get():MainNetParams.get();
    /** obsrj global context. */
    public static final Context CONTEXT = new Context(NETWORK_PARAMETERS);

    /** Currency exchange rate */
    public static final String URL_FIAT_CURRENCIES_RATE = "https://bitpay.com/rates";

    public static final String URL_BLOCK_EXPLORER = IS_TEST? "https://explorer2.obsr.org": "https://explorer.obsr.org";


    public static final class Files{

        private static final String FILENAME_NETWORK_SUFFIX = NETWORK_PARAMETERS.getId();

        public static final String BIP39_WORDLIST_FILENAME = "bip39-wordlist.txt";
        /** Filename of the block store for storing the chain. */
        public static final String BLOCKCHAIN_FILENAME = "blockchain" + FILENAME_NETWORK_SUFFIX;
        /** Filename of the wallet. */
        public static final String WALLET_FILENAME_PROTOBUF = "wallet-protobuf" + FILENAME_NETWORK_SUFFIX;
        /** How often the wallet is autosaved. */
        public static final long WALLET_AUTOSAVE_DELAY_MS = 5 * DateUtils.SECOND_IN_MILLIS;
        /** Filename of the automatic wallet backup. */
        public static final String WALLET_KEY_BACKUP_PROTOBUF = "key-backup-protobuf" + FILENAME_NETWORK_SUFFIX;
        /** Path to external storage */
        public static final File EXTERNAL_STORAGE_DIR = Environment.getExternalStorageDirectory();
        /** Filename of the manual wallet backup. */
        public static final String EXTERNAL_WALLET_BACKUP = "obsr-wallet-backup" +"_"+ FILENAME_NETWORK_SUFFIX;
        /** Manual backups go here. */
        public static final File EXTERNAL_WALLET_BACKUP_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        public static final String getExternalWalletBackupFileName(String appName){
            return appName+"_"+EXTERNAL_WALLET_BACKUP;
        }
        /** Checkpoint filename */
        public static final String CHECKPOINTS_FILENAME = "checkpoints";

    }

    public static final int PEER_DISCOVERY_TIMEOUT_MS = 10 * (int) DateUtils.SECOND_IN_MILLIS;
    public static final int PEER_TIMEOUT_MS = 15 * (int) DateUtils.SECOND_IN_MILLIS;

    /** Maximum size of backups. Files larger will be rejected. */
    public static final long BACKUP_MAX_CHARS = 10000000;

}
