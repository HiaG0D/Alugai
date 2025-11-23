package com.example.aluga;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import CadastrarLocal.Local;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // As configurações de EdgeToEdge/Insets podem ser mantidas aqui, mas não são essenciais para este problema.

        // 1. Identificar o botão no layout usando o ID: cadastrarlocal
        Button botaoCadastrarLocal = findViewById(R.id.cadastrarlocal);

        // 2. Adicionar o ouvinte de clique (OnClickListener)
        botaoCadastrarLocal.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                // 3. Criar a Intent: Especifica a Activity atual (MainActivity.this)
                //    e a Activity de destino (Local.class)
                Intent intent = new Intent(MainActivity.this, Local.class);

                // 4. Iniciar a nova Activity
                startActivity(intent);
            }
        });

        Button botaoListarLocais = findViewById(R.id.button_listar);
        botaoListarLocais.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ListaLocaisActivity.class);
            startActivity(intent);
        });

    }

    // Os outros botões (Listar Locais e Locar Equipamento)
    // seguiriam a mesma lógica de identificação e Intent.
}