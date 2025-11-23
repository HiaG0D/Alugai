package com.example.aluga;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

// Esta é a Activity para a tela de Cadastro de Local.
// A lógica para salvar os dados no Firebase será adicionada aqui.
public class CadastroLocalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Define qual arquivo de layout esta Activity vai usar.
        // O app vai falhar aqui se o arquivo 'activity_cadastro.xml' não existir.
        setContentView(R.layout.activity_cadastro);
    }
}
