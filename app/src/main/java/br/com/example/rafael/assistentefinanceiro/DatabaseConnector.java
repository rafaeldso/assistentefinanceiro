// DatabaseConnector.java
// Fornece fácil conexão e criação do banco de dados UserAccounts1.
package br.com.example.rafael.assistentefinanceiro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseConnector {
    // database name
    private static final String DATABASE_NAME = "UserAccounts1";

    private SQLiteDatabase database; // para interagir com o banco de dados
    private DatabaseOpenHelper databaseOpenHelper; // cria o banco de dados

    // construtor public de DatabaseConnector
    public DatabaseConnector(Context context) {
        // cria um novo DatabaseOpenHelper
        databaseOpenHelper =
                new DatabaseOpenHelper(context, DATABASE_NAME, null, 1);
    }

    // abre a conexão de banco de dados
    public void open() throws SQLException {
        // cria ou abre um banco de dados para leitura/gravação
        database = databaseOpenHelper.getWritableDatabase();
    }

    // fecha a conexão com o banco de dados
    public void close() {
        if (database != null)
            database.close(); // fecha a conexão com o banco de dados
    }

    // insere uma nova conta no banco de dados
    public long insertAccount(String name, String description, String type, String value) {
        ContentValues newConta = new ContentValues();
        newConta.put("name", name);
        newConta.put("description", description);
        newConta.put("type", type);
        newConta.put("value", value);

        open(); // open the database
        long rowID = database.insert("accounts", null, newConta);
        close(); // close the database
        return rowID;
    }

    // atualiza um contato existente no banco de dados
    public void updateAccount(long id, String name, String description, String type, String value) {
        ContentValues editConta = new ContentValues();
        editConta.put("name", name);
        editConta.put("description", description);
        editConta.put("type", type);
        editConta.put("value", value);

        open(); // open the database
        database.update("accounts", editConta, "_id=" + id, null);
        close(); // close the database
    } // end method updateAccount

    // retorna um Cursor com todos os nomes de contato do banco de dados
    public Cursor getAllContas() {
        return database.query("accounts", new String[]{"_id", "name"},
                null, null, null, null, "name");
    }

    public Cursor getAllAccount() {
        return database.query("accounts", new String[]{"_id", "name", "description", "type", "value"},
                null, null, null, null, "name");
    }

    // retorna um Cursor contendo as informações da conta especificada
    public Cursor getOneAccount(long id) {
        return database.query(
                "accounts", null, "_id=" + id, null, null, null, null);
    }

    // exclui a conta especificado pelo nome String fornecido
    public void deleteAcc(long id) {
        open(); // open the database
        database.delete("accounts", "_id=" + id, null);
        close(); // close the database
    }

    private class DatabaseOpenHelper extends SQLiteOpenHelper {
        // constructor
        public DatabaseOpenHelper(Context context, String name,
                                  CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        // cria a tabela de contatos quando o banco de dados é gerado
        @Override
        public void onCreate(SQLiteDatabase db) {
            // query to create a new table named accounts
            String createQuery = "CREATE TABLE accounts" +
                    "(_id integer primary key autoincrement," +
                    "name TEXT, description TEXT, type TEXT, "
                    + "value TEXT);";

            db.execSQL(createQuery); // execute query to create the database
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion,
                              int newVersion) {
        }
    } // end class DatabaseOpenHelper
} // end class DatabaseConnector

