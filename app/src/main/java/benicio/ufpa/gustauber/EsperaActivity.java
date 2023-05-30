package benicio.ufpa.gustauber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

import benicio.ufpa.gustauber.Model.ReqModel;
import benicio.ufpa.gustauber.databinding.ActivityEsperaBinding;

public class EsperaActivity extends AppCompatActivity {

    private ActivityEsperaBinding vbinding;
    private DatabaseReference refRequisicoes = FirebaseDatabase.getInstance().getReference("requisicoes");
    private  ReqModel model;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vbinding = ActivityEsperaBinding.inflate(getLayoutInflater());
        Bundle extras = getIntent().getExtras();
        
        setContentView(vbinding.getRoot());
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        refRequisicoes.child(extras.getString("idMotorista")).child(extras.getString("idUser")).addValueEventListener(new ValueEventListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                model = snapshot.getValue(ReqModel.class);
                
                if( model.getStatus() == 1){
                    vbinding.textView4.setText("Corrida em andamento.\nEspere no seu ponto fixo");
                    vbinding.progressBar2.setVisibility(View.GONE);
                    vbinding.textDetalhesCorrida.setVisibility(View.VISIBLE);
                    vbinding.textDetalhesCorrida.setText(
                            String.format(
                                        "Detalhe da corrida:"+"\n\n"+
                                        "Motorista: %s."+"\n"
                                        +"Distância: %.2f metros."+"\n"
                                        +"Valor: R$ %s",
                                        model.getNomeMotorista(),
                                        model.getDistancia(),
                                        model.getValor()
                            )
                    );
                    Toast.makeText(EsperaActivity.this, "Sua corrida foi aceita, aguarde!", Toast.LENGTH_SHORT).show();
                }else if (model.getStatus() == 0){
                    vbinding.progressBar2.setVisibility(View.GONE);
                    Toast.makeText(EsperaActivity.this, "Sua corrida foi cancelada, volte!", Toast.LENGTH_SHORT).show();
                }else if(model.getStatus() == 2){
                    Toast.makeText(EsperaActivity.this, "Corrida concluída!", Toast.LENGTH_SHORT).show();
                }
                
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        vbinding.btnCancelar2.setOnClickListener( viewCancelar -> {
            model.setStatus(0);
            refRequisicoes.child(extras.getString("idMotorista")).child(extras.getString("idUser")).setValue(model).addOnCompleteListener(task -> {
                if ( task.isSuccessful()){
                    Toast.makeText(getApplicationContext(), "Corrida cancelada!", Toast.LENGTH_SHORT).show();
                    finish();
                }else{
                    Toast.makeText(getApplicationContext(), "Erro ao cancelar corrida, tente denovo!", Toast.LENGTH_SHORT).show();
                }
            });

        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if ( item.getItemId() == android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}