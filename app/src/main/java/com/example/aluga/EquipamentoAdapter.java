package com.example.aluga;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EquipamentoAdapter extends RecyclerView.Adapter<EquipamentoAdapter.EquipamentoViewHolder> {

    private List<Map<String, Object>> equipamentosList;

    public EquipamentoAdapter(List<Map<String, Object>> equipamentosList) {
        this.equipamentosList = equipamentosList;
    }

    @NonNull
    @Override
    public EquipamentoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipamento, parent, false);
        return new EquipamentoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipamentoViewHolder holder, int position) {
        Map<String, Object> equipamento = equipamentosList.get(position);

        // Preenche o nome do equipamento
        holder.nomeEquipamento.setText((String) equipamento.get("nomeEquipamento"));

        // Preenche os detalhes (quantidade, valor, data de devolução)
        Long quantidade = (Long) equipamento.get("quantidadeLocada");
        Double valor = (Double) equipamento.get("valorTotalAluguel");
        String dataAluguelStr = (String) equipamento.get("dataAluguel");
        Long prazoQtd = (Long) equipamento.get("prazoQuantidade");
        String prazoUnidade = (String) equipamento.get("prazoUnidade");

        String dataDevolucao = calcularDataDevolucao(dataAluguelStr, prazoQtd, prazoUnidade);

        String detalhes = String.format(Locale.getDefault(),
                "Qtd: %d | Valor: R$ %.2f | Devolução: %s",
                quantidade != null ? quantidade : 0,
                valor != null ? valor : 0.0,
                dataDevolucao);

        holder.detalhesEquipamento.setText(detalhes);
    }

    @Override
    public int getItemCount() {
        return equipamentosList.size();
    }

    static class EquipamentoViewHolder extends RecyclerView.ViewHolder {
        TextView nomeEquipamento;
        TextView detalhesEquipamento;

        public EquipamentoViewHolder(@NonNull View itemView) {
            super(itemView);
            nomeEquipamento = itemView.findViewById(R.id.item_equipamento_nome);
            detalhesEquipamento = itemView.findViewById(R.id.item_equipamento_detalhes);
        }
    }

    /**
     * Calcula a data de devolução com base nos dados do Firebase.
     */
    private String calcularDataDevolucao(String dataInicioStr, Long prazoQtd, String prazoUnidade) {
        if (dataInicioStr == null || prazoQtd == null || prazoUnidade == null) {
            return "--";
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(dataInicioStr));

            if (prazoUnidade.equalsIgnoreCase("dias")) {
                cal.add(Calendar.DAY_OF_MONTH, prazoQtd.intValue());
            } else if (prazoUnidade.equalsIgnoreCase("semanas")) {
                cal.add(Calendar.WEEK_OF_YEAR, prazoQtd.intValue());
            } else if (prazoUnidade.equalsIgnoreCase("meses")) {
                cal.add(Calendar.MONTH, prazoQtd.intValue());
            }
            return sdf.format(cal.getTime());
        } catch (ParseException e) {
            return "Data inválida";
        }
    }
}
