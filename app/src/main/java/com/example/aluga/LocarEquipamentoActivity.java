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

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class LocarEquipamentoActivity extends AppCompatActivity {

    // Componentes da Interface
    private Spinner spinnerLocais, spinnerPrazoUnidade;
    private EditText inputDataAluguel, inputPrazoQuantidade;
    private TextView textDataDevolucao;
    private Button btnLocar;

    // Firebase
    private FirebaseFirestore db;

    // Adaptador para o Spinner de Locais
    private ArrayAdapter<String> locaisAdapter;
    private List<String> nomesLocais = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locar_equipamento);

        // Inicializa o Firebase
        db = FirebaseFirestore.getInstance();

        // Associa os componentes da UI com as variáveis
        spinnerLocais = findViewById(R.id.spinner_locais);
        spinnerPrazoUnidade = findViewById(R.id.spinner_prazo_unidade);
        inputDataAluguel = findViewById(R.id.input_data_aluguel);
        inputPrazoQuantidade = findViewById(R.id.input_prazo_quantidade);
        textDataDevolucao = findViewById(R.id.text_data_devolucao);
        btnLocar = findViewById(R.id.btn_locar);

        // Configura o adapter do Spinner de locais
        locaisAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nomesLocais);
        locaisAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocais.setAdapter(locaisAdapter);

        // Carrega os locais do Firebase para o Spinner
        carregarLocaisDoFirebase();

        // Configura os listeners para os botões e campos
        configurarListeners();
    }

    /**
     * Busca a coleção "locais" no Firebase e preenche o Spinner.
     */
    private void carregarLocaisDoFirebase() {
        db.collection("locais")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    nomesLocais.clear(); // Limpa a lista antes de adicionar novos itens
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Pega o valor do campo "nome" de cada documento
                        String nome = document.getString("nome");
                        if (nome != null && !nome.isEmpty()) {
                            nomesLocais.add(nome);
                        }
                    }
                    // Notifica o adapter que os dados mudaram
                    locaisAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "Erro ao carregar locais", e);
                    Toast.makeText(this, "Erro ao carregar locais.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Agrupa a configuração de todos os listeners da Activity.
     */
    private void configurarListeners() {
        // Listener para o campo de data
        inputDataAluguel.setOnClickListener(v -> showDatePickerDialog());

        // Listener para o botão "Locar"
        btnLocar.setOnClickListener(v -> {
            Toast.makeText(LocarEquipamentoActivity.this, "Lógica de salvar no Firebase virá aqui!", Toast.LENGTH_SHORT).show();
        });

        // Listeners para o cálculo da data de devolução
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

    /**
     * Abre o calendário para seleção de data.
     */
    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar dataSelecionada = Calendar.getInstance();
            dataSelecionada.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            inputDataAluguel.setText(sdf.format(dataSelecionada.getTime()));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Lê os campos de data e prazo, calcula a data final e atualiza o TextView.
     */
    private void calcularEAtualizarDataDevolucao() {
        String dataInicioStr = inputDataAluguel.getText().toString();
        String prazoQtdStr = inputPrazoQuantidade.getText().toString();

        // Só continua se os campos de data e quantidade de prazo não estiverem vazios
        if (dataInicioStr.isEmpty() || prazoQtdStr.isEmpty()) {
            textDataDevolucao.setText("Data de Devolução: --");
            return;
        }

        try {
            int prazoQtd = Integer.parseInt(prazoQtdStr);
            String prazoUnidade = spinnerPrazoUnidade.getSelectedItem().toString();

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(dataInicioStr)); // Define a data de início no calendário

            // Adiciona o prazo com base na unidade selecionada
            if (prazoUnidade.equalsIgnoreCase("dias")) {
                cal.add(Calendar.DAY_OF_MONTH, prazoQtd);
            } else if (prazoUnidade.equalsIgnoreCase("semanas")) {
                cal.add(Calendar.WEEK_OF_YEAR, prazoQtd);
            } else if (prazoUnidade.equalsIgnoreCase("meses")) {
                cal.add(Calendar.MONTH, prazoQtd);
            }

            // Formata a data final e atualiza o TextView
            String dataFinalFormatada = sdf.format(cal.getTime());
            textDataDevolucao.setText("Data de Devolução: " + dataFinalFormatada);

        } catch (ParseException | NumberFormatException e) {
            Log.e("DateCalcError", "Erro ao calcular data de devolução", e);
            textDataDevolucao.setText("Data de Devolução: Erro");
        }
    }
}
