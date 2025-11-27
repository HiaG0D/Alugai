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

/**
 * Adapter para a RecyclerView que exibe a lista de equipamentos alugados.
 * Ele é responsável por pegar a lista de dados e adaptar para o formato visual de cada item.
 */
public class EquipamentoAdapter extends RecyclerView.Adapter<EquipamentoAdapter.EquipamentoViewHolder> {

    /**
     * Interface para comunicar os eventos de clique nas opções do menu (Editar, Excluir, Devolver)
     * de volta para a Activity que está usando este adapter.
     */
    public interface OnEquipmentOptionsClickListener {
        void onEditClick(Map<String, Object> equipment);
        void onDeleteClick(Map<String, Object> equipment);
        void onReturnClick(Map<String, Object> equipment);
    }

    private List<Map<String, Object>> equipamentosList;
    private final OnEquipmentOptionsClickListener listener;

    /**
     * Construtor do Adapter.
     * @param equipamentosList A lista de equipamentos a ser exibida.
     * @param listener A instância da Activity ou Fragment que vai "ouvir" os eventos de clique.
     */
    public EquipamentoAdapter(List<Map<String, Object>> equipamentosList, OnEquipmentOptionsClickListener listener) {
        this.equipamentosList = equipamentosList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EquipamentoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla (cria) a view do layout de um item a partir do XML.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_equipamento, parent, false);
        return new EquipamentoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipamentoViewHolder holder, int position) {
        // Pega o equipamento na posição atual da lista.
        Map<String, Object> equipamento = equipamentosList.get(position);
        // Conecta os dados do equipamento com a view (ViewHolder).
        holder.bind(equipamento, listener);
    }

    @Override
    public int getItemCount() {
        return equipamentosList.size();
    }

    /**
     * ViewHolder que representa a view de um único item na lista de equipamentos.
     * Ele armazena as referências para os componentes da UI (TextViews, ImageView).
     */
    static class EquipamentoViewHolder extends RecyclerView.ViewHolder {
        TextView nomeEquipamento, detalhesEquipamento;
        ImageView optionsMenu;

        public EquipamentoViewHolder(@NonNull View itemView) {
            super(itemView);
            nomeEquipamento = itemView.findViewById(R.id.item_equipamento_nome);
            detalhesEquipamento = itemView.findViewById(R.id.item_equipamento_detalhes);
            optionsMenu = itemView.findViewById(R.id.equipment_options_menu);
        }

        /**
         * Conecta os dados de um equipamento específico aos componentes da UI deste ViewHolder.
         * @param equipamento O mapa de dados do equipamento.
         * @param listener O listener para os eventos de clique.
         */
        void bind(final Map<String, Object> equipamento, final OnEquipmentOptionsClickListener listener) {
            nomeEquipamento.setText((String) equipamento.get("nomeEquipamento"));

            // Pega os dados do mapa para montar a string de detalhes.
            Long quantidade = (Long) equipamento.get("quantidadeLocada");
            Double valor = (Double) equipamento.get("valorTotalAluguel");
            String dataDevolucao = calcularDataDevolucao((String) equipamento.get("dataAluguel"), (Long) equipamento.get("prazoQuantidade"), (String) equipamento.get("prazoUnidade"));

            // Formata a string de detalhes de forma organizada.
            String detalhes = String.format(Locale.getDefault(),
                    "Qtd: %d | Valor: R$ %.2f | Devolução: %s",
                    quantidade != null ? quantidade : 0,
                    valor != null ? valor : 0.0,
                    dataDevolucao);
            detalhesEquipamento.setText(detalhes);

            // Configura o clique no ícone de menu.
            optionsMenu.setOnClickListener(v -> showPopupMenu(v.getContext(), v, equipamento, listener));
        }

        /**
         * Cria e exibe o menu de opções (Editar, Excluir, Devolver).
         */
        private void showPopupMenu(Context context, View view, Map<String, Object> equipment, OnEquipmentOptionsClickListener listener) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.getMenuInflater().inflate(R.menu.equipment_item_menu, popup.getMenu());

            // Lógica para mostrar/esconder a opção de devolução parcial.
            Long quantidade = (Long) equipment.get("quantidadeLocada");
            if (quantidade != null && quantidade > 0) {
                // Se a quantidade for maior que 1, a opção de devolver fica visível.
                popup.getMenu().findItem(R.id.menu_equip_return).setVisible(true);
            } else {
                // Caso contrário, fica invisível.
                popup.getMenu().findItem(R.id.menu_equip_return).setVisible(false);
            }

            // Define o que acontece ao clicar em cada item do menu.
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_equip_edit) {
                    listener.onEditClick(equipment); // Chama o método de edição na Activity.
                    return true;
                } else if (itemId == R.id.menu_equip_delete) {
                    listener.onDeleteClick(equipment); // Chama o método de exclusão na Activity.
                    return true;
                } else if (itemId == R.id.menu_equip_return) {
                    listener.onReturnClick(equipment); // Chama o método de devolução na Activity.
                    return true;
                }
                return false;
            });
            popup.show();
        }

        /**
         * Método auxiliar para calcular a data de devolução formatada.
         */
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
