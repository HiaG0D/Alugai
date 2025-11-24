package com.example.aluga;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
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

    public interface OnEquipmentOptionsClickListener {
        void onEditClick(Map<String, Object> equipment);
        void onDeleteClick(Map<String, Object> equipment);
        void onReturnClick(Map<String, Object> equipment);
    }

    private List<Map<String, Object>> equipamentosList;
    private final OnEquipmentOptionsClickListener listener;

    public EquipamentoAdapter(List<Map<String, Object>> equipamentosList, OnEquipmentOptionsClickListener listener) {
        this.equipamentosList = equipamentosList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EquipamentoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_equipamento, parent, false);
        return new EquipamentoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipamentoViewHolder holder, int position) {
        Map<String, Object> equipamento = equipamentosList.get(position);
        holder.bind(equipamento, listener);
    }

    @Override
    public int getItemCount() {
        return equipamentosList.size();
    }

    static class EquipamentoViewHolder extends RecyclerView.ViewHolder {
        TextView nomeEquipamento, detalhesEquipamento;
        ImageView optionsMenu;

        public EquipamentoViewHolder(@NonNull View itemView) {
            super(itemView);
            nomeEquipamento = itemView.findViewById(R.id.item_equipamento_nome);
            detalhesEquipamento = itemView.findViewById(R.id.item_equipamento_detalhes);
            optionsMenu = itemView.findViewById(R.id.equipment_options_menu);
        }

        void bind(final Map<String, Object> equipamento, final OnEquipmentOptionsClickListener listener) {
            nomeEquipamento.setText((String) equipamento.get("nomeEquipamento"));

            Long quantidade = (Long) equipamento.get("quantidadeLocada");
            Double valor = (Double) equipamento.get("valorTotalAluguel");
            String dataDevolucao = calcularDataDevolucao((String) equipamento.get("dataAluguel"), (Long) equipamento.get("prazoQuantidade"), (String) equipamento.get("prazoUnidade"));

            String detalhes = String.format(Locale.getDefault(),
                    "Qtd: %d | Valor: R$ %.2f | Devolução: %s",
                    quantidade != null ? quantidade : 0,
                    valor != null ? valor : 0.0,
                    dataDevolucao);
            detalhesEquipamento.setText(detalhes);

            optionsMenu.setOnClickListener(v -> showPopupMenu(v.getContext(), v, equipamento, listener));
        }

        private void showPopupMenu(Context context, View view, Map<String, Object> equipment, OnEquipmentOptionsClickListener listener) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.getMenuInflater().inflate(R.menu.equipment_item_menu, popup.getMenu());

            Long quantidade = (Long) equipment.get("quantidadeLocada");
            if (quantidade != null && quantidade > 1) {
                popup.getMenu().findItem(R.id.menu_equip_return).setVisible(true);
            } else {
                popup.getMenu().findItem(R.id.menu_equip_return).setVisible(false);
            }

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_equip_edit) {
                    listener.onEditClick(equipment);
                    return true;
                } else if (itemId == R.id.menu_equip_delete) {
                    listener.onDeleteClick(equipment);
                    return true;
                } else if (itemId == R.id.menu_equip_return) {
                    listener.onReturnClick(equipment);
                    return true;
                }
                return false;
            });
            popup.show();
        }

        private String calcularDataDevolucao(String dataInicioStr, Long prazoQtd, String prazoUnidade) {
            if (dataInicioStr == null || prazoQtd == null || prazoUnidade == null) return "--";
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                cal.setTime(sdf.parse(dataInicioStr));
                if (prazoUnidade.equalsIgnoreCase("dias")) cal.add(Calendar.DAY_OF_MONTH, prazoQtd.intValue());
                else if (prazoUnidade.equalsIgnoreCase("semanas")) cal.add(Calendar.WEEK_OF_YEAR, prazoQtd.intValue());
                else if (prazoUnidade.equalsIgnoreCase("meses")) cal.add(Calendar.MONTH, prazoQtd.intValue());
                return sdf.format(cal.getTime());
            } catch (ParseException e) {
                return "Data inválida";
            }
        }
    }
}
