package com.example.aluga;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocarEquipamentoActivity extends AppCompatActivity {

    // Componentes da Interface
    private Spinner spinnerLocais, spinnerPrazoUnidade;
    private EditText inputEquipamentoNome, inputQuantidade, inputValor, inputDataAluguel, inputPrazoQuantidade;
    private TextView textDataDevolucao;
    private Button btnLocar;

    private FirebaseFirestore db;

    // Lista para guardar os objetos de Local (ID e Nome)
    private List<LocalSpinnerItem> locaisList = new ArrayList<>();
    private ArrayAdapter<LocalSpinnerItem> locaisAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locar_equipamento);

        db = FirebaseFirestore.getInstance();

        // Associa os componentes da UI
        spinnerLocais = findViewById(R.id.spinner_locais);
        spinnerPrazoUnidade = findViewById(R.id.spinner_prazo_unidade);
        inputEquipamentoNome = findViewById(R.id.equipamento);
        inputQuantidade = findViewById(R.id.quantidadeLocada);
        inputValor = findViewById(R.id.valorequipamento);
        inputDataAluguel = findViewById(R.id.input_data_aluguel);
        inputPrazoQuantidade = findViewById(R.id.input_prazo_quantidade);
        textDataDevolucao = findViewById(R.id.text_data_devolucao);
        btnLocar = findViewById(R.id.btn_locar);

        // Configura o adapter do Spinner de locais
        locaisAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locaisList);
        locaisAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocais.setAdapter(locaisAdapter);

        carregarLocaisDoFirebase();
        configurarListeners();
    }

    private void carregarLocaisDoFirebase() {
        db.collection("locais")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    locaisList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String nome = document.getString("nome");
                        if (id != null && nome != null) {
                            // Adiciona o objeto completo (ID e Nome) à lista
                            locaisList.add(new LocalSpinnerItem(id, nome));
                        }
                    }
                    locaisAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "Erro ao carregar locais", e);
                    Toast.makeText(this, "Erro ao carregar locais.", Toast.LENGTH_SHORT).show();
                });
    }

    private void configurarListeners() {
        inputDataAluguel.setOnClickListener(v -> showDatePickerDialog());
        
        // O listener do botão agora chama a função para salvar
        btnLocar.setOnClickListener(v -> salvarLocacao());

        TextWatcher dateCalculationWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                calcularEAtualizarDataDevolucao();
            }
        };

        inputDataAluguel.addTextChangedListener(dateCalculationWatcher);
        inputPrazoQuantidade.addTextChangedListener(dateCalculationWatcher);

        spinnerPrazoUnidade.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calcularEAtualizarDataDevolucao();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void salvarLocacao() {
        // 1. Coleta dos dados do formulário
        LocalSpinnerItem localSelecionado = (LocalSpinnerItem) spinnerLocais.getSelectedItem();
        String equipamentoNome = inputEquipamentoNome.getText().toString().trim();
        String quantidadeStr = inputQuantidade.getText().toString().trim();
        String valorStr = inputValor.getText().toString().trim();
        String dataAluguel = inputDataAluguel.getText().toString();
        String prazoQtdStr = inputPrazoQuantidade.getText().toString();
        String prazoUnidade = spinnerPrazoUnidade.getSelectedItem().toString();

        // 2. Validação dos campos
        if (localSelecionado == null) {
            Toast.makeText(this, "Selecione um local.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (equipamentoNome.isEmpty() || quantidadeStr.isEmpty() || valorStr.isEmpty() || dataAluguel.isEmpty() || prazoQtdStr.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 3. Conversão de tipos
            int quantidade = Integer.parseInt(quantidadeStr);
            double valor = Double.parseDouble(valorStr.replace(",", "."));
            int prazoQtd = Integer.parseInt(prazoQtdStr);
            String localId = localSelecionado.getId();

            // 4. Criação do mapa de dados para o Firebase
            Map<String, Object> locacaoData = new HashMap<>();
            locacaoData.put("nomeEquipamento", equipamentoNome);
            locacaoData.put("quantidadeLocada", quantidade);
            locacaoData.put("valorTotalAluguel", valor);
            locacaoData.put("dataAluguel", dataAluguel);
            locacaoData.put("prazoQuantidade", prazoQtd);
            locacaoData.put("prazoUnidade", prazoUnidade);

            // 5. Salva na subcoleção do local selecionado
            db.collection("locais").document(localId)
                    .collection("equipamentos_locados")
                    .add(locacaoData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Equipamento locado com sucesso!", Toast.LENGTH_LONG).show();
                        limparFormulario();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FirebaseSaveError", "Erro ao salvar locação", e);
                        Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Verifique os campos de quantidade e valor.", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void limparFormulario() {
        spinnerLocais.setSelection(0);
        inputEquipamentoNome.setText("");
        inputQuantidade.setText("");
        inputValor.setText("");
        inputDataAluguel.setText("");
        inputPrazoQuantidade.setText("");
        spinnerPrazoUnidade.setSelection(0);
        textDataDevolucao.setText("Data de Devolução: --");
    }

    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar dataSelecionada = Calendar.getInstance();
            dataSelecionada.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            inputDataAluguel.setText(sdf.format(dataSelecionada.getTime()));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void calcularEAtualizarDataDevolucao() {
        String dataInicioStr = inputDataAluguel.getText().toString();
        String prazoQtdStr = inputPrazoQuantidade.getText().toString();

        if (dataInicioStr.isEmpty() || prazoQtdStr.isEmpty()) {
            textDataDevolucao.setText("Data de Devolução: --");
            return;
        }

        try {
            int prazoQtd = Integer.parseInt(prazoQtdStr);
            String prazoUnidade = spinnerPrazoUnidade.getSelectedItem().toString();

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(dataInicioStr));

            if (prazoUnidade.equalsIgnoreCase("dias")) {
                cal.add(Calendar.DAY_OF_MONTH, prazoQtd);
            } else if (prazoUnidade.equalsIgnoreCase("semanas")) {
                cal.add(Calendar.WEEK_OF_YEAR, prazoQtd);
            } else if (prazoUnidade.equalsIgnoreCase("meses")) {
                cal.add(Calendar.MONTH, prazoQtd);
            }

            String dataFinalFormatada = sdf.format(cal.getTime());
            textDataDevolucao.setText("Data de Devolução: " + dataFinalFormatada);

        } catch (ParseException | NumberFormatException e) {
            Log.e("DateCalcError", "Erro ao calcular data de devolução", e);
            textDataDevolucao.setText("Data de Devolução: Erro");
        }
    }
    
    // Classe interna para representar os itens do Spinner de Locais
    private static class LocalSpinnerItem {
        private String id;
        private String nome;

        public LocalSpinnerItem(String id, String nome) {
            this.id = id;
            this.nome = nome;
        }

        public String getId() {
            return id;
        }

        // O ArrayAdapter usa este método para exibir o texto no Spinner
        @NonNull
        @Override
        public String toString() {
            return nome;
        }
    }
}
