package com.zbychu;

import com.zbychu.common.Config;
import com.zbychu.common.Constants;
import com.zbychu.db.ConnectionPool;
import com.zbychu.db.DatabaseOperations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DatabaseOperationsTest {

    @Before
    public void setupDatabase(){
        Config.setConfig(Constants.CONFIG_FILE_DEFAULT);
        ConnectionPool.getInstance().setDbReady(true);
    }

    @Test
    public void setupTest(){
        DatabaseOperations dbo = new DatabaseOperations();
        Assert.assertTrue(dbo.setupObjects());
    }


}