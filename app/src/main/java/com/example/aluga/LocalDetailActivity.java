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

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activity para exibir os detalhes de um Local específico, incluindo os equipamentos
 * que estão atualmente alugados e o histórico de equipamentos já devolvidos.
 */
public class LocalDetailActivity extends AppCompatActivity implements EquipamentoAdapter.OnEquipmentOptionsClickListener {

    // Chaves para passar dados via Intent
    public static final String EXTRA_LOCAL_ID = "EXTRA_LOCAL_ID";
    public static final String EXTRA_LOCAL_NOME = "EXTRA_LOCAL_NOME";

    // --- Componentes da UI ---
    private TextView detailLocalNome, detailValorTotal, detailProximaDevolucao;
    private RecyclerView recyclerEquipamentos, recyclerHistorico;

    // --- Firebase & Adapters ---
    private FirebaseFirestore db;
    private EquipamentoAdapter equipamentoAdapter;
    private DevolvidoAdapter historicoAdapter;
    private List<Map<String, Object>> equipamentosList = new ArrayList<>();
    private List<Map<String, Object>> historicoList = new ArrayList<>();

    private String localId; // ID do documento do local no Firebase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_detail);

        db = FirebaseFirestore.getInstance();

        setupViews();

        // Pega os dados passados da tela de lista
        localId = getIntent().getStringExtra(EXTRA_LOCAL_ID);
        String localNome = getIntent().getStringExtra(EXTRA_LOCAL_NOME);

