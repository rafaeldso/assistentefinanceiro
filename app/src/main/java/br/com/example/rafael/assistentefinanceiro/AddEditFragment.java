// AddEditFragment.java
// Permite ao usuário adicionar um novo contato ou editar um já existente
package br.com.example.rafael.assistentefinanceiro;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class AddEditFragment extends Fragment {
    // método de callback implementado por MainActivity
    public interface AddEditFragmentListener {
        // chamado após a conclusão da edição para que o contato possa ser reexibido
        public void onAddEditCompleted(long rowID);
    }

    private AddEditFragmentListener listener;

    private long rowID; // identificador de linha do contato no banco de dados
    private Bundle accountInfoBundle; // argumentos para editar um contato

    // componentes EditText para informações de contato
    private EditText nameEditText;
    private EditText descriptionEditText;
    private EditText typeEditText;
    private EditText valueEditText;
    private Spinner typeSpinner;

    // configura AddEditFragmentListener quando o fragmento é anexado
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (AddEditFragmentListener) activity;
    }

    // remove AddEditFragmentListener quando o fragmento é desanexado
    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    // chamado quando a view do fragmento precisa ser criada
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setRetainInstance(true); // salva o fragmento entre mudanças de configuração
        setHasOptionsMenu(true); // o fragmento tem itens de menu a exibir

        // infla a interface gráfica do usuário e obtém referências para os componentes
        // EditText
        View view =
                inflater.inflate(R.layout.fragment_add_edit, container, false);
        nameEditText = (EditText) view.findViewById(R.id.nameEditText);
        descriptionEditText = (EditText) view.findViewById(R.id.descriptionEditText);
        //typeEditText = (EditText) view.findViewById(R.id.typeEditText);
        valueEditText = (EditText) view.findViewById(R.id.valueEditText);
        typeSpinner = (Spinner) view.findViewById(R.id.spinner);

        //typeSpinner.setPrompt(getResources().getString(R.string.prompt));
        // Spinner spinner = (Spinner) findViewById(R.id.spinner);
// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getActivity(),
                R.array.types_array, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        typeSpinner.setAdapter(adapter);
        typeSpinner.setPrompt("Choose a type");


        accountInfoBundle = getArguments(); // null, se for a criação de uma nova conta

        if (accountInfoBundle != null) {
            rowID = accountInfoBundle.getLong(MainActivity.ROW_ID);
            nameEditText.setText(accountInfoBundle.getString("name"));
            descriptionEditText.setText(accountInfoBundle.getString("description"));
            //typeEditText.setText(accountInfoBundle.getString("type"));
            valueEditText.setText(accountInfoBundle.getString("value"));
            String compareValue = accountInfoBundle.getString("type");
            Log.d("SPINNER", "Populando o Spinner, Inicio "+compareValue);
             ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(this.getActivity(), R.array.types_array, android.R.layout.simple_spinner_item);
             adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
             typeSpinner.setAdapter(adapter1);
             if (!compareValue.equals(null)) {
             int spinnerPosition = adapter.getPosition(compareValue);
             typeSpinner.setSelection(spinnerPosition);
             }

        }

        // configura o receptor de eventos do componente Button Save Conta
        Button saveContaButton =
                (Button) view.findViewById(R.id.saveAccountButton);
        saveContaButton.setOnClickListener(saveAccountButtonClicked);
        return view;
    }

    // responde ao evento gerado quando o usuário salva uma conta
    OnClickListener saveAccountButtonClicked = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (nameEditText.getText().toString().trim().length() != 0) {
                // AsyncTask para salvar contato e, então, notificar o receptor
                AsyncTask<Object, Object, Object> saveAccountTask =
                        new AsyncTask<Object, Object, Object>() {
                            @Override
                            protected Object doInBackground(Object... params) {
                                saveAccount(); // salvando uma conta no banco de dados database
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Object result) {
                                // oculta o teclado virtual
                                InputMethodManager imm = (InputMethodManager)
                                        getActivity().getSystemService(
                                                Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(
                                        getView().getWindowToken(), 0);

                                listener.onAddEditCompleted(rowID);
                            }
                        }; // end AsyncTask

                // salva o contato no banco de dados usando uma thread separada
                saveAccountTask.execute((Object[]) null);
            } else // o nome do contato obrigatório está em branco; portanto, exibe
            // caixa de diálogo de erro
            {
                DialogFragment errorSaving =
                        new DialogFragment() {
                            @Override
                            public Dialog onCreateDialog(Bundle savedInstanceState) {
                                AlertDialog.Builder builder =
                                        new AlertDialog.Builder(getActivity());
                                builder.setMessage(R.string.error_message);
                                builder.setPositiveButton(R.string.ok, null);
                                return builder.create();
                            }
                        };

                errorSaving.show(getFragmentManager(), "error saving account");
            }
        } // end method onClick
    }; // end OnClickListener saveAccountButtonClicked

    // salva informações de uma conta no banco de dados
    private void saveAccount() {
        // obtém DatabaseConnector para interagir com o banco de dados SQLite
        DatabaseConnector databaseConnector =
                new DatabaseConnector(getActivity());
        // Spinner mySpinner = (Spinner) getView().findViewById(R.id.spinner);
        // String type = mySpinner.getSelectedItem().toString();

        if (accountInfoBundle == null) {
            // insere as informações do contato no banco de dados

            rowID = databaseConnector.insertAccount(
                    nameEditText.getText().toString(),
                    descriptionEditText.getText().toString(),
                    //typeEditText.getText().toString()
                    typeSpinner.getSelectedItem().toString(),
                    valueEditText.getText().toString());

        } else {
            databaseConnector.updateAccount(rowID,
                    nameEditText.getText().toString(),
                    descriptionEditText.getText().toString(),
                    //typeEditText.getText().toString(),
                    typeSpinner.getSelectedItem().toString(),
                    valueEditText.getText().toString());

        }
    } // end method saveAccount
} // end class AddEditFragment


