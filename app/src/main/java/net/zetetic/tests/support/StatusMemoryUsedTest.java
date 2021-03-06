package net.zetetic.tests.support;

import net.sqlcipher.database.SQLiteDatabase;
import net.zetetic.tests.SQLCipherTest;

public class StatusMemoryUsedTest extends SupportTest {

    public static final int SQLITE_STATUS_MEMORY_USED = 0;

    @Override
    public boolean execute(SQLiteDatabase database) {

        int originalMemory = database.status(SQLITE_STATUS_MEMORY_USED, false);
        database.execSQL("create table t1(a,b)");
        database.execSQL("insert into t1(a,b) values(?, ?)",
                         new Object[]{"one for the money", "two for the show"});
        int currentMemory = database.status(SQLITE_STATUS_MEMORY_USED, false);
        return originalMemory != currentMemory;
    }

    @Override
    public String getName() {
        return "Status Memory Used Test";
    }
}
