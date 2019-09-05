package com.observer.wallet.module.wallet;

import com.observer.wallet.CoinApplication;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import global.BackupHelper;
import global.CoinCoreContext;
import global.utils.Iso8601Format;

import static com.google.common.base.Preconditions.checkState;

/**
 * Created by furszy on 6/29/17.
 */

public class WalletBackupHelper implements BackupHelper{

    public File determineBackupFile(String extraData) {
        CoinCoreContext.Files.EXTERNAL_WALLET_BACKUP_DIR.mkdirs();
        checkState(CoinCoreContext.Files.EXTERNAL_WALLET_BACKUP_DIR.isDirectory(), "%s is not a directory", CoinCoreContext.Files.EXTERNAL_WALLET_BACKUP_DIR);

        final DateFormat dateFormat = Iso8601Format.newDateFormat();
        dateFormat.setTimeZone(TimeZone.getDefault());

        String appName = CoinApplication.getInstance().getVersionName();

        for (int i = 0; true; i++) {
            final StringBuilder filename = new StringBuilder(CoinCoreContext.Files.getExternalWalletBackupFileName(appName));
            filename.append('-');
            filename.append(dateFormat.format(new Date()));
            if (extraData!=null){
                filename.append("-"+extraData);
            }
            if (i > 0)
                filename.append(" (").append(i).append(')');

            final File file = new File(CoinCoreContext.Files.EXTERNAL_WALLET_BACKUP_DIR, filename.toString());
            if (!file.exists())
                return file;
        }
    }

}
