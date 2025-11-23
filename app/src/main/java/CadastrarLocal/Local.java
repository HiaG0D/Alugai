package CadastrarLocal;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aluga.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class Local extends AppCompatActivity {

    private int id;
    private EditText nome;
    private EditText endereco;
    private Button btnSalvar;
    private long dataCadastro;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);
        db=FirebaseFirestore.getInstance();

        nome=findViewById(R.id.nome);
        endereco=findViewById(R.id.endereco);
        btnSalvar=findViewById(R.id.btn_salvar);

        btnSalvar.setOnClickListener(v ->{
            salvarLocal();
        });
    }

    private void salvarLocal() {
        String nomeLocal = nome.getText().toString().trim();
        String enderecoLocal = endereco.getText().toString().trim();

        if (nomeLocal.isEmpty() || enderecoLocal.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_LONG).show();
            return; //
        }

        Map<String, Object> localData = new HashMap<>();
        localData.put("nome", nomeLocal);
        localData.put("endereco", enderecoLocal); // 🚨 CORRIGIDO: Salvando a String 'enderecoLocal'

        db.collection("locais")
                .add(localData)
                .addOnSuccessListener(documentReference -> {

                    Toast.makeText(this, "Local cadastrado com sucesso!", Toast.LENGTH_SHORT).show();


                    nome.setText("");
                    endereco.setText("");
                })
                .addOnFailureListener(e -> {

                    Log.e("CADASTRO_FIREBASE", "Erro ao salvar: ", e);
                    Toast.makeText(this, "Erro ao salvar local: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
