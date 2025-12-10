package com.example.aluga;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter para a RecyclerView que exibe o histórico de itens devolvidos.
 * Sua responsabilidade é pegar uma lista de dados do histórico e adaptar para o formato
 * visual definido em item_devolvido.xml.
 */
public class DevolvidoAdapter extends RecyclerView.Adapter<DevolvidoAdapter.DevolvidoViewHolder> {

    private List<Map<String, Object>> historicoList;

    /**
     * Construtor do adapter.
     * @param historicoList A lista de itens do histórico a serem exibidos.
     */
    public DevolvidoAdapter(List<Map<String, Object>> historicoList) {
        this.historicoList = historicoList;
    }

    /**
     * Chamado quando a RecyclerView precisa de um novo ViewHolder para representar um item.
     * Ele infla (cria) a view a partir do layout XML.
     */
    @NonNull
    @Override
    public DevolvidoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_devolvido, parent, false);
        return new DevolvidoViewHolder(view);
    }

    /**
     * Chamado pela RecyclerView para exibir os dados em uma posição específica.
     * Este método atualiza o conteúdo do ViewHolder para refletir o item na posição dada.
     */
    @Override
    public void onBindViewHolder(@NonNull DevolvidoViewHolder holder, int position) {
        // Pega o item do histórico na posição atual.
        Map<String, Object> item = historicoList.get(position);

        // Define o nome do equipamento no TextView.
        holder.nomeEquipamento.setText((String) item.get("nomeEquipamento"));

        // Pega os dados de quantidade e data do mapa.
        Long quantidade = (Long) item.get("quantidadeDevolvida");
        com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) item.get("dataDaDevolucao");
        
        // Converte o Timestamp do Firebase para um objeto Date do Java.
        Date dataDevolucao = timestamp != null ? timestamp.toDate() : null;

        // Formata a data para um formato legível (ex: "25/12/2024").
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String dataFormatada = dataDevolucao != null ? sdf.format(dataDevolucao) : "--";

        // Monta a string de detalhes com a quantidade e a data formatada.
        String detalhes = String.format(Locale.getDefault(),
                "Qtd: %d | Devolvido em: %s",
                quantidade != null ? quantidade : 0,
                dataFormatada);

        // Define o texto de detalhes no TextView correspondente.
        holder.detalhesDevolvido.setText(detalhes);
    }

    /**
     * Retorna o número total de itens na lista de dados.
     */
    @Override
    public int getItemCount() {
        return historicoList.size();
    }

    /**
     * ViewHolder que armazena as referências para as views de cada item da lista.
     * Isso evita chamadas repetidas a `findViewById`, otimizando a performance.
     */
    static class DevolvidoViewHolder extends RecyclerView.ViewHolder {
        TextView nomeEquipamento;
        TextView detalhesDevolvido;

        public DevolvidoViewHolder(@NonNull View itemView) {
            super(itemView);
            nomeEquipamento = itemView.findViewById(R.id.item_devolvido_nome);
            detalhesDevolvido = itemView.findViewById(R.id.item_devolvido_detalhes);
        }
    }
}
