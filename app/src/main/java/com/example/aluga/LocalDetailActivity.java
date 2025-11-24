package com.example.aluga;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocalDetailActivity extends AppCompatActivity implements EquipamentoAdapter.OnEquipmentOptionsClickListener {

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

        detailLocalNome = findViewById(R.id.detail_local_nome);
        detailValorTotal = findViewById(R.id.detail_valor_total);
        detailProximaDevolucao = findViewById(R.id.detail_proxima_devolucao);
        recyclerEquipamentos = findViewById(R.id.recycler_equipamentos);

        recyclerEquipamentos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EquipamentoAdapter(equipamentosList, this);
        recyclerEquipamentos.setAdapter(adapter);

        localId = getIntent().getStringExtra(EXTRA_LOCAL_ID);
        String localNome = getIntent().getStringExtra(EXTRA_LOCAL_NOME);

        if (localNome != null) {
            detailLocalNome.setText(localNome);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                        equipamento.put("id", document.getId());
                        equipamentosList.add(equipamento);

                        Double valor = document.getDouble("valorTotalAluguel");
                        if (valor != null) {
                            valorTotal += valor;
                        }

                        long dataDevolucaoMillis = calcularDataDevolucaoMillis(document);
                        if (dataDevolucaoMillis < proximaDataMillis) {
                            proximaDataMillis = dataDevolucaoMillis;
                        }
                    }

                    detailValorTotal.setText(String.format(Locale.getDefault(), "Valor Total Alugado: R$ %.2f", valorTotal));

                    if (proximaDataMillis == Long.MAX_VALUE) {
                        detailProximaDevolucao.setText("Próxima Devolução: --");
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        detailProximaDevolucao.setText("Próxima Devolução: " + sdf.format(new Date(proximaDataMillis)));
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseDetailError", "Erro ao buscar equipamentos", e);
                    Toast.makeText(this, "Erro ao buscar equipamentos.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onEditClick(Map<String, Object> equipment) {
        Intent intent = new Intent(this, LocarEquipamentoActivity.class);
        intent.putExtra("LOCAL_ID", localId);
        intent.putExtra("EQUIPMENT_ID", (String) equipment.get("id"));
        intent.putExtra("EQUIPMENT_DATA", (Serializable) equipment);
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Map<String, Object> equipment) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Registro")
                .setMessage("Tem certeza que deseja excluir o registro deste equipamento? Esta ação não pode ser desfeita.")
                .setPositiveButton("Sim, Excluir", (dialog, which) -> {
                    excluirEquipamentoDoFirebase((String) equipment.get("id"));
                })
                .setNegativeButton("Não", null).show();
    }

    @Override
    public void onReturnClick(Map<String, Object> equipment) {
        showDevolucaoDialog(equipment);
    }

    private void showDevolucaoDialog(Map<String, Object> equipment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_devolucao_parcial, null);
        builder.setView(dialogView);

        final EditText inputQuantidade = dialogView.findViewById(R.id.input_quantidade_devolver);
        final TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        final Long quantidadeAtual = (Long) equipment.get("quantidadeLocada");

        dialogMessage.setText("Quantidade a devolver (Máx: " + quantidadeAtual + "):");

        builder.setTitle("Devolução Parcial")
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    String qtdStr = inputQuantidade.getText().toString();
                    if (TextUtils.isEmpty(qtdStr)) {
                        Toast.makeText(this, "Por favor, insira uma quantidade.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        long qtdADevolver = Long.parseLong(qtdStr);
                        if (qtdADevolver <= 0 || qtdADevolver > quantidadeAtual) {
                            Toast.makeText(this, "Quantidade inválida.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        processarDevolucao((String) equipment.get("id"), quantidadeAtual, qtdADevolver);

                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Número inválido.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.create().show();
    }

    private void processarDevolucao(String equipmentId, long quantidadeAtual, long qtdADevolver) {
        DocumentReference equipmentRef = db.collection("locais").document(localId)
                .collection("equipamentos_locados").document(equipmentId);

        if (qtdADevolver == quantidadeAtual) {
            // Devolveu tudo, então exclui o registro
            equipmentRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Equipamento devolvido com sucesso!", Toast.LENGTH_SHORT).show();
                        carregarDetalhesDoLocal();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Erro ao processar devolução total.", Toast.LENGTH_SHORT).show());
        } else {
            // Devolveu uma parte, então atualiza a quantidade
            long novaQuantidade = quantidadeAtual - qtdADevolver;
            equipmentRef.update("quantidadeLocada", novaQuantidade)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, qtdADevolver + " ite(ns) devolvido(s) com sucesso!", Toast.LENGTH_SHORT).show();
                        carregarDetalhesDoLocal();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Erro ao processar devolução parcial.", Toast.LENGTH_SHORT).show());
        }
    }

    private void excluirEquipamentoDoFirebase(String equipmentId) {
        if (localId == null || equipmentId == null) {
            Toast.makeText(this, "Erro: ID inválido.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("locais").document(localId)
                .collection("equipamentos_locados").document(equipmentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registro excluído com sucesso!", Toast.LENGTH_SHORT).show();
                    carregarDetalhesDoLocal();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao excluir o registro.", Toast.LENGTH_SHORT).show();
                });
    }

    private long calcularDataDevolucaoMillis(QueryDocumentSnapshot document) {
        String dataAluguelStr = document.getString("dataAluguel");
        Long prazoQtd = document.getLong("prazoQuantidade");
        String prazoUnidade = document.getString("prazoUnidade");
        if (dataAluguelStr == null || prazoQtd == null || prazoUnidade == null) return Long.MAX_VALUE;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(dataAluguelStr));
            if (prazoUnidade.equalsIgnoreCase("dias")) cal.add(Calendar.DAY_OF_MONTH, prazoQtd.intValue());
            else if (prazoUnidade.equalsIgnoreCase("semanas")) cal.add(Calendar.WEEK_OF_YEAR, prazoQtd.intValue());
            else if (prazoUnidade.equalsIgnoreCase("meses")) cal.add(Calendar.MONTH, prazoQtd.intValue());
            return cal.getTimeInMillis();
        } catch (ParseException e) {
            return Long.MAX_VALUE;
        }
    }
}
