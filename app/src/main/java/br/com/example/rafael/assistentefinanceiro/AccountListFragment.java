
// Displays the list of account names
package br.com.example.rafael.assistentefinanceiro;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

public class AccountListFragment extends ListFragment {
    // métodos de callback implementados por MainActivity
    public interface AccountListFragmentListener {
        // chamado quando o usuário seleciona uma conta
        public void onAccountSelected(long rowID);

        // chamado quando um usuário decide add uma conta
        public void onAddAccount();

        // chamado quando você deseja enviar um email
        public void onAccountSend();
    }

    private AccountListFragmentListener listener;

    private ListView accountListView; // the ListActivity's ListView
    private CursorAdapter accountAdapter; // adapter for ListView
    private long rowID = -1; // rowID do contato selecionado
    private static final int REQUEST_CODE = 0x11;
    //private final String path = getContext().getFilesDir().getCanonicalPath();

    // configura AccountListFragmentListener quando o fragmento é anexado
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (AccountListFragmentListener) activity;
    }

    // remove AccountListFragmentListener quando o fragmento é desanexado
    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    // chamado depois que a View é criada
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setRetainInstance(true); // salva o fragmento entre mudanças de configuração
        setHasOptionsMenu(true); // este fragmento tem itens de menu a exibir

        // configura o texto a exibir quando não houver contatos
        setEmptyText(getResources().getString(R.string.no_accounts));

        // obtém referência de ListView e configura ListView
        accountListView = getListView();
        accountListView.setOnItemClickListener(viewAccListener);
        accountListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // mapeia o nome de cada conta em um componente TextView no layout de ListView
        String[] from = new String[]{"name"};
        int[] to = new int[]{android.R.id.text1};
        accountAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_1, null, from, to, 0);
        setListAdapter(accountAdapter); // configura o adaptador que fornece dados
    }

    // responde ao toque do usuário no nome de uma conta no componente ListView
    OnItemClickListener viewAccListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view,
                                int position, long id) {
            listener.onAccountSelected(id); // passa a seleção para MainActivity
        }
    }; // end viewAccListener

    // quando o fragmento recomeça, usa um elemento GetAccountsTask para
    // carregar as contas
    @Override
    public void onResume() {
        super.onResume();
        new GetAccountsTask().execute((Object[]) null);
    }

    // executa a consulta de banco de dados fora da thread da interface gráfica
    // do usuário
    private class GetAccountsTask extends AsyncTask<Object, Object, Cursor> {
        DatabaseConnector databaseConnector =
                new DatabaseConnector(getActivity());

        // abre o banco de dados e retorna um Cursor para todas as contas
        @Override
        protected Cursor doInBackground(Object... params) {
            databaseConnector.open();
            return databaseConnector.getAllContas();
        }

        // usa o Cursor retornado pelo método doInBackground
        @Override
        protected void onPostExecute(Cursor result) {
            accountAdapter.changeCursor(result); // configura o Cursor do adaptador
            databaseConnector.close();
        }
    } // end class GetAccountsTask

    // quando o fragmento para, fecha o Cursor e remove de contaAdapter
    @Override
    public void onStop() {
        Cursor cursor = accountAdapter.getCursor(); // obtém o objeto Cursor atual
        accountAdapter.changeCursor(null); // agora o adaptador não tem objeto Cursor

        if (cursor != null)
            cursor.close(); // libera os recursos do Cursor

        super.onStop();
    }

    // exibe os itens de menu deste fragmento
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_account_list_menu, menu);
    }

    // trata a escolha no menu de opções
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                listener.onAddAccount();
                return true;
            case R.id.action_send:

                sendAccount();

                return true;

        }

        return super.onOptionsItemSelected(item); // chama o método de super
    }

    // atualiza o conjunto de dados
    public void updateAccountList() {
        new GetAccountsTask().execute((Object[]) null);
        //Toast.makeText(AccountListFragment.this.getActivity(), "Account Update", Toast.LENGTH_SHORT).show();
    }

    // envia uma conta
    private void sendAccount() {
        // usa FragmentManager para exibir o componente DialogFragment de confirmDelete
        confirmSend.show(getFragmentManager(), "confirm delete");
    }

    // DialogFragment para confirmar o envio das contas
    private DialogFragment confirmSend =
            new DialogFragment() {
                // cria um componente AlertDialog e o retorna
                @Override
                public Dialog onCreateDialog(Bundle bundle) {
                    // cria um novo AlertDialog Builder
                    final AlertDialog.Builder builder =
                            new AlertDialog.Builder(getActivity());

                    builder.setTitle(R.string.send_confirm_title);
                    builder.setMessage(R.string.send_confirm_message);

                    // fornece um botão OK que simplesmente descarta a caixa de diálogo
                    builder.setPositiveButton(R.string.button_send,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        DialogInterface dialog, int button) {
                                    final DatabaseConnector databaseConnector =
                                            new DatabaseConnector(getActivity());
                                    Log.d("SEND", "Entrou no OnClick de Send");
                                    // AsyncTask exclui contato e notifica o receptor
                                    AsyncTask<Long, Object, Object> sendTask =
                                            new AsyncTask<Long, Object, Object>() {
                                                //@RequiresApi(api = Build.VERSION_CODES.KITKAT)

                                                @Override
                                                protected Object doInBackground(Long... params) {
                                                    //databaseConnector.deleteAcc(params[0]);
                                                    ArrayList<Account> list = new ArrayList<Account>();
                                                    databaseConnector.open();
                                                    Cursor cursor = databaseConnector.getAllAccount();
                                                    Log.d("SEND", "Entrou no doInBackground de Send");

                                                    if (cursor.moveToFirst()) {
                                                        do
                                                        //cursor.moveToFirst();
                                                        //while (cursor.moveToNext())
                                                        {
                                                            Log.d("SEND", "Entrou no if dp cursor para configurar account");
                                                            int nameIndex = cursor.getColumnIndex("name");
                                                            int descriptionIndex = cursor.getColumnIndex("description");
                                                            int typeIndex = cursor.getColumnIndex("type");
                                                            int valueIndex = cursor.getColumnIndex("value");
                                                            Log.d("SEND", "Passou no if dp cursor para configurar account" + cursor.getString(nameIndex) + "=" + nameIndex + " description" + descriptionIndex + " type=" + typeIndex + " value" + valueIndex);
                                                            Account acc = new Account();

                                                            acc.setName(cursor.getString(nameIndex));
                                                            acc.setDescription(cursor.getString(descriptionIndex));
                                                            acc.setType(cursor.getString(typeIndex));
                                                            acc.setValue(cursor.getString(valueIndex));
                                                            list.add(acc);
                                                        }
                                                        while (cursor.moveToNext());
                                                    }
                                                    if (cursor != null && !cursor.isClosed()) {
                                                        cursor.close();
                                                    }


                                                    // Novo objecto JSON
                                                    JSONObject jsonObject = new JSONObject();

                                                    // Consulta BD
                                                    //ArrayList<> drugDetails = DataInterface
                                                    //.getSelectedDrugDetails(); //isto deve ser a consulta que devolve um arraylist
                                                    Log.d("SEND", "Passou do JSONOBject");
                                                    // Se temos dados
                                                    if (list != null && list.size() > 0) {

                                                        // Novo array para guardar as entradas do JSON
                                                        JSONArray array = new JSONArray();
                                                        Log.d("SEND", "Entrou no if e criou o JSONARRAY");

                                                        // Por cada entrada da BD, adiciona uma entrada no JSON
                                                        for (Account selectedAccount : list) {
                                                            Log.d("SEND", "Entrou no FOR para criar o JSON");

                                                            JSONObject json = new JSONObject();
                                                            try {
                                                                Log.d("SEND", "Entrou no TRY PARA CRIAR O JSON");
                                                                json.put("title", selectedAccount.getName());
                                                                json.put("description", "" + selectedAccount.getDescription());
                                                                json.put("type", "" + selectedAccount.getType());
                                                                json.put("value", "" + selectedAccount.getValue());
                                                                Log.d("SEND", "Passou do try para criar o JSON");
                                                            } catch (JSONException e) {
                                                                Log.d("SEND", "Caiu no catch para CRIAR o JSON");
                                                                e.printStackTrace();
                                                            }
                                                            array.put(json);
                                                            Log.d("SEND", "Fez o PUT no array CRIAR o JSON");

                                                        }

                                                        try {
                                                            Log.d("SEND", "Entrou no try para CRIAR o JSONOBJECT");
                                                            jsonObject.put("ACCOUNT_LIST", array);
                                                            Log.d("SEND", "Passou no try para CRIAR o JSONOBJECT");
                                                        } catch (JSONException e) {
                                                            Log.d("SEND", "Entrou no catch para CRIAR o JSONOBJECT");
                                                            e.printStackTrace();
                                                        }
                                                        Log.d("SEND", "CRIOU o JSONOBJECT" + jsonObject.toString());

                                                    }

                                                    Log.d("SEND", "Inicio da criação do Intent");
                                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                                    intent.setType("text/plain");
                                                    intent.putExtra(Intent.EXTRA_EMAIL, "blabla");
                                                    intent.putExtra(Intent.EXTRA_SUBJECT, "");
                                                    intent.putExtra(Intent.EXTRA_TEXT, "");
                                                    Log.d("SEND", "Continuação da criação do Intent");
                                                    // try-with-resources statement based on post comment below :)
                                                    Uri uri = null;
                                                    try {

                                                        Log.d("SEND", "Entrei no try criação do anexo");
                                                        Writer output = null;
                                                        Log.d("SEND", "Passei do Write");
                                                        File file = null;

                                                        Log.d("SEND", "Entrou no IF do ExernalStorage, e eu tenho permissão= "+shouldAskPermission());
                                                        if(shouldAskPermission()){
                                                            String[] perms = {"android.permission. WRITE_EXTERNAL_STORAGE"};
                                                            Log.d("SEND", "Entrou no If do shouldAsk");


                                                            String[] permissions = {"android.permission.WRITE_EXTERNAL_STORAGE"};
                                                            int permsRequestCode = 200;

                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                                Log.d("SEND", "Entrou no if requestPermission");
                                                                //Activity ac = AccountListFragment.this.getActivity();
                                                                //AccountListFragment.this.getActivity().re requestPermissions(ac, permissions, REQUEST_CODE);
                                                                AccountListFragment.this.getActivity().requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
                                                                Log.d("SEND","Voltou");
                                                                MainActivity ac = (MainActivity) AccountListFragment.this.getActivity();
                                                                Log.d("SEND","Write="+ac.isWriteAccepted());
                                                                if(!ac.isWriteAccepted()) return null;
                                                                Log.d("SEND", "Passou no if requestPermission");
                                                            }
                                                        }
                                                        File path = Environment.getExternalStoragePublicDirectory(
                                                                Environment.DIRECTORY_PICTURES);
                                                        //File path = AccountListFragment.this.getActivity().getApplicationContext().getExternalFilesDir(
                                                        // Environment.DIRECTORY_PICTURES);//getExternalFilesDir
                                                        file = new File(path, "Account.json");
                                                        if (file == null)
                                                            Log.d("SEND", "FILE É IGUAL A NULL");
                                                        //file.mkdirs();
                                                        file.createNewFile();
                                                        // Make sure the Pictures directory exists.
                                                        Log.d("SEND", "" + " PATH=" + file.getAbsolutePath());
                                                        // file = new File(Environment.getExternalStoragePublicDirectory(
                                                        //Environment.DIRECTORY_DCIM), "file.json");

                                                        //if (!file.mkdirs()) {
                                                        //  Log.d("SEND", "Directory not created");
                                                        //}
                                                        if (!isExternalStorageWritable())
                                                            file = new File(AccountListFragment.this.getActivity().getApplicationContext().getFilesDir(), "file.json");
                                                        Log.d("SEND", "Passei do File");
                                                        //FileWriter fw = new FileWriter(file);
                                                        Log.d("SEND", "Passei do FileWriter");
                                                        if (file == null)
                                                            Log.d("SEND", "FILE É IGUAL A NULL");
                                                        FileOutputStream out = new FileOutputStream(file);
                                                        Log.d("SEND", "Passou do FileOutputStream");
                                                        if (out == null)
                                                            Log.d("SEND", "FILEout É IGUAL A NULL");
                                                        String jsonFile = jsonObject.toString();
                                                        Log.d("SEND", "Passou do jsonFile");
                                                        out.write(jsonFile.getBytes());
                                                        //output = new BufferedWriter(fw);
                                                        Log.d("SEND", "Passou do Buffered");
                                                        //output.write(jsonObject.toString());
                                                        uri = Uri.fromFile(file);
                                                        //output.close();
                                                        out.close();
                                                        Log.d("SEND", "Fim do try da criação do anexo");
                                                    } catch (IOException e) {
                                                        Log.d("SEND", "Entrou no catch criação do anexo");
                                                        e.printStackTrace();
                                                    }/**
                                                     String filename = "file.json";
                                                     String string = jsonObject.toString();
                                                     FileOutputStream outputStream;
                                                     Log.d("SEND", "Passei do File OutputStream");
                                                     try {
                                                     Log.d("SEND", "Entrei no try criação do anexo");
                                                     outputStream = AccountListFragment.this.getActivity().openFileOutput(filename, Context.MODE_PRIVATE);
                                                     Log.d("SEND", "Criei o file do anexo");
                                                     outputStream.write(string.getBytes());
                                                     outputStream.close();
                                                     Log.d("SEND", "Fim da criação do anexo");
                                                     } catch (Exception e) {
                                                     e.printStackTrace();
                                                     }
                                                     **/
                                                    Log.d("SEND", "Inicio do anexo Path=" + AccountListFragment.this.getActivity().getApplicationContext().getFilesDir().getAbsolutePath() + " Uri=" + uri.toString());
                                                    //String uri = Uri.fromFile(file);
                                                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                                                    Log.d("SEND", "Passou do EXTRA_STREAM");
                                                    // Create intent to show chooser
                                                    Intent chooser = Intent.createChooser(intent, "Send Email");
                                                    Log.d("SEND", "Passou da criação do CHOOSER");

                                                    // Verify the intent will resolve to at least one activity
                                                    //if (intent.resolveActivity(AccountListFragment.this.getActivity().getApplicationContext().getPackageManager()) != null) {
                                                    Log.d("SEND", "Entrou no if do envio do email CHOOSER");
                                                    // AccountListFragment.this.getActivity().startActivity(chooser);
                                                    Log.d("SEND", "Passou no if do envio do email CHOOSER");

                                                    //}
                                                    AccountListFragment.this.getActivity().startActivity(intent);
                                                    Log.d("SEND", "Fim, envio do email");

                                                    return null;
                                                }

                                                @Override
                                                protected void onPostExecute(Object result) {
                                                    listener.onAccountSend();
                                                }
                                            }; // end new AsyncTask

                                    // executa AsyncTask para excluir o contato em rowID
                                    sendTask.execute(new Long[]{rowID});
                                } // end method onClick
                            } // end anonymous inner class
                    ); // end call to method setPositiveButton

                    builder.setNegativeButton(R.string.button_cancel, null);
                    return builder.create(); // retorna o componente AlertDialog
                }
            };

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
    private boolean shouldAskPermission(){

        return(Build.VERSION.SDK_INT>Build.VERSION_CODES.LOLLIPOP_MR1);

    }

} // end class AccountListFragment


