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

public class LocaisAdapter extends RecyclerView.Adapter<LocaisAdapter.LocalViewHolder> {

    // Interface para o clique no item (navegação)
    public interface OnItemClickListener {
        void onItemClick(Map<String, Object> local);
    }

    // Nova interface para o clique nas opções do menu
    public interface OnOptionsClickListener {
        void onEditClick(Map<String, Object> local);
        void onDeleteClick(Map<String, Object> local);
    }

    private final List<Map<String, Object>> locaisList;
    private final OnItemClickListener itemClickListener;
    private final OnOptionsClickListener optionsClickListener; // Novo listener

    public LocaisAdapter(List<Map<String, Object>> locaisList, OnItemClickListener itemClickListener, OnOptionsClickListener optionsClickListener) {
        this.locaisList = locaisList;
        this.itemClickListener = itemClickListener;
        this.optionsClickListener = optionsClickListener;
    }

    public static class LocalViewHolder extends RecyclerView.ViewHolder {
        TextView nome, endereco;
        ImageView optionsMenu;

        public LocalViewHolder(View itemView) {
            super(itemView);
            nome = itemView.findViewById(R.id.text_item_nome);
            endereco = itemView.findViewById(R.id.text_item_endereco);
            optionsMenu = itemView.findViewById(R.id.options_menu);
        }

        public void bind(final Map<String, Object> local, final OnItemClickListener itemListener, final OnOptionsClickListener optionsListener) {
            // Clique no item inteiro para ver detalhes
            itemView.setOnClickListener(v -> itemListener.onItemClick(local));

            // Clique no menu de opções
            optionsMenu.setOnClickListener(v -> {
                showPopupMenu(v.getContext(), v, local, optionsListener);
            });
        }

        private void showPopupMenu(Context context, View view, Map<String, Object> local, OnOptionsClickListener listener) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.getMenuInflater().inflate(R.menu.local_item_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_edit) {
                    listener.onEditClick(local);
                    return true;
                } else if (itemId == R.id.menu_delete) {
                    listener.onDeleteClick(local);
                    return true;
                }
                return false;
            });
            popup.show();
        }
    }

    @NonNull
    @Override
    public LocalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_local, parent, false);
        return new LocalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocalViewHolder holder, int position) {
        Map<String, Object> local = locaisList.get(position);
        holder.nome.setText((String) local.get("nome"));
        holder.endereco.setText((String) local.get("endereco"));
        holder.bind(local, itemClickListener, optionsClickListener);
    }

    @Override
    public int getItemCount() {
        return locaisList.size();
    }
}