        if (localNome != null) {
            detailLocalNome.setText(localNome);
        }
    }

    /**
     * O ciclo de vida onResume é usado para garantir que os dados sejam recarregados
     * sempre que o usuário retorna a esta tela (por exemplo, após editar um item).
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (localId != null && !localId.isEmpty()) {
            carregarDetalhesDoLocal();
            carregarHistoricoDoLocal();
        } else {
            Toast.makeText(this, "ID do local não encontrado.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Inicializa todos os componentes da interface e configura os RecyclerViews.
     */
    private void setupViews() {
        detailLocalNome = findViewById(R.id.detail_local_nome);
        detailValorTotal = findViewById(R.id.detail_valor_total);
        detailProximaDevolucao = findViewById(R.id.detail_proxima_devolucao);
        
        // Configura a lista de equipamentos atualmente alugados
        recyclerEquipamentos = findViewById(R.id.recycler_equipamentos);
        recyclerEquipamentos.setLayoutManager(new LinearLayoutManager(this));
        equipamentoAdapter = new EquipamentoAdapter(equipamentosList, this);
        recyclerEquipamentos.setAdapter(equipamentoAdapter);

        // Configura a lista do histórico de devoluções
        recyclerHistorico = findViewById(R.id.recycler_historico_devolucoes);
        recyclerHistorico.setLayoutManager(new LinearLayoutManager(this));
        historicoAdapter = new DevolvidoAdapter(historicoList);
        recyclerHistorico.setAdapter(historicoAdapter);
    }

    /**
     * Busca os dados da subcoleção 'equipamentos_locados' no Firebase.
     * Calcula o valor total e a data de devolução mais próxima em tempo real.
     */
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
                        equipamento.put("id", document.getId()); // Adiciona o ID do documento para referência futura
                        equipamentosList.add(equipamento);

                        // Soma o valor total de todos os aluguéis ativos
                        Double valor = document.getDouble("valorTotalAluguel");
                        if (valor != null) {
                            valorTotal += valor;
                        }

                        // Encontra a data de devolução mais próxima entre todos os equipamentos
                        long dataDevolucaoMillis = calcularDataDevolucaoMillis(document);
                        if (dataDevolucaoMillis < proximaDataMillis) {
                            proximaDataMillis = dataDevolucaoMillis;
                        }
                    }

                    // Atualiza os TextViews no topo da tela
                    detailValorTotal.setText(String.format(Locale.getDefault(), "Valor Total Alugado: R$ %.2f", valorTotal));

                    if (proximaDataMillis == Long.MAX_VALUE) {
                        detailProximaDevolucao.setText("Próxima Devolução: --");
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        detailProximaDevolucao.setText("Próxima Devolução: " + sdf.format(new Date(proximaDataMillis)));
                    }

                    equipamentoAdapter.notifyDataSetChanged();
                });
    }

    /**
     * Busca os dados da subcoleção 'historico_devolucoes' e os exibe na lista de histórico.
     */
    private void carregarHistoricoDoLocal() {
        db.collection("locais").document(localId)
                .collection("historico_devolucoes")
                .orderBy("dataDaDevolucao") // Ordena para mostrar os mais antigos primeiro
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historicoList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        historicoList.add(document.getData());
                    }
                    historicoAdapter.notifyDataSetChanged();
                });
    }

    // --- Implementação dos Métodos de Clique do Menu de Equipamento ---

    @Override
    public void onEditClick(Map<String, Object> equipment) {
        Intent intent = new Intent(this, LocarEquipamentoActivity.class);
        intent.putExtra("LOCAL_ID", localId);
        intent.putExtra("EQUIPMENT_ID", (String) equipment.get("id"));
        intent.putExtra("EQUIPMENT_DATA", (Serializable) equipment); // Passa todos os dados para a tela de edição
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

    /**
     * Mostra o diálogo para inserir a quantidade de itens a devolver.
     */
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

                        // Verifica se o valor do aluguel foi definido como unitário ou total
                        Boolean isUnitario = (Boolean) equipment.getOrDefault("isValorUnitario", false);

                        // Se for devolução total ou se o preço for unitário, processa diretamente.
                        if (isUnitario || qtdADevolver == quantidadeAtual) {
                            processarDevolucao(equipment, qtdADevolver, -1); // -1 indica que o valor pode ser recalculado ou não é necessário
                        } else {
                            // Se o preço for total e a devolução for parcial, pede o novo valor do contrato.
                            promptForNewTotalValue(equipment, qtdADevolver);
                        }

                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Número inválido.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.create().show();
    }

    /**
     * Mostra um segundo diálogo para o usuário inserir o novo valor total do contrato
     * após uma devolução parcial de um item com preço total.
     */
    private void promptForNewTotalValue(Map<String, Object> equipment, long qtdADevolver) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_novo_valor, null);
        builder.setView(dialogView);

        final EditText inputNovoValor = dialogView.findViewById(R.id.input_novo_valor_total);

        builder.setTitle("Ajuste de Valor")
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    String valorStr = inputNovoValor.getText().toString();
                    if (TextUtils.isEmpty(valorStr)) {
                        Toast.makeText(this, "Por favor, insira o novo valor total.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double novoValorTotal = Double.parseDouble(valorStr.replace(",", "."));
                        processarDevolucao(equipment, qtdADevolver, novoValorTotal);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Valor inválido.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .create().show();
    }

    /**
     * Processa a devolução, movendo o item para o histórico e atualizando ou deletando o registro original.
     * @param equipment Os dados do equipamento original.
     * @param qtdADevolver A quantidade de itens a serem devolvidos.
     * @param novoValorTotal O novo valor total do contrato (ou -1 se não for necessário).
     */
    private void processarDevolucao(Map<String, Object> equipment, long qtdADevolver, double novoValorTotal) {
        String equipmentId = (String) equipment.get("id");
        long quantidadeAtual = (long) equipment.get("quantidadeLocada");

        DocumentReference locadoRef = db.collection("locais").document(localId).collection("equipamentos_locados").document(equipmentId);
        CollectionReference historicoRef = db.collection("locais").document(localId).collection("historico_devolucoes");

        // Usa um WriteBatch para garantir que todas as operações sejam atômicas (ou tudo funciona, ou nada funciona)
        WriteBatch batch = db.batch();

        // Prepara o novo item para o histórico
        Map<String, Object> historicoItem = new HashMap<>(equipment);
        historicoItem.remove("id"); // Remove o ID temporário do mapa de dados
        historicoItem.put("quantidadeDevolvida", qtdADevolver);
        historicoItem.put("dataDaDevolucao", FieldValue.serverTimestamp()); // Usa o timestamp do servidor para a data

        // Adiciona o novo item ao histórico
        batch.set(historicoRef.document(), historicoItem);

        if (qtdADevolver == quantidadeAtual) {
            // Se a devolução é total, deleta o registro original de equipamentos_locados
            batch.delete(locadoRef);
        } else {
            // Se a devolução é parcial, atualiza o registro original
            long novaQuantidade = quantidadeAtual - qtdADevolver;
            Map<String, Object> updates = new HashMap<>();
            updates.put("quantidadeLocada", novaQuantidade);

            // Verifica se o valor precisa ser recalculado
            if (novoValorTotal != -1) {
                // Usa o novo valor total fornecido pelo usuário
                updates.put("valorTotalAluguel", novoValorTotal);
            } else {
                // Se o valor era unitário, recalcula automaticamente
                double valorTotalAtual = (double) equipment.get("valorTotalAluguel");
                double valorUnitario = valorTotalAtual / quantidadeAtual;
                updates.put("valorTotalAluguel", valorUnitario * novaQuantidade);
            }
            batch.update(locadoRef, updates);
        }

        // Executa todas as operações em lote
        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Devolução processada!", Toast.LENGTH_SHORT).show();
            carregarDetalhesDoLocal(); // Recarrega ambas as listas
            carregarHistoricoDoLocal();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Erro ao processar devolução.", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Exclui permanentemente o registro de um equipamento alugado.
     */
    private void excluirEquipamentoDoFirebase(String equipmentId) {
        if (localId == null || equipmentId == null) return;
        db.collection("locais").document(localId)
                .collection("equipamentos_locados").document(equipmentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registro excluído!", Toast.LENGTH_SHORT).show();
                    carregarDetalhesDoLocal();
                });
    }

    /**
     * Calcula a data de devolução em milissegundos para um dado equipamento.
     */
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
