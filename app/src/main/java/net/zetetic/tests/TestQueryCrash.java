package net.zetetic.tests;

import android.database.Cursor;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;
import net.zetetic.ZeteticApplication;

import java.io.File;

public class TestQueryCrash extends SQLCipherTest {

     private SQLiteDatabase database;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean execute(SQLiteDatabase database) {
        try {
            database.close();
            close();

            ObjectMapper objectMapper = new ObjectMapper();

           String queryString = ZeteticApplication.getInstance().loadTextFromAssets("query.txt");
           ArrayNode resultsRow = selectWithQueryString(queryString);

            setMessage("result row #"+resultsRow.size());
            return true;
        } catch (Exception ex) {
            setMessage(ex.getMessage());
            return false;
        } finally {
            close();
        }
    }

    public ArrayNode selectWithQueryString(String query) {
        return retrieveRowArrays(query, null);
    }

    public ArrayNode retrieveRowArrays(String query, Object[] bindingArgs) {
        ObjectMapper objectMapper = new ObjectMapper();
        open();
        ArrayNode result = objectMapper.createArrayNode();
        Cursor cursor = null;
        try {
            if(bindingArgs != null) {
                cursor = database.rawQuery(query, bindingArgs);
            }else{
                cursor = database.query(query);
            }
            int rowCount = cursor.getCount();
            int columnCount = cursor.getColumnCount();
            cursor.moveToFirst();
            for (int i = 0; i < rowCount; i++) {
                ArrayNode row = objectMapper.createArrayNode();
                for (int j = 0; j < columnCount; j++) {
                    row.add(cursor.getString(j));
                }
                result.add(row);
                cursor.moveToNext();
            }
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return result;
    }

    public boolean open() {
        try {
            database = getDatabase();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
        return (database != null && database.isOpen());
    }

    public SQLiteDatabase getDatabase() {
        SQLiteDatabase sourceDatabase;
        try {
            ZeteticApplication.getInstance().extractAssetToDatabaseDirectory("dbTest.db");
            File sourceDatabaseFile = ZeteticApplication.getInstance().getDatabasePath("dbTest.db");

            SQLiteDatabaseHook hook = new SQLiteDatabaseHook() {
                public void preKey(SQLiteDatabase database) {
                    database.rawExecSQL("PRAGMA kdf_iter = 5000");
                    database.rawExecSQL("PRAGMA cipher_memory_security = OFF;");
                }

                public void postKey(SQLiteDatabase database) {
                }
            };
            //sourceDatabase = SQLiteDatabase.openDatabase(sourceDatabaseFile.getPath(), "", null, SQLiteDatabase.OPEN_READWRITE);
            sourceDatabase = SQLiteDatabase.openOrCreateDatabase(sourceDatabaseFile.getPath(), "", null, hook);

        } catch (Exception e) {
            sourceDatabase = null;
        }
        return sourceDatabase;
    }

    public boolean close() {
        try {
            if (database != null) {
                if (database.isOpen()) {
                    database.close();
                    return true;
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return false;
    }


    @Override
    public String getName() {
        return "TestQueryCrash";
    }



}
