package com.example.aluga;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activity responsável por criar um novo aluguel de equipamento ou editar um existente.
 * Lida com a lógica de preenchimento do formulário, seleção de período, cálculo de valor e comunicação com o Firebase.
 */
public class LocarEquipamentoActivity extends AppCompatActivity {

    // --- Componentes da UI ---
    private Spinner spinnerLocais, spinnerPeriodoSimples, spinnerPrazoUnidade;
    private EditText inputEquipamentoNome, inputQuantidade, inputValor, inputDataAluguel, inputPrazoQuantidade;
    private TextView textDataDevolucao;
    private Button btnLocar;
    private RadioGroup rgPeriodoModo, rgValorTipo;
    private RadioButton rbPeriodoSimples, rbValorUnitario, rbValorTotal;
    private LinearLayout containerPeriodoCustom;

    // --- Firebase ---
    private FirebaseFirestore db;

    // --- Listas e Adapters ---
    private List<LocalSpinnerItem> locaisList = new ArrayList<>();
    private ArrayAdapter<LocalSpinnerItem> locaisAdapter;

    // --- Estado da Activity ---
    private boolean isEditMode = false; // Flag para controlar se a tela está em modo de criação ou edição
    private String localId, equipmentId; // IDs para identificar os documentos no Firebase durante a edição

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locar_equipamento);

        db = FirebaseFirestore.getInstance();
        setupViews();
        setupListeners();

        // Verifica se a activity foi iniciada com dados de um equipamento para edição
        if (getIntent().hasExtra("EQUIPMENT_ID")) {
            enterEditMode();
        } else {
            // Se não, carrega os locais para um novo aluguel
            carregarLocaisDoFirebase();
        }
    }

    /**
     * Inicializa todas as variáveis de componentes da UI, associando-as com seus IDs no layout XML.
     */
    private void setupViews() {
        spinnerLocais = findViewById(R.id.spinner_locais);
        spinnerPeriodoSimples = findViewById(R.id.spinner_periodo_simples);
        spinnerPrazoUnidade = findViewById(R.id.spinner_prazo_unidade);
        inputEquipamentoNome = findViewById(R.id.equipamento);
        inputQuantidade = findViewById(R.id.quantidadeLocada);
        inputValor = findViewById(R.id.valorequipamento);
        inputDataAluguel = findViewById(R.id.input_data_aluguel);
        inputPrazoQuantidade = findViewById(R.id.input_prazo_quantidade);
        textDataDevolucao = findViewById(R.id.text_data_devolucao);
        btnLocar = findViewById(R.id.btn_locar);
        rgPeriodoModo = findViewById(R.id.rg_periodo_modo);
        rbPeriodoSimples = findViewById(R.id.rb_periodo_simples);
        containerPeriodoCustom = findViewById(R.id.container_periodo_custom);
        rgValorTipo = findViewById(R.id.valor_tipo);
        rbValorUnitario = findViewById(R.id.valor_unitario);
        rbValorTotal = findViewById(R.id.valor_total);

        locaisAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locaisList);
        locaisAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocais.setAdapter(locaisAdapter);
    }

    /**
     * Configura todos os listeners de eventos para os componentes da UI.
     */
    private void setupListeners() {
        inputDataAluguel.setOnClickListener(v -> showDatePickerDialog());
        btnLocar.setOnClickListener(v -> salvarOuAtualizarLocacao());

        // Listener para o seletor de modo de período (Simples vs. Customizado)
        rgPeriodoModo.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_periodo_simples) {
                // Mostra o spinner de período simples e esconde o customizado
                spinnerPeriodoSimples.setVisibility(View.VISIBLE);
                containerPeriodoCustom.setVisibility(View.GONE);
            } else {
                // Mostra os campos de período customizado e esconde o simples
                spinnerPeriodoSimples.setVisibility(View.GONE);
                containerPeriodoCustom.setVisibility(View.VISIBLE);
            }
            calcularEAtualizarDataDevolucao();
        });

        // Watcher para recalcular a data sempre que a data de início ou o prazo mudam
        TextWatcher textWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { calcularEAtualizarDataDevolucao(); }
        };
        inputDataAluguel.addTextChangedListener(textWatcher);
        inputPrazoQuantidade.addTextChangedListener(textWatcher);

        // Listener para os spinners de período, para recalcular a data quando a seleção muda
        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calcularEAtualizarDataDevolucao();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
        spinnerPeriodoSimples.setOnItemSelectedListener(spinnerListener);
        spinnerPrazoUnidade.setOnItemSelectedListener(spinnerListener);
    }

    /**
     * Configura a tela para o modo de edição, preenchendo os campos com os dados existentes.
     */
    private void enterEditMode() {
        isEditMode = true;
        btnLocar.setText("Atualizar");

        localId = getIntent().getStringExtra("LOCAL_ID");
        equipmentId = getIntent().getStringExtra("EQUIPMENT_ID");
        Map<String, Object> equipmentData = (Map<String, Object>) getIntent().getSerializableExtra("EQUIPMENT_DATA");

        if (equipmentData == null) {
            Toast.makeText(this, "Erro: Dados do equipamento não encontrados.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Preenche os campos do formulário com os dados do equipamento
        inputEquipamentoNome.setText((String) equipmentData.get("nomeEquipamento"));
        inputQuantidade.setText(String.valueOf(equipmentData.get("quantidadeLocada")));
        inputValor.setText(String.format(Locale.US, "%.2f", equipmentData.get("valorTotalAluguel")));
        inputDataAluguel.setText((String) equipmentData.get("dataAluguel"));

        // Seleciona o modo de período (Simples ou Customizado) correto
        Long prazoQtd = (Long) equipmentData.get("prazoQuantidade");
        if (prazoQtd != null && prazoQtd == 1) {
            rbPeriodoSimples.setChecked(true);
            setPeriodoSimplesSpinner((String) equipmentData.get("prazoUnidade"));
        } else {
            findViewById(R.id.rb_periodo_custom).performClick();
            inputPrazoQuantidade.setText(String.valueOf(prazoQtd));
            setPeriodoCustomSpinner((String) equipmentData.get("prazoUnidade"));
        }

        // Em modo de edição, o valor é sempre o total, e o seletor é escondido
        rbValorTotal.performClick();
        rgValorTipo.setVisibility(View.GONE);

        // Carrega apenas o local atual no spinner e o desabilita para evitar mudança de local
        carregarLocalUnico(localId);
        spinnerLocais.setEnabled(false);
    }

    /**
     * Coleta os dados do formulário, valida, e decide se deve criar um novo registro ou atualizar um existente.
     */
    private void salvarOuAtualizarLocacao() {
        // 1. Coleta os dados dos campos de texto
        String equipamentoNome = inputEquipamentoNome.getText().toString().trim();
        String quantidadeStr = inputQuantidade.getText().toString().trim();
        String valorStr = inputValor.getText().toString().trim();
        String dataAluguel = inputDataAluguel.getText().toString();

        // 2. Validação de campos obrigatórios
        if (equipamentoNome.isEmpty() || quantidadeStr.isEmpty() || valorStr.isEmpty() || dataAluguel.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 3. Conversão e cálculo dos valores
            int quantidade = Integer.parseInt(quantidadeStr);
            double valorInput = Double.parseDouble(valorStr.replace(",", "."));
            double valorTotalAluguel;

            if (rbValorUnitario.isChecked()) {
                valorTotalAluguel = valorInput * quantidade;
            } else {
                valorTotalAluguel = valorInput;
            }

            // 4. Tradução da seleção de período para os dados a serem salvos
            int prazoQtd;
            String prazoUnidade;

            if (rbPeriodoSimples.isChecked()) {
                String periodoSelecionado = spinnerPeriodoSimples.getSelectedItem().toString();
                prazoQtd = 1;
                if (periodoSelecionado.equalsIgnoreCase("Semanal")) prazoUnidade = "semanas";
                else if (periodoSelecionado.equalsIgnoreCase("Mensal")) prazoUnidade = "meses";
                else prazoUnidade = "dias";
            } else {
                String prazoQtdStr = inputPrazoQuantidade.getText().toString().trim();
                if (prazoQtdStr.isEmpty()) {
                    Toast.makeText(this, "Preencha a quantidade do prazo customizado.", Toast.LENGTH_SHORT).show();
                    return;
                }
                prazoQtd = Integer.parseInt(prazoQtdStr);
                prazoUnidade = spinnerPrazoUnidade.getSelectedItem().toString();
            }

            // 5. Monta o objeto (mapa) com todos os dados para o Firebase
            Map<String, Object> locacaoData = new HashMap<>();
            locacaoData.put("nomeEquipamento", equipamentoNome);
            locacaoData.put("quantidadeLocada", quantidade);
            locacaoData.put("valorTotalAluguel", valorTotalAluguel);
            locacaoData.put("dataAluguel", dataAluguel);
            locacaoData.put("prazoQuantidade", prazoQtd);
            locacaoData.put("prazoUnidade", prazoUnidade);
            locacaoData.put("isValorUnitario", rbValorUnitario.isChecked()); // Salva como o valor foi inserido

            // 6. Decide se atualiza um documento existente ou cria um novo
            if (isEditMode) {
                db.collection("locais").document(localId).collection("equipamentos_locados").document(equipmentId)
                        .update(locacaoData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Equipamento atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                            finish(); // Fecha a tela e volta para os detalhes
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Erro ao atualizar.", Toast.LENGTH_SHORT).show());
            } else {
                LocalSpinnerItem localSelecionado = (LocalSpinnerItem) spinnerLocais.getSelectedItem();
                if (localSelecionado == null) {
                    Toast.makeText(this, "Selecione um local.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String currentLocalId = localSelecionado.getId();
                db.collection("locais").document(currentLocalId).collection("equipamentos_locados")
                        .add(locacaoData)
                        .addOnSuccessListener(docRef -> {
                            Toast.makeText(this, "Equipamento locado com sucesso!", Toast.LENGTH_SHORT).show();
                            limparFormulario(); // Limpa o formulário para a próxima locação
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Erro ao locar.", Toast.LENGTH_SHORT).show());
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Verifique os campos numéricos (quantidade e valor).", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Busca todos os locais do Firebase para preencher o spinner de seleção de local.
     */
    private void carregarLocaisDoFirebase() {
        db.collection("locais").get().addOnSuccessListener(queryDocumentSnapshots -> {
            locaisList.clear();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                locaisList.add(new LocalSpinnerItem(document.getId(), document.getString("nome")));
            }
            locaisAdapter.notifyDataSetChanged();
        });
    }

    /**
     * Em modo de edição, carrega apenas o local atual para exibir no spinner desabilitado.
     */
    private void carregarLocalUnico(String localId) {
        db.collection("locais").document(localId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                locaisList.add(new LocalSpinnerItem(localId, documentSnapshot.getString("nome")));
                locaisAdapter.notifyDataSetChanged();
                spinnerLocais.setSelection(0);
            }
        });
    }

    /**
     * Seleciona o item correto no spinner de período simples com base na unidade de prazo.
     */
    private void setPeriodoSimplesSpinner(String prazoUnidade) {
        if (prazoUnidade == null) return;
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinnerPeriodoSimples.getAdapter();
        if (prazoUnidade.equalsIgnoreCase("semanas")) spinnerPeriodoSimples.setSelection(adapter.getPosition("Semanal"));
        else if (prazoUnidade.equalsIgnoreCase("meses")) spinnerPeriodoSimples.setSelection(adapter.getPosition("Mensal"));
        else spinnerPeriodoSimples.setSelection(adapter.getPosition("Diário"));
    }

    /**
     * Seleciona o item correto no spinner de período customizado.
     */
    private void setPeriodoCustomSpinner(String prazoUnidade) {
        if (prazoUnidade == null) return;
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinnerPrazoUnidade.getAdapter();
        if (prazoUnidade.equalsIgnoreCase("semanas")) spinnerPrazoUnidade.setSelection(adapter.getPosition("semanas"));
        else if (prazoUnidade.equalsIgnoreCase("meses")) spinnerPrazoUnidade.setSelection(adapter.getPosition("meses"));
        else spinnerPrazoUnidade.setSelection(adapter.getPosition("dias"));
    }

    /**
     * Limpa todos os campos do formulário para uma nova inserção.
     */
    private void limparFormulario() {
        inputEquipamentoNome.setText("");
        inputQuantidade.setText("");
        inputValor.setText("");
        inputDataAluguel.setText("");
        rbPeriodoSimples.setChecked(true);
        spinnerPeriodoSimples.setSelection(0);
        inputPrazoQuantidade.setText("");
        textDataDevolucao.setText("Previsão de Devolução: --");
    }

    /**
     * Exibe o calendário para o usuário selecionar a data de início do aluguel.
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
     * Calcula e atualiza a previsão da data de devolução com base nos campos de período.
     */
    private void calcularEAtualizarDataDevolucao() {
        String dataInicioStr = inputDataAluguel.getText().toString();
        if (dataInicioStr.isEmpty()) {
            textDataDevolucao.setText("Previsão de Devolução: --");
            return;
        }
        try {
            int prazoQtd;
            String prazoUnidade;

            if (rbPeriodoSimples.isChecked()) {
                String periodoSelecionado = spinnerPeriodoSimples.getSelectedItem().toString();
                prazoQtd = 1;
                if (periodoSelecionado.equalsIgnoreCase("Semanal")) prazoUnidade = "semanas";
                else if (periodoSelecionado.equalsIgnoreCase("Mensal")) prazoUnidade = "meses";
                else prazoUnidade = "dias";
            } else {
                String prazoQtdStr = inputPrazoQuantidade.getText().toString().trim();
                if (prazoQtdStr.isEmpty()) {
                    textDataDevolucao.setText("Previsão de Devolução: --");
                    return;
                }
                prazoQtd = Integer.parseInt(prazoQtdStr);
                prazoUnidade = spinnerPrazoUnidade.getSelectedItem().toString();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(dataInicioStr));

            if (prazoUnidade.equalsIgnoreCase("dias")) cal.add(Calendar.DAY_OF_MONTH, prazoQtd);
            else if (prazoUnidade.equalsIgnoreCase("semanas")) cal.add(Calendar.WEEK_OF_YEAR, prazoQtd);
            else if (prazoUnidade.equalsIgnoreCase("meses")) cal.add(Calendar.MONTH, prazoQtd);

            textDataDevolucao.setText("Previsão de Devolução: " + sdf.format(cal.getTime()));

        } catch (ParseException | NumberFormatException e) {
            textDataDevolucao.setText("Previsão de Devolução: Erro");
        }
    }

    /**
     * Classe interna para popular o Spinner de locais, guardando o ID e o Nome do local.
     */
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
            return nome; // O ArrayAdapter usará isso para exibir o texto no Spinner
        }
    }
}
