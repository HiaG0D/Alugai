package com.example.aluga;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Configuração dos Listeners para os Botões ---

        // 1. Botão Cadastrar Local
        Button botaoCadastrar = findViewById(R.id.cadastrarlocal);
        botaoCadastrar.setOnClickListener(v -> {
            // Para este botão funcionar, a CadastroLocalActivity deve existir.
            // Se ela não existir, o app vai travar ao clicar aqui.
            Intent intent = new Intent(MainActivity.this, CadastroLocalActivity.class);
            startActivity(intent);
        });

        // 2. Botão Listar Locais
        Button botaoListar = findViewById(R.id.button_listar);
        botaoListar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ListaLocaisActivity.class);
            startActivity(intent);
        });

        // 3. Botão Locar Equipamento
        Button botaoLocar = findViewById(R.id.button_locar);
        botaoLocar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LocarEquipamentoActivity.class);
            startActivity(intent);
        });
    }
}
