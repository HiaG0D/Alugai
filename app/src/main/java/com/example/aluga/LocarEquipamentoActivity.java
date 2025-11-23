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
    private Spinner spinnerLocais, spinnerPeriodoAluguel;
    private EditText inputEquipamentoNome, inputQuantidade, inputValor, inputDataAluguel;
    private TextView textDataDevolucao;
    private Button btnLocar;

    private FirebaseFirestore db;

    private List<LocalSpinnerItem> locaisList = new ArrayList<>();
    private ArrayAdapter<LocalSpinnerItem> locaisAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locar_equipamento);

        db = FirebaseFirestore.getInstance();

        // Associa os componentes da UI
        spinnerLocais = findViewById(R.id.spinner_locais);
        spinnerPeriodoAluguel = findViewById(R.id.spinner_periodo_aluguel);
        inputEquipamentoNome = findViewById(R.id.equipamento);
        inputQuantidade = findViewById(R.id.quantidadeLocada);
        inputValor = findViewById(R.id.valorequipamento);
        inputDataAluguel = findViewById(R.id.input_data_aluguel);
        textDataDevolucao = findViewById(R.id.text_data_devolucao);
        btnLocar = findViewById(R.id.btn_locar);

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
        btnLocar.setOnClickListener(v -> salvarLocacao());

        // Listener para o campo de data (para recalcular ao mudar)
        inputDataAluguel.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                calcularEAtualizarDataDevolucao();
            }
        });

        // Listener para o novo spinner de período
        spinnerPeriodoAluguel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calcularEAtualizarDataDevolucao();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void salvarLocacao() {
        LocalSpinnerItem localSelecionado = (LocalSpinnerItem) spinnerLocais.getSelectedItem();
        String equipamentoNome = inputEquipamentoNome.getText().toString().trim();
        String quantidadeStr = inputQuantidade.getText().toString().trim();
        String valorStr = inputValor.getText().toString().trim();
        String dataAluguel = inputDataAluguel.getText().toString();
        String periodoSelecionado = spinnerPeriodoAluguel.getSelectedItem().toString();

        if (localSelecionado == null) {
            Toast.makeText(this, "Selecione um local.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (equipamentoNome.isEmpty() || quantidadeStr.isEmpty() || valorStr.isEmpty() || dataAluguel.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int quantidade = Integer.parseInt(quantidadeStr);
            double valor = Double.parseDouble(valorStr.replace(",", "."));
            String localId = localSelecionado.getId();

            // Traduz a seleção do Spinner para o formato que já usamos no DB
            int prazoQtd = 1;
            String prazoUnidade = "dias"; // Padrão
            if (periodoSelecionado.equalsIgnoreCase("Semanal")) {
                prazoUnidade = "semanas";
            } else if (periodoSelecionado.equalsIgnoreCase("Mensal")) {
                prazoUnidade = "meses";
            }

            Map<String, Object> locacaoData = new HashMap<>();
            locacaoData.put("nomeEquipamento", equipamentoNome);
            locacaoData.put("quantidadeLocada", quantidade);
            locacaoData.put("valorTotalAluguel", valor);
            locacaoData.put("dataAluguel", dataAluguel);
            locacaoData.put("prazoQuantidade", prazoQtd);
            locacaoData.put("prazoUnidade", prazoUnidade);

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
        // Não limpa o local selecionado, para facilitar múltiplos cadastros
        inputEquipamentoNome.setText("");
        inputQuantidade.setText("");
        inputValor.setText("");
        inputDataAluguel.setText("");
        spinnerPeriodoAluguel.setSelection(0);
        textDataDevolucao.setText("Previsão de Devolução: --");
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
        if (dataInicioStr.isEmpty()) {
            textDataDevolucao.setText("Previsão de Devolução: --");
            return;
        }

        try {
            String periodoSelecionado = spinnerPeriodoAluguel.getSelectedItem().toString();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(dataInicioStr));

            if (periodoSelecionado.equalsIgnoreCase("Diário")) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
            } else if (periodoSelecionado.equalsIgnoreCase("Semanal")) {
                cal.add(Calendar.WEEK_OF_YEAR, 1);
            } else if (periodoSelecionado.equalsIgnoreCase("Mensal")) {
                cal.add(Calendar.MONTH, 1);
            }

            String dataFinalFormatada = sdf.format(cal.getTime());
            textDataDevolucao.setText("Previsão de Devolução: " + dataFinalFormatada);

        } catch (ParseException e) {
            Log.e("DateCalcError", "Erro ao calcular data de devolução", e);
            textDataDevolucao.setText("Previsão de Devolução: Erro");
        }
    }

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

        @NonNull
        @Override
        public String toString() {
            return nome;
        }
    }
}
