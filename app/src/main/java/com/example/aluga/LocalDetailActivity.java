package com.example.aluga;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocalDetailActivity extends AppCompatActivity {

    public static final String EXTRA_LOCAL_ID = "EXTRA_LOCAL_ID";
    public static final String EXTRA_LOCAL_NOME = "EXTRA_LOCAL_NOME";

    private TextView detailLocalNome, detailValorTotal, detailProximaDevolucao;
    private RecyclerView recyclerEquipamentos;

    private FirebaseFirestore db;
    private EquipamentoAdapter adapter;
    private List<Map<String, Object>> equipamentosList = new ArrayList<>();

    private String localId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_detail);

        db = FirebaseFirestore.getInstance();

        // Inicializa os componentes da UI
        detailLocalNome = findViewById(R.id.detail_local_nome);
        detailValorTotal = findViewById(R.id.detail_valor_total);
        detailProximaDevolucao = findViewById(R.id.detail_proxima_devolucao);
        recyclerEquipamentos = findViewById(R.id.recycler_equipamentos);

        // Configura o RecyclerView
        recyclerEquipamentos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EquipamentoAdapter(equipamentosList);
        recyclerEquipamentos.setAdapter(adapter);

        // Pega os dados da Intent
        localId = getIntent().getStringExtra(EXTRA_LOCAL_ID);
        String localNome = getIntent().getStringExtra(EXTRA_LOCAL_NOME);

        if (localNome != null) {
            detailLocalNome.setText(localNome);
        }

        // Verifica se o ID do local é válido antes de carregar os dados
        if (localId != null && !localId.isEmpty()) {
            carregarDetalhesDoLocal();
        } else {
            Toast.makeText(this, "ID do local não encontrado.", Toast.LENGTH_SHORT).show();
        }
    }

    private void carregarDetalhesDoLocal() {
        db.collection("locais").document(localId)
                .collection("equipamentos_locados")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    equipamentosList.clear();
                    double valorTotal = 0.0;
                    long proximaDataMillis = Long.MAX_VALUE;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> equipamento = document.getData();
                        equipamentosList.add(equipamento);

                        // Calcula o valor total
                        Double valor = document.getDouble("valorTotalAluguel");
                        if (valor != null) {
                            valorTotal += valor;
                        }

                        // Encontra a data de devolução mais próxima
                        long dataDevolucaoMillis = calcularDataDevolucaoMillis(document);
                        if (dataDevolucaoMillis < proximaDataMillis) {
                            proximaDataMillis = dataDevolucaoMillis;
                        }
                    }

                    // Atualiza a UI com os dados calculados
                    detailValorTotal.setText(String.format(Locale.getDefault(), "Valor Total Alugado: R$ %.2f", valorTotal));

                    if (proximaDataMillis == Long.MAX_VALUE) {
                        detailProximaDevolucao.setText("Próxima Devolução: --");
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        detailProximaDevolucao.setText("Próxima Devolução: " + sdf.format(new Date(proximaDataMillis)));
                    }

                    adapter.notifyDataSetChanged(); // Notifica o adapter que a lista mudou
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseDetailError", "Erro ao buscar equipamentos", e);
                    Toast.makeText(this, "Erro ao buscar equipamentos.", Toast.LENGTH_SHORT).show();
                });
    }

    private long calcularDataDevolucaoMillis(QueryDocumentSnapshot document) {
        String dataAluguelStr = document.getString("dataAluguel");
        Long prazoQtd = document.getLong("prazoQuantidade");
        String prazoUnidade = document.getString("prazoUnidade");

        if (dataAluguelStr == null || prazoQtd == null || prazoUnidade == null) {
            return Long.MAX_VALUE;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(dataAluguelStr));

            if (prazoUnidade.equalsIgnoreCase("dias")) {
                cal.add(Calendar.DAY_OF_MONTH, prazoQtd.intValue());
            } else if (prazoUnidade.equalsIgnoreCase("semanas")) {
                cal.add(Calendar.WEEK_OF_YEAR, prazoQtd.intValue());
            } else if (prazoUnidade.equalsIgnoreCase("meses")) {
                cal.add(Calendar.MONTH, prazoQtd.intValue());
            }
            return cal.getTimeInMillis();
        } catch (ParseException e) {
            return Long.MAX_VALUE;
        }
    }
}
