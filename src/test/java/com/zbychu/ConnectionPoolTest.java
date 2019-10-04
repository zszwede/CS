package com.zbychu;

import com.zbychu.common.Config;
import com.zbychu.common.Constants;
import com.zbychu.db.ConnectionPool;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ConnectionPoolTest {

    @Before
    public void setupDatabase(){
        Config.setConfig(Constants.CONFIG_FILE_DEFAULT);
    }

    @Test
    public void hikariDataSource(){
        Assert.assertNotNull(ConnectionPool.getInstance().getDataSource());
    }


}