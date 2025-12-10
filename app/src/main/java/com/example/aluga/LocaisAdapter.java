package com.example.aluga;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

/**
 * Adapter para a RecyclerView que exibe a lista de locais cadastrados.
 * Ele é responsável por pegar a lista de dados (locais) e adaptar para o formato visual de cada item.
 */
public class LocaisAdapter extends RecyclerView.Adapter<LocaisAdapter.LocalViewHolder> {

    /**
     * Interface para comunicar o evento de clique em um item inteiro da lista.
     * Usada para navegar para a tela de detalhes do local.
     */
    public interface OnItemClickListener {
        void onItemClick(Map<String, Object> local);
    }

    /**
     * Interface para comunicar os eventos de clique nas opções do menu (Editar, Excluir).
     */
    public interface OnOptionsClickListener {
        void onEditClick(Map<String, Object> local);
        void onDeleteClick(Map<String, Object> local);
    }

    // --- Variáveis do Adapter ---
    private final List<Map<String, Object>> locaisList; // A lista de dados a ser exibida.
    private final OnItemClickListener itemClickListener; // O "ouvinte" para o clique no item inteiro.
    private final OnOptionsClickListener optionsClickListener; // O "ouvinte" para o clique nas opções do menu.

    /**
     * Construtor do Adapter.
     * @param locaisList A lista de locais a ser exibida.
     * @param itemClickListener A instância da Activity/Fragment que implementa o listener de clique no item.
     * @param optionsClickListener A instância da Activity/Fragment que implementa o listener de clique nas opções.
     */
    public LocaisAdapter(List<Map<String, Object>> locaisList, OnItemClickListener itemClickListener, OnOptionsClickListener optionsClickListener) {
        this.locaisList = locaisList;
        this.itemClickListener = itemClickListener;
        this.optionsClickListener = optionsClickListener;
    }

    /**
     * ViewHolder que representa a view de um único item na lista.
     * Ele armazena as referências para os componentes da UI para evitar chamadas repetidas a `findViewById`.
     */
    public static class LocalViewHolder extends RecyclerView.ViewHolder {
        TextView nome, endereco;
        ImageView optionsMenu;

        public LocalViewHolder(View itemView) {
            super(itemView);
            nome = itemView.findViewById(R.id.text_item_nome);
            endereco = itemView.findViewById(R.id.text_item_endereco);
            optionsMenu = itemView.findViewById(R.id.options_menu);
        }

        /**
         * Conecta os dados de um local específico aos componentes da UI e configura os listeners de clique.
         * @param local O mapa de dados do local.
         * @param itemListener O listener para o clique no item inteiro.
         * @param optionsListener O listener para o clique no menu de opções.
         */
        public void bind(final Map<String, Object> local, final OnItemClickListener itemListener, final OnOptionsClickListener optionsListener) {
            // Configura o clique no item inteiro para navegar para os detalhes.
            itemView.setOnClickListener(v -> itemListener.onItemClick(local));

            // Configura o clique no ícone de três pontinhos para mostrar o menu.
            optionsMenu.setOnClickListener(v -> {
                showPopupMenu(v.getContext(), v, local, optionsListener);
            });
        }

        /**
         * Cria e exibe o menu de opções (Editar, Excluir) para o item atual.
         */
        private void showPopupMenu(Context context, View view, Map<String, Object> local, OnOptionsClickListener listener) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.getMenuInflater().inflate(R.menu.local_item_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_edit) {
                    listener.onEditClick(local); // Notifica a Activity sobre o clique em "Editar".
                    return true;
                } else if (itemId == R.id.menu_delete) {
                    listener.onDeleteClick(local); // Notifica a Activity sobre o clique em "Excluir".
                    return true;
                }
                return false;
            });
            popup.show();
        }
    }

    /**
     * Chamado quando a RecyclerView precisa de um novo ViewHolder.
     * Cria e retorna um novo LocalViewHolder, inflando o layout do item.
     */
    @NonNull
    @Override
    public LocalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_local, parent, false);
        return new LocalViewHolder(view);
    }

    /**
     * Chamado pela RecyclerView para exibir os dados em uma posição específica.
     * Pega os dados da lista na `position` e os conecta ao `holder`.
     */
    @Override
    public void onBindViewHolder(@NonNull LocalViewHolder holder, int position) {
        Map<String, Object> local = locaisList.get(position);
        holder.nome.setText((String) local.get("nome"));
        holder.endereco.setText((String) local.get("endereco"));
        // Conecta os listeners para este item específico.
        holder.bind(local, itemClickListener, optionsClickListener);
    }

    /**
     * Retorna o número total de itens na lista.
     */
    @Override
    public int getItemCount() {
        return locaisList.size();
    }
}
