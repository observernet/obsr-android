package network;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by furszy on 7/2/17.
 */

public class PeerGlobalData {

    public static final String[] TRUSTED_TEST_NODES = new String[]{"testnet1.seeder.obsr.org"};

    public static final String[] TRUSTED_NODES = new String[]{"main1.seeder.obsr.org"};

    public static final List<PeerData> listTrustedHosts() {
        List<PeerData> list = new ArrayList<>();
        for (String trustedNode : TRUSTED_NODES) {
            list.add(new PeerData(trustedNode, 9567, 9567));
        }
        return list;
    }

    public static final List<PeerData> listTrustedTestHosts() {
        List<PeerData> list = new ArrayList<>();
        for (String trustedNode : TRUSTED_TEST_NODES) {
            list.add(new PeerData(trustedNode, 29567, 29567));
        }
        return list;
    }
}
