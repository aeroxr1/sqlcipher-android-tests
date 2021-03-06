package net.zetetic.tests;

import android.util.Log;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;
import net.zetetic.QueryHelper;
import net.zetetic.ZeteticApplication;

import java.io.File;
import java.security.SecureRandom;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class SQLCipherTest {

    public abstract boolean execute(SQLiteDatabase database);
    public abstract String getName();
    public String TAG = getClass().getSimpleName();
    SecureRandom random = new SecureRandom();
    private TestResult result;

    private SQLiteDatabase database;
    private File databasePath;

    protected void internalSetUp() {
        Log.i(TAG, "Before prepareDatabaseEnvironment");
        ZeteticApplication.getInstance().prepareDatabaseEnvironment();
        Log.i(TAG, "Before getDatabasePath");
        databasePath = ZeteticApplication.getInstance().getDatabasePath(ZeteticApplication.DATABASE_NAME);
        Log.i(TAG, "Before createDatabase");
        database = createDatabase(databasePath);
        Log.i(TAG, "Before setUp");
        setUp();
    }

    public TestResult run() {

        result = new TestResult(getName(), false);
        try {
            internalSetUp();
            long startTime = System.nanoTime();
            result.setResult(execute(database));
            long endTime = System.nanoTime();
            Log.i(TAG, String.format("Test complete: %s ran in %.2f seconds using library version %s", getName(), (endTime - startTime)/1000000000.0d, SQLiteDatabase.SQLCIPHER_ANDROID_VERSION));
            internalTearDown();
        } catch (Exception e) {
            Log.v(ZeteticApplication.TAG, e.toString());
        }
        return result;
    }

    protected double toSeconds(long start, long end){
        return (end - start)/1000000000.0d;
    }

    protected void setMessage(String message){
        result.setMessage(message);
    }

    private void internalTearDown(){
        tearDown(database);
        SQLiteDatabase.releaseMemory();
        database.close();
        if(databasePath != null && databasePath.exists()){
            databasePath.delete();
        }
    }
    
    protected SQLiteDatabase createDatabase(File databasePath){
        Log.i(TAG, "Before ZeteticApplication.getInstance().createDatabase");
        return ZeteticApplication.getInstance().createDatabase(databasePath, new SQLiteDatabaseHook() {
            @Override
            public void preKey(SQLiteDatabase database) {
                createDatabasePreKey(database);
            }

            @Override
            public void postKey(SQLiteDatabase database) {
                createDatabasePostKey(database);
            }
        });
    }

    private String generateColumnName(List<String> columnNames, int columnIndex){
        String labels = "abcdefghijklmnopqrstuvwxyz";
        String element = columnIndex < labels.length()
            ? String.valueOf(labels.charAt(columnIndex))
            : String.valueOf(labels.charAt(random.nextInt(labels.length() - 1)));
        while(columnNames.contains(element) || ReservedWords.contains(element.toUpperCase())){
            element += labels.charAt(random.nextInt(labels.length() - 1));
        }
        columnNames.add(element);
        Log.i(TAG, String.format("Generated column name:%s for index:%d", element, columnIndex));
        return element;
    }

    protected void buildDatabase(SQLiteDatabase database, int rows, int columns, RowColumnValueBuilder builder) {
        List<String> columnNames = new ArrayList<>();
        Log.i(TAG, String.format("Building database with %s rows, %d columns",
            NumberFormat.getInstance().format(rows), columns));
        String createTemplate = "CREATE TABLE t1(%s);";
        String insertTemplate = "INSERT INTO t1 VALUES(%s);";
        StringBuilder createBuilder = new StringBuilder();
        StringBuilder insertBuilder = new StringBuilder();
        for (int column = 0; column < columns; column++) {
            String columnName = generateColumnName(columnNames, column);
            createBuilder.append(String.format("%s BLOB%s",
                columnName,
                column != columns - 1 ? "," : ""));
            insertBuilder.append(String.format("?%s", column != columns - 1 ? "," : ""));
        }
        String create = String.format(createTemplate, createBuilder.toString());
        String insert = String.format(insertTemplate, insertBuilder.toString());
        database.execSQL("DROP TABLE IF EXISTS t1;");
        database.execSQL(create);
        database.execSQL("BEGIN;");
        String[] names = columnNames.toArray(new String[0]);
        for (int row = 0; row < rows; row++) {
            Object[] insertArgs = new Object[columns];
            for (int column = 0; column < columns; column++) {
                insertArgs[column] = builder.buildRowColumnValue(names, row, column);
            }
            database.execSQL(insert, insertArgs);
        }
        database.execSQL("COMMIT;");
        Log.i(TAG, String.format("Database built with %d columns, %d rows", columns, rows));
    }

    protected byte[] generateRandomByteArray(int size) {
        byte[] data = new byte[size];
        random.nextBytes(data);
        return data;
    }

    protected void setUp(){};
    protected void tearDown(SQLiteDatabase database){};
    protected void createDatabasePreKey(SQLiteDatabase database){};
    protected void createDatabasePostKey(SQLiteDatabase database){};

    protected void log(String message){
        Log.i(TAG, message);
    }

    protected void logPragmaSetting(SQLiteDatabase database) {
        String[] pragmas = new String[]{"cipher_version", "kdf_iter", "cipher_page_size", "cipher_use_hmac",
            "cipher_hmac_algorithm", "cipher_kdf_algorithm"};
        String[] defaultPragmas = new String[]{"cipher_default_kdf_iter", "cipher_default_page_size",
            "cipher_default_use_hmac", "cipher_default_hmac_algorithm", "cipher_default_kdf_algorithm"};
        for (String pragma : pragmas) {
            String value = QueryHelper.singleValueFromQuery(database, String.format("PRAGMA %s;", pragma));
            log(String.format("PRAGMA %s set to %s", pragma, value));
        }
        for (String defaultPragma : defaultPragmas) {
            String value = QueryHelper.singleValueFromQuery(database, String.format("PRAGMA %s;", defaultPragma));
            log(String.format("PRAGMA %s set to %s", defaultPragma, value));
        }
    }

    protected List<String> ReservedWords = Arrays.asList(new String[]{
        "ABORT",
        "ACTION",
        "ADD",
        "AFTER",
        "ALL",
        "ALTER",
        "ANALYZE",
        "AND",
        "AS",
        "ASC",
        "ATTACH",
        "AUTOINCREMENT",
        "BEFORE",
        "BEGIN",
        "BETWEEN",
        "BY",
        "CASCADE",
        "CASE",
        "CAST",
        "CHECK",
        "COLLATE",
        "COLUMN",
        "COMMIT",
        "CONFLICT",
        "CONSTRAINT",
        "CREATE",
        "CROSS",
        "CURRENT_DATE",
        "CURRENT_TIME",
        "CURRENT_TIMESTAMP",
        "DATABASE",
        "DEFAULT",
        "DEFERRABLE",
        "DEFERRED",
        "DELETE",
        "DESC",
        "DETACH",
        "DISTINCT",
        "DROP",
        "EACH",
        "ELSE",
        "END",
        "ESCAPE",
        "EXCEPT",
        "EXCLUSIVE",
        "EXISTS",
        "EXPLAIN",
        "FAIL",
        "FOR",
        "FOREIGN",
        "FROM",
        "FULL",
        "GLOB",
        "GROUP",
        "HAVING",
        "IF",
        "IGNORE",
        "IMMEDIATE",
        "IN",
        "INDEX",
        "INDEXED",
        "INITIALLY",
        "INNER",
        "INSERT",
        "INSTEAD",
        "INTERSECT",
        "INTO",
        "IS",
        "ISNULL",
        "JOIN",
        "KEY",
        "LEFT",
        "LIKE",
        "LIMIT",
        "MATCH",
        "NATURAL",
        "NO",
        "NOT",
        "NOTNULL",
        "NULL",
        "OF",
        "OFFSET",
        "ON",
        "OR",
        "ORDER",
        "OUTER",
        "PLAN",
        "PRAGMA",
        "PRIMARY",
        "QUERY",
        "RAISE",
        "RECURSIVE",
        "REFERENCES",
        "REGEXP",
        "REINDEX",
        "RELEASE",
        "RENAME",
        "REPLACE",
        "RESTRICT",
        "RIGHT",
        "ROLLBACK",
        "ROW",
        "SAVEPOINT",
        "SELECT",
        "SET",
        "TABLE",
        "TEMP",
        "TEMPORARY",
        "THEN",
        "TO",
        "TRANSACTION",
        "TRIGGER",
        "UNION",
        "UNIQUE",
        "UPDATE",
        "USING",
        "VACUUM",
        "VALUES",
        "VIEW",
        "VIRTUAL",
        "WHEN",
        "WHERE",
        "WITH",
        "WITHOUT"
    });
}
