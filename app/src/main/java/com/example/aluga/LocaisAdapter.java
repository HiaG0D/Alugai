package com.example.aluga; // Use o pacote onde a ListaLocaisActivity está

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class LocaisAdapter extends RecyclerView.Adapter<LocaisAdapter.LocalViewHolder> {


    private final List<Map<String, Object>> locaisList;

    public LocaisAdapter(List<Map<String, Object>> locaisList) {
        this.locaisList = locaisList;
    }

    public static class LocalViewHolder extends RecyclerView.ViewHolder {
        public TextView nome;
        public TextView endereco;

        public LocalViewHolder(View itemView) {
            super(itemView);

            nome = itemView.findViewById(R.id.text_item_nome);
            endereco = itemView.findViewById(R.id.text_item_endereco);
        }
    }


    @NonNull
    @Override
    public LocalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_local, parent, false);
        return new LocalViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull LocalViewHolder holder, int position) {
        Map<String, Object> local = locaisList.get(position);


        holder.nome.setText((String) local.get("nome"));
        holder.endereco.setText((String) local.get("endereco"));
    }


    @Override
    public int getItemCount() {
        return locaisList.size();
    }
}