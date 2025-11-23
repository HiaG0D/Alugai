package com.example.aluga;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CadastroLocalActivity extends AppCompatActivity {

    // Componentes da UI
    private EditText nomeInput, enderecoInput;
    private Button btnSalvar;

    // Firebase
    private FirebaseFirestore db;

    // Estado da Activity
    private boolean isEditMode = false;
    private String localIdParaEditar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        db = FirebaseFirestore.getInstance();

        // Associa os componentes da UI
        nomeInput = findViewById(R.id.nome);
        enderecoInput = findViewById(R.id.endereco);
        btnSalvar = findViewById(R.id.btn_salvar);

        // Verifica se a Activity foi iniciada em modo de edição
        if (getIntent().hasExtra("LOCAL_ID")) {
            isEditMode = true;
            localIdParaEditar = getIntent().getStringExtra("LOCAL_ID");
            String nomeAtual = getIntent().getStringExtra("NOME_ATUAL");
            String enderecoAtual = getIntent().getStringExtra("ENDERECO_ATUAL");

            // Preenche os campos com os dados existentes
            nomeInput.setText(nomeAtual);
            enderecoInput.setText(enderecoAtual);
            btnSalvar.setText("Atualizar"); // Muda o texto do botão
        }

        // Configura o listener do botão
        btnSalvar.setOnClickListener(v -> {
            if (isEditMode) {
                atualizarLocal();
            } else {
                salvarNovoLocal();
            }
        });
    }

    private void salvarNovoLocal() {
        String nome = nomeInput.getText().toString().trim();
        String endereco = enderecoInput.getText().toString().trim();

        if (nome.isEmpty() || endereco.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> local = new HashMap<>();
        local.put("nome", nome);
        local.put("endereco", endereco);

        db.collection("locais").add(local)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Local cadastrado com sucesso!", Toast.LENGTH_SHORT).show();
                    finish(); // Fecha a tela e volta para a lista
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao cadastrar local.", Toast.LENGTH_SHORT).show();
                    Log.e("CADASTRO_ERRO", "Erro: ", e);
                });
    }

    private void atualizarLocal() {
        String nome = nomeInput.getText().toString().trim();
        String endereco = enderecoInput.getText().toString().trim();

        if (nome.isEmpty() || endereco.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> local = new HashMap<>();
        local.put("nome", nome);
        local.put("endereco", endereco);

        db.collection("locais").document(localIdParaEditar)
                .update(local)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Local atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                    finish(); // Fecha a tela e volta para a lista
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao atualizar local.", Toast.LENGTH_SHORT).show();
                    Log.e("UPDATE_ERRO", "Erro: ", e);
                });
    }
}
