package global.store;

import global.ObsrRate;

/**
 * Created by furszy on 3/3/18.
 */

public interface RateDbDao<T> extends AbstractDbDao<T>{

    ObsrRate getRate(String coin);


    void insertOrUpdateIfExist(ObsrRate obsrRate);

}
