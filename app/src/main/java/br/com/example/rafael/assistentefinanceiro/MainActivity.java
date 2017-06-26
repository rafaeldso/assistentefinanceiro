package br.com.example.rafael.assistentefinanceiro;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

// Armazena os fragmentos do aplicativo
public class MainActivity extends Activity
        implements AccountListFragment.AccountListFragmentListener,
        DetailsFragment.DetailsFragmentListener,
        AddEditFragment.AddEditFragmentListener {
    // chaves para armazenar identificador de linha no objeto Bundle passado a um fragmento
    public static final String ROW_ID = "row_id";
    private static final int REQUEST_CODE = 0x11;
    private boolean writeAccepted = false;

    AccountListFragment accountListFragment; // exibe a lista de contas

    // exibe AccountListFragment quando MainActivity é carregada
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // retorna se a atividade está sendo restaurada, não precisa recriar a
        // interface gráfica do usuário
        if (savedInstanceState != null)
            return;

        // verifica se o layout contém fragmentContainer (layout para telefone);
        // AccountListFragment é sempre exibido
        if (findViewById(R.id.fragmentContainer) != null) {
            // create AccountListFragment
            accountListFragment = new AccountListFragment();

            // adiciona o fragmento a FrameLayout
            FragmentTransaction transaction =
                    getFragmentManager().beginTransaction();
            transaction.add(R.id.fragmentContainer, accountListFragment);
            transaction.commit(); // causes AccountListFragment to display
        }
    }

    // chamado quando MainActivity recomeça
    @Override
    protected void onResume() {
        super.onResume();

        // se accountListFragment é null, a atividade está sendo executada em tablet;
        // portanto, obtém referência a partir de FragmentManager
        if (accountListFragment == null) {
            accountListFragment =
                    (AccountListFragment) getFragmentManager().findFragmentById(
                            R.id.accountListFragment);
        }
    }

    // apresenta o DetailsFragment quando uma conta é selecionada
    @Override
    public void onAccountSelected(long rowID) {
        if (findViewById(R.id.fragmentContainer) != null) // phone
            displayConta(rowID, R.id.fragmentContainer);
        else // tablet
        {
            getFragmentManager().popBackStack(); // removes top of back stack
            displayConta(rowID, R.id.rightPaneContainer);
        }
    }

    // exibe uma conta
    private void displayConta(long rowID, int viewID) {
        DetailsFragment detailsFragment = new DetailsFragment();

        // especifica rowID como argumento para DetailsFragment
        Bundle arguments = new Bundle();
        arguments.putLong(ROW_ID, rowID);
        detailsFragment.setArguments(arguments);

        // usa um elemento FragmentTransaction para exibir o componente DetailsFragment
        FragmentTransaction transaction =
                getFragmentManager().beginTransaction();
        transaction.replace(viewID, detailsFragment);
        transaction.addToBackStack(null);
        transaction.commit(); // faz DetailsFragment aparecer
    }

    // exibe o AddEditFragment para add uma nova conta
    @Override
    public void onAddAccount() {
        if (findViewById(R.id.fragmentContainer) != null)
            displayAddEditFragment(R.id.fragmentContainer, null);
        else
            displayAddEditFragment(R.id.rightPaneContainer, null);
    }

    @Override
    public void onAccountSend() {
        getFragmentManager().popBackStack(); // removes top of back stack

        if (findViewById(R.id.fragmentContainer) == null) // tablet
            accountListFragment.updateAccountList();
        //Toast.makeText(getApplicationContext(), "Contas Enviadas", Toast.LENGTH_SHORT).show();
    }

    // exibe fragmento para adicionar um novo contato ou editar um já existente
    private void displayAddEditFragment(int viewID, Bundle arguments) {
        AddEditFragment addEditFragment = new AddEditFragment();

        if (arguments != null) // editando um conta existente
            addEditFragment.setArguments(arguments);

        // usa um elemento FragmentTransaction para exibir o componente AddEditFragment
        FragmentTransaction transaction =
                getFragmentManager().beginTransaction();
        transaction.replace(viewID, addEditFragment);
        transaction.addToBackStack(null);
        transaction.commit(); // causes AddEditFragment to display
    }

    // retorna à lista de contatos quando exibiu uma conta excluída
    @Override
    public void onAccountDeleted() {
        getFragmentManager().popBackStack(); // removes top of back stack

        if (findViewById(R.id.fragmentContainer) == null) // tablet
            accountListFragment.updateAccountList();
        Toast.makeText(getApplicationContext(), "Account Deleted", Toast.LENGTH_SHORT).show();
    }

    // exibe o componente AddEditFragment para editar uma conta já existente
    @Override
    public void onEditAccount(Bundle arguments) {
        if (findViewById(R.id.fragmentContainer) != null) // phone
            displayAddEditFragment(R.id.fragmentContainer, arguments);
        else // tablet
            displayAddEditFragment(R.id.rightPaneContainer, arguments);
    }

    // atualiza a interface gráfica do usuário após um contato novo ou atualizado
// ser salvo
    @Override
    public void onAddEditCompleted(long rowID) {
        getFragmentManager().popBackStack(); // removes top of back stack

        if (findViewById(R.id.fragmentContainer) == null) // tablet
        {
            getFragmentManager().popBackStack(); // removes top of back stack
            accountListFragment.updateAccountList(); // refresh conta

            // em tablet, exibe o contato que acabou de ser adicionado ou editado
            displayConta(rowID, R.id.rightPaneContainer);
        }
    }
    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){
/**
         switch(permsRequestCode){

         case REQUEST_CODE:

         boolean writeAccepted = grantResults[0]== PackageManager.PERMISSION_GRANTED;

         break;

         }**/
        Log.d("SEND","Entrou no onRequestPermissions write="+isWriteAccepted());
        //super.onRequestPermissionsResult(permsRequestCode, permissions, grantResults);
        if (permsRequestCode == REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // save file
                Log.d("SEND","Irá salvar o arquivo");
                writeAccepted = true;
                Log.d("SEND","Write "+isWriteAccepted());

            } else {
                Toast.makeText(MainActivity.this.getApplicationContext(), "PERMISSION_DENIED", Toast.LENGTH_SHORT).show();
            }
            return;
        }

    }

    public boolean isWriteAccepted() {
        return writeAccepted;
    }
}
