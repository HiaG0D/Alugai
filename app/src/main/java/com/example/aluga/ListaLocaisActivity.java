package com.example.aluga;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 1. Implementa a interface de clique nas opções, além da de clique no item
public class ListaLocaisActivity extends AppCompatActivity implements LocaisAdapter.OnItemClickListener, LocaisAdapter.OnOptionsClickListener {

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private List<Map<String, Object>> locaisList = new ArrayList<>();
    private LocaisAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_locais);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.lista_locais_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 2. Passa "this" como o listener para AMBAS as interfaces
        adapter = new LocaisAdapter(locaisList, this, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarrega os locais sempre que a tela se torna visível
        carregarLocais();
    }

    private void carregarLocais() {
        db.collection("locais")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        locaisList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> localData = document.getData();
                            localData.put("id", document.getId());
                            locaisList.add(localData);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.e("LISTAGEM_FIREBASE", "Erro ao buscar documentos.", task.getException());
                    }
                });
    }

    // --- Implementação dos Métodos de Clique ---

    // Clique no item inteiro (para ver detalhes)
    @Override
    public void onItemClick(Map<String, Object> local) {
        Intent intent = new Intent(this, LocalDetailActivity.class);
        intent.putExtra(LocalDetailActivity.EXTRA_LOCAL_ID, (String) local.get("id"));
        intent.putExtra(LocalDetailActivity.EXTRA_LOCAL_NOME, (String) local.get("nome"));
        startActivity(intent);
    }

    // Clique na opção "Editar" do menu
    @Override
    public void onEditClick(Map<String, Object> local) {
        Intent intent = new Intent(this, CadastroLocalActivity.class);
        // Passa os dados do local para a tela de cadastro
        intent.putExtra("LOCAL_ID", (String) local.get("id"));
        intent.putExtra("NOME_ATUAL", (String) local.get("nome"));
        intent.putExtra("ENDERECO_ATUAL", (String) local.get("endereco"));
        startActivity(intent);
    }

    // Clique na opção "Excluir" do menu
    @Override
    public void onDeleteClick(Map<String, Object> local) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Local")
                .setMessage("Tem certeza que deseja excluir o local '" + local.get("nome") + "'?\n\nTodos os equipamentos alugados para este local também serão excluídos.")
                .setPositiveButton("Sim, Excluir", (dialog, which) -> {
                    excluirLocalDoFirebase((String) local.get("id"));
                })
                .setNegativeButton("Não", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void excluirLocalDoFirebase(String localId) {
        if (localId == null || localId.isEmpty()) {
            Toast.makeText(this, "Erro: ID do local inválido.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference localRef = db.collection("locais").document(localId);
        CollectionReference equipamentosRef = localRef.collection("equipamentos_locados");

        // Primeiro, exclui todos os documentos da subcoleção
        equipamentosRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WriteBatch batch = db.batch();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    batch.delete(document.getReference());
                }
                // Executa a exclusão em lote da subcoleção
                batch.commit().addOnCompleteListener(batchTask -> {
                    if (batchTask.isSuccessful()) {
                        // Se a subcoleção foi excluída, exclui o documento principal do local
                        localRef.delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(ListaLocaisActivity.this, "Local excluído com sucesso!", Toast.LENGTH_SHORT).show();
                                    carregarLocais(); // Recarrega a lista
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(ListaLocaisActivity.this, "Erro ao excluir o local.", Toast.LENGTH_SHORT).show();
                                    Log.e("DELETE_ERROR", "Erro ao excluir documento principal", e);
                                });
                    } else {
                        Toast.makeText(ListaLocaisActivity.this, "Erro ao excluir equipamentos do local.", Toast.LENGTH_SHORT).show();
                        Log.e("DELETE_ERROR", "Erro no batch commit", batchTask.getException());
                    }
                });
            } else {
                Toast.makeText(ListaLocaisActivity.this, "Erro ao encontrar equipamentos para excluir.", Toast.LENGTH_SHORT).show();
                Log.e("DELETE_ERROR", "Erro ao buscar subcoleção", task.getException());
            }
        });
    }
}
