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

public class DevolvidoAdapter extends RecyclerView.Adapter<DevolvidoAdapter.DevolvidoViewHolder> {

    private List<Map<String, Object>> historicoList;

    public DevolvidoAdapter(List<Map<String, Object>> historicoList) {
        this.historicoList = historicoList;
    }

    @NonNull
    @Override
    public DevolvidoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_devolvido, parent, false);
        return new DevolvidoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DevolvidoViewHolder holder, int position) {
        Map<String, Object> item = historicoList.get(position);

        holder.nomeEquipamento.setText((String) item.get("nomeEquipamento"));

        Long quantidade = (Long) item.get("quantidadeDevolvida");
        com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) item.get("dataDaDevolucao");
        Date dataDevolucao = timestamp != null ? timestamp.toDate() : null;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String dataFormatada = dataDevolucao != null ? sdf.format(dataDevolucao) : "--";

        String detalhes = String.format(Locale.getDefault(),
                "Qtd: %d | Devolvido em: %s",
                quantidade != null ? quantidade : 0,
                dataFormatada);

        holder.detalhesDevolvido.setText(detalhes);
    }

    @Override
    public int getItemCount() {
        return historicoList.size();
    }

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
