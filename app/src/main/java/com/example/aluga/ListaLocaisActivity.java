package com.example.aluga;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Map;

public class ListaLocaisActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private ArrayList<Map<String, Object>> locaisList = new ArrayList<>();
    private LocaisAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_locais);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.lista_locais_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LocaisAdapter(locaisList);
        recyclerView.setAdapter(adapter);

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
                            locaisList.add(localData);
                        }

                        adapter.notifyDataSetChanged();

                    } else {
                        Log.e("LISTAGEM_FIREBASE", "Erro ao buscar documentos.", task.getException());
                    }
                });
    }
}